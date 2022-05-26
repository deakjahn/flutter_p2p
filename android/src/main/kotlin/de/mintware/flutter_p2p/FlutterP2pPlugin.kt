/*
 * This file is part of the flutter_p2p package.
 *
 * Copyright 2019 by Julian Finkler <julian@mintware.de>
 *
 * For the full copyright and license information, please read the LICENSE
 * file that was distributed with this source code.
 *
 */

package de.mintware.flutter_p2p

import android.Manifest
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.*
import io.flutter.plugin.common.PluginRegistry.Registrar
import android.content.ContentValues.TAG
import android.content.Context
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.nsd.*
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.*
import java.lang.reflect.Method
import java.lang.Exception
import java.util.HashMap
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.WpsInfo
import androidx.annotation.Keep
import de.mintware.flutter_p2p.utility.EventChannelPool
import de.mintware.flutter_p2p.wifi_direct.*
import android.app.Activity
import kotlinx.coroutines.*
import kotlin.coroutines.*
import android.net.wifi.p2p.WifiP2pManager.ActionListener
import de.mintware.flutter_p2p.utility.ProtoHelper


class FlutterP2pPlugin : FlutterPlugin, MethodCallHandler, ActivityAware {
  private var activity: Activity? = null
  private lateinit var context: Context
  private lateinit var platform: MethodChannel
  private val intentFilter = IntentFilter()
  private var receiver: WiFiDirectBroadcastReceiver? = null
  private lateinit var eventPool: EventChannelPool
  private lateinit var socketPool: SocketPool
  private lateinit var channel: WifiP2pManager.Channel
  private lateinit var manager: WifiP2pManager

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    context = flutterPluginBinding.applicationContext
    platform = MethodChannel(flutterPluginBinding.binaryMessenger, "de.mintware.flutter_p2p/flutter_p2p")
    platform.setMethodCallHandler(this)
    eventPool = EventChannelPool(flutterPluginBinding.binaryMessenger)
    setupEventPool()
    setupIntentFilters()
    setupWifiP2pManager()
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    platform.setMethodCallHandler(null)
  }

  override fun onAttachedToActivity(activityPluginBinding: ActivityPluginBinding) {
    activity = activityPluginBinding.getActivity()
  }

  override fun onDetachedFromActivityForConfigChanges() {
    activity = null
  }

  override fun onReattachedToActivityForConfigChanges(activityPluginBinding: ActivityPluginBinding) {
    activity = activityPluginBinding.getActivity()
  }

  override fun onDetachedFromActivity() {
    activity = null
  }

  companion object {
    private const val REQUEST_ENABLE_LOCATION = 600
    private const val CH_STATE_CHANGE = "bc/state-change"
    private const val CH_PEERS_CHANGE = "bc/peers-change"
    private const val CH_CON_CHANGE = "bc/connection-change"
    private const val CH_DEVICE_CHANGE = "bc/this-device-change"
    private const val CH_DISCOVERY_CHANGE = "bc/discovery-change"
    private const val CH_SERVICES_CHANGE = "bc/services-change"
    private const val CH_SOCKET_READ = "socket/read"
    val config: Config = Config()
  }

  fun setupEventPool() {
    eventPool.register(CH_STATE_CHANGE)
    eventPool.register(CH_PEERS_CHANGE)
    eventPool.register(CH_CON_CHANGE)
    eventPool.register(CH_DEVICE_CHANGE)
    eventPool.register(CH_SOCKET_READ)
    eventPool.register(CH_DISCOVERY_CHANGE)
    eventPool.register(CH_SERVICES_CHANGE)

    socketPool = SocketPool(eventPool.getHandler(CH_SOCKET_READ))
  }

  private fun setupIntentFilters() {
    intentFilter.apply {
      // Indicates a change in the Wi-Fi P2P status.
      addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
      // Indicates a change in the list of available peers.
      addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
      // Indicates the state of Wi-Fi P2P connectivity has changed.
      addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
      // Indicates this device'base details have changed.
      addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
      // Indicates the state of peer discovery has changed
      addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION)
    }
  }

  private fun setupWifiP2pManager() {
    manager = context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
    channel = manager.initialize(context, Looper.getMainLooper(), null)
  }

  @Keep
  @Suppress("unused", "UNUSED_PARAMETER")
  private fun requestLocationPermission(call: MethodCall, result: Result) {
    val perm = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    activity?.requestPermissions(perm, REQUEST_ENABLE_LOCATION)
    result.success(true)
  }

  @Keep
  @Suppress("unused", "UNUSED_PARAMETER")
  private fun isLocationPermissionGranted(call: MethodCall, result: Result) {
    val permission = Manifest.permission.ACCESS_FINE_LOCATION
    result.success(PackageManager.PERMISSION_GRANTED == context.checkSelfPermission(permission))
  }

  /**
   * Subscribe to WiFi Events
   *
   * @param call The Method call
   * @param result The Method result
   */
  @Keep
  @Suppress("unused", "UNUSED_PARAMETER")
  fun register(call: MethodCall, result: Result) {
    if (receiver != null) {
      result.success(false)
      return
    }

    try {
      receiver = WiFiDirectBroadcastReceiver(
        manager,
        channel,
        eventPool.getHandler(CH_STATE_CHANGE).sink,
        eventPool.getHandler(CH_PEERS_CHANGE).sink,
        eventPool.getHandler(CH_CON_CHANGE).sink,
        eventPool.getHandler(CH_DEVICE_CHANGE).sink,
        eventPool.getHandler(CH_DISCOVERY_CHANGE).sink,
      )
      context.registerReceiver(receiver, intentFilter)
      result.success(true)
    } catch (e: Exception) {
      result.success(false)
    }
  }

  /**
   * Unsubscribe from WiFi Events
   *
   * @param call The Method call
   * @param result The Method result
   */
  @Keep
  @Suppress("unused", "UNUSED_PARAMETER")
  fun unregister(call: MethodCall, result: Result) {
    if (receiver == null) {
      result.success(false)
      return
    }

    try {
      context.unregisterReceiver(receiver)
      receiver = null;
      result.success(true)
    } catch (e: Exception) {
      result.success(false)
    }
  }

  /**
   * Start discovering WiFi devices
   *
   * @param call The Method call
   * @param result The Method result
   */
  @Keep
  @Suppress("unused", "UNUSED_PARAMETER")
  fun discover(call: MethodCall, result: Result) {
    manager.discoverPeers(channel, ResultActionListener(result))
  }

  /**
   * Stop discovering WiFi devices
   *
   * @param call The Method call
   * @param result The Method result
   */
  @Keep
  @Suppress("unused", "UNUSED_PARAMETER")
  fun stopDiscover(call: MethodCall, result: Result) {
    manager.stopPeerDiscovery(channel, ResultActionListener(result))
  }

  /**
   * Start discovering WiFi services
   *
   * @param call The Method call
   * @param result The Method result
   */
  @Keep
  @Suppress("unused", "UNUSED_PARAMETER")
  fun discoverServices(call: MethodCall, result: Result) {
    GlobalScope.launch(Dispatchers.Main) {
      val sink = eventPool.getHandler(CH_SERVICES_CHANGE).sink
      val upnpType = call.argument<String>("upnp")!!
      val dndType = call.argument<String>("dnd")!!

      withContext(Dispatchers.Default) {
        manager.setUpnpServiceResponseListener(channel, UpnpServiceListener(sink))
        manager.addServiceRequest(channel, WifiP2pUpnpServiceRequest.newInstance(upnpType), object : ActionListener {
          override fun onSuccess() {
          }

          override fun onFailure(arg0: Int) {
            result.error(arg0.toString(), "WiFi error: service request error", null)
          }
        })

        manager.setDnsSdResponseListeners(channel, DndServiceListener(sink), DndServiceTxtListener(sink))
        manager.addServiceRequest(channel, WifiP2pDnsSdServiceRequest.newInstance(dndType), object : ActionListener {
          override fun onSuccess() {
            result.success(true)
          }

          override fun onFailure(arg0: Int) {
            result.error(arg0.toString(), "WiFi error: service request error", null)
          }
        })

        manager.discoverServices(channel, object : ActionListener {
          override fun onSuccess() {
          }

          override fun onFailure(arg0: Int) {
            val message = when (arg0) {
              0 -> "internal error"
              1 -> "unsupported"
              2 -> "busy"
              else -> "other error"
            }
            result.error(arg0.toString(), "WiFi error: $message", null)
          }
        })
      }
    }
  }

  /**
   * Stop discovering WiFi services
   *
   * @param call The Method call
   * @param result The Method result
   */
  @Keep
  @Suppress("unused", "UNUSED_PARAMETER")
  fun stopDiscoverServices(call: MethodCall, result: Result) {
    manager.clearServiceRequests(channel, ResultActionListener(result))
  }

  @Keep
  @Suppress("unused", "UNUSED_PARAMETER")
  fun connect(call: MethodCall, result: Result) {
    try {
      val device = Protos.WifiP2pDevice.parseFrom(call.argument<ByteArray>("device"))
      manager.connect(channel, WifiP2pConfig().apply {
        deviceAddress = device.deviceAddress
        wps.setup = if (device.wpsPbcSupported) WpsInfo.PBC
        else if (device.wpsDisplaySupported) WpsInfo.DISPLAY
        else if (device.wpsKeypadSupported) WpsInfo.KEYPAD
        else WpsInfo.INVALID
        groupOwnerIntent = 0
      }, ResultActionListener(result))
    } catch (e: Exception) {
      result.error("0", e.localizedMessage, e.stackTraceToString())
    }
  }

  @Keep
  @Suppress("unused", "UNUSED_PARAMETER")
  fun cancelConnect(call: MethodCall, result: Result) {
    manager.cancelConnect(channel, ResultActionListener(result))
  }

  @Keep
  @Suppress("unused", "UNUSED_PARAMETER")
  fun getConnectionInfo(call: MethodCall, result: Result) {
    manager.requestConnectionInfo(channel, { info ->
      result.success(ProtoHelper.create(info).toByteArray())
    })
  }

  @Keep
  @Suppress("unused", "UNUSED_PARAMETER")
  fun removeGroup(call: MethodCall, result: Result) {
    manager.requestGroupInfo(channel) { group ->
      if (group != null) {
        manager.removeGroup(channel, ResultActionListener(result))
      } else {
        //signal success as the device is not currently a member of a group
        result.success(true)
      }
    }
  }

  @Keep
  @Suppress("unused", "UNUSED_PARAMETER")
  fun getGroupInfo(call: MethodCall, result: Result) {
    manager.requestGroupInfo(channel, { group ->
      result.success(ProtoHelper.create(group).toByteArray())
    })
  }

  @Keep
  @Suppress("unused", "UNUSED_PARAMETER")
  fun openHostPort(call: MethodCall, result: Result) {
    try {
      val port = call.argument<Int>("port")
      if (port == null) {
        result.error("0", "Invalid port given", null)
        return
      }

      socketPool.openSocket(port)
      result.success(true)
    } catch (e: Exception) {
      result.error("0", e.localizedMessage, e.stackTraceToString())
    }
  }

  @Keep
  @Suppress("unused", "UNUSED_PARAMETER")
  fun closeHostPort(call: MethodCall, result: Result) {
    try {
      val port = call.argument<Int>("port")
      if (port == null) {
        result.error("0", "Invalid port given", null)
        return
      }

      socketPool.closeSocket(port)
      result.success(true)
    } catch (e: Exception) {
      result.success(false)
    }
  }

  @Keep
  @Suppress("unused", "UNUSED_PARAMETER")
  fun acceptPort(call: MethodCall, result: Result) {
    try {
      val port = call.argument<Int>("port")
      if (port == null) {
        result.error("0", "Invalid port given", null)
        return
      }

      socketPool.acceptClientConnection(port)
      result.success(true)
    } catch (e: Exception) {
      result.error("0", e.localizedMessage, e.stackTraceToString())
    }
  }

  @Keep
  @Suppress("unused", "UNUSED_PARAMETER")
  fun connectToHost(call: MethodCall, result: Result) {
    try {
      val address = call.argument<String>("address")
      val port = call.argument<Int>("port")
      val timeout = call.argument<Int>("timeout") ?: config.timeout

      if (port == null || address == null) {
        result.error("0", "Invalid address or port given", null)
        return
      }

      socketPool.connectToHost(address, port, timeout)
      result.success(true)
    } catch (e: Exception) {
      result.error("0", e.localizedMessage, e.stackTraceToString())
    }
  }

  @Keep
  @Suppress("unused", "UNUSED_PARAMETER")
  fun disconnectFromHost(call: MethodCall, result: Result) {
    try {
      val port = call.argument<Int>("port")
      if (port == null) {
        result.error("0", "Invalid port given", null)
        return
      }
      this.socketPool.disconnectFromHost(port)
      result.success(true)
    } catch (e: Exception) {
      result.success(false)
    }
  }

  @Keep
  @Suppress("unused", "UNUSED_PARAMETER")
  fun sendDataToHost(call: MethodCall, result: Result) {
    try {
      val socketMessage = Protos.SocketMessage.parseFrom(call.argument<ByteArray>("payload"))
      this.socketPool.sendDataToHost(socketMessage.port, socketMessage.data.toByteArray())
      result.success(true)
    } catch (e: Exception) {
      result.error("0", e.localizedMessage, e.stackTraceToString())
    }
  }

  @Keep
  @Suppress("unused", "UNUSED_PARAMETER")
  fun sendDataToClient(call: MethodCall, result: Result) {
    try {
      val socketMessage = Protos.SocketMessage.parseFrom(call.argument<ByteArray>("payload"))
      this.socketPool.sendDataToClient(socketMessage.port, socketMessage.data.toByteArray())
      result.success(true)
    } catch (e: Exception) {
      result.error("0", e.localizedMessage, e.stackTraceToString())
    }
  }

  override fun onMethodCall(call: MethodCall, rawResult: Result) {
    val result = MethodResultWrapper(rawResult)
    when (call.method) {
      "requestLocationPermission" -> requestLocationPermission(call, result)
      "isLocationPermissionGranted" -> isLocationPermissionGranted(call, result)
      "register" -> register(call, result)
      "unregister" -> unregister(call, result)
      "discover" -> discover(call, result)
      "stopDiscover" -> stopDiscover(call, result)
      "connect" -> connect(call, result)
      "cancelConnect" -> cancelConnect(call, result)
      "getConnectionInfo" -> getConnectionInfo(call, result)
      "removeGroup" -> removeGroup(call, result)
      "getGroupInfo" -> getGroupInfo(call, result)
      "openHostPort" -> openHostPort(call, result)
      "closeHostPort" -> closeHostPort(call, result)
      "acceptPort" -> acceptPort(call, result)
      "connectToHost" -> connectToHost(call, result)
      "disconnectFromHost" -> disconnectFromHost(call, result)
      "sendDataToHost" -> sendDataToHost(call, result)
      "sendDataToClient" -> sendDataToClient(call, result)
      "discoverServices" -> discoverServices(call, result)
      "stopDiscoverServices" -> stopDiscoverServices(call, result)
      else -> result.notImplemented()
    }
  }

  private class MethodResultWrapper(private val methodResult: Result) : Result {
    private val handler: Handler = Handler(Looper.getMainLooper())

    override fun success(result: Any?) {
      handler.post { methodResult.success(result) }
    }

    override fun error(errorCode: String, errorMessage: String?, errorDetails: Any?) {
      handler.post { methodResult.error(errorCode, errorMessage, errorDetails) }
    }

    override fun notImplemented() {
      handler.post { methodResult.notImplemented() }
    }
  }
}

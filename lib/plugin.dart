/*
 * This file is part of the flutter_p2p package.
 *
 * Copyright 2019 by Julian Finkler <julian@mintware.de>
 *
 * For the full copyright and license information, please read the LICENSE
 * file that was distributed with this source code.
 *
 */

part of 'flutter_p2p.dart';

class FlutterP2p {
  static const channelBase = "de.mintware.flutter_p2p";

  static const _channel = const MethodChannel('$channelBase/flutter_p2p');

  static WiFiDirectBroadcastReceiver wifiEvents = WiFiDirectBroadcastReceiver();
  static SocketMaster _socketMaster = SocketMaster();

  static Future<bool> isLocationPermissionGranted() async {
    final result = await _channel.invokeMethod<bool>("isLocationPermissionGranted", {});
    return result!;
  }

  static Future<bool> requestLocationPermission() async {
    final result = await _channel.invokeMethod<bool>("requestLocationPermission", {});
    return result!;
  }

  static Future<bool> register() async {
    final result = await _channel.invokeMethod<bool>("register", {});
    return result!;
  }

  static Future<bool> unregister() async {
    final result = await _channel.invokeMethod<bool>("unregister", {});
    return result!;
  }

  static Future<bool> discoverDevices() async {
    final result = await _channel.invokeMethod<bool>("discover", {});
    return result!;
  }

  static Future<bool> stopDiscoverDevices() async {
    final result = await _channel.invokeMethod<bool>("stopDiscover", {});
    return result!;
  }

  static Future<bool> discoverServices(String upnpType, String dndType) async {
    final result = await _channel.invokeMethod<bool>("discoverServices", {
      "upnp": upnpType,
      "dnd": dndType,
    });
    return result!;
  }

  static Future<bool> stopDiscoverServices() async {
    final result = await _channel.invokeMethod<bool>("stopDiscoverServices", {});
    return result!;
  }

  static Future<bool> connect(WifiP2pDevice device, {String? name, String? passphrase}) async {
    final result = await _channel.invokeMethod<bool>("connect", {
      "device": device.writeToBuffer(),
      "name": name,
      "passphrase": passphrase,
    });
    return result!;
  }

  static Future<bool> cancelConnect(WifiP2pDevice device) async {
    final result = await _channel.invokeMethod<bool>("cancelConnect", {});
    return result!;
  }

  static Future<WifiP2pInfo> getConnectionInfo() async {
    final result = await _channel.invokeMethod<Uint8List>("getConnectionInfo", {});
    return WifiP2pInfo.fromBuffer(result!);
  }

  static Future<bool> removeGroup() async {
    final result = await _channel.invokeMethod<bool>("removeGroup", {});
    return result!;
  }

  static Future<WifiP2pGroup> getGroupInfo() async {
    final result = await _channel.invokeMethod<Uint8List>("getGroupInfo", {});
    return WifiP2pGroup.fromBuffer(result!);
  }

  static Future<P2pSocket> openHostPort(int port) async {
    await _channel.invokeMethod("openHostPort", {"port": port});
    return _socketMaster.registerSocket(port, true);
  }

  static Future<P2pSocket> closeHostPort(int port) async {
    await _channel.invokeMethod("closeHostPort", {"port": port});
    return _socketMaster.unregisterServerPort(port);
  }

  static Future<bool> acceptPort(int port) async {
    final result = await _channel.invokeMethod<bool>("acceptPort", {"port": port});
    return result!;
  }

  static Future<P2pSocket?> connectToHost(String address, int port, {int timeout = 500}) async {
    if (await _channel.invokeMethod("connectToHost", {
      "address": address,
      "port": port,
      "timeout": timeout,
    })) {
      return _socketMaster.registerSocket(port, false);
    }
    return null;
  }

  static Future<bool> disconnectFromHost(int port) async {
    final result = await _channel.invokeMethod<bool>("disconnectFromHost", {
      "port": port,
    });
    return result!;
  }

  static Future<bool> sendData(int port, bool isHost, Uint8List data) async {
    var req = SocketMessage.create();
    req.port = port;
    req.data = data;
    req.dataAvailable = 0;

    var action = isHost ? "sendDataToClient" : "sendDataToHost";
    final result = await _channel.invokeMethod<bool>(action, {
      "payload": req.writeToBuffer(),
    });
    return result!;
  }
}

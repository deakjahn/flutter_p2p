/*
 * This file is part of the flutter_p2p package.
 *
 * Copyright 2019 by Julian Finkler <julian@mintware.de>
 *
 * For the full copyright and license information, please read the LICENSE
 * file that was distributed with this source code.
 *
 */

package de.mintware.flutter_p2p.utility

import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pGroup
import android.net.wifi.p2p.WifiP2pManager
import com.google.protobuf.ByteString
import de.mintware.flutter_p2p.Protos

class ProtoHelper {
    companion object {
        fun create(isEnabled: Boolean): Protos.StateChange {
            return Protos.StateChange.newBuilder()
                    .setIsEnabled(isEnabled)
                    .build()
        }

        fun create(discoveryState: Int): Protos.DiscoveryStateChange {
            return Protos.DiscoveryStateChange.newBuilder()
                    .setIsDiscovering(discoveryState != WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED)
                    .build();
        }

        fun create(device: WifiP2pDevice): Protos.WifiP2pDevice {
            return Protos.WifiP2pDevice.newBuilder()
                    .setWpsPbcSupported(device.wpsPbcSupported())
                    .setWpsKeypadSupported(device.wpsPbcSupported())
                    .setWpsDisplaySupported(device.wpsDisplaySupported())
                    .setIsServiceDiscoveryCapable(device.isServiceDiscoveryCapable)
                    .setIsGroupOwner(device.isGroupOwner)
                    .setDeviceName(device.deviceName ?: "")
                    .setDeviceAddress(device.deviceAddress ?: "")
                    .setPrimaryDeviceType(device.primaryDeviceType ?: "")
                    .setSecondaryDeviceType(device.secondaryDeviceType ?: "")
                    .setStatusValue(device.status)
                    .build()
        }

        fun create(devices: List<WifiP2pDevice>): Protos.WifiP2pDeviceList {
            return Protos.WifiP2pDeviceList.newBuilder()
                    .addAllDevices(devices.map(Companion::create))
                    .build()
        }

        fun create(device: WifiP2pDevice, instance: String?, registration: String?, domain: String?, txt: Map<String, String>, uniqueNames: List<String>): Protos.WifiP2pService {
            return Protos.WifiP2pService.newBuilder()
                    .setDevice(create(device))
                    .setInstance(instance ?: "")
                    .setRegistration(registration ?: "")
                    .setDomain(domain ?: "")
                    .putAllTxt(txt)
                    .addAllUniqueNames(uniqueNames)
                    .build()
        }

        fun create(p2pInfo: WifiP2pInfo, networkInfo: NetworkInfo): Protos.ConnectionChange {
            return Protos.ConnectionChange.newBuilder()
                    .setWifiP2PInfo(create(p2pInfo))
                    .setNetworkInfo(create(networkInfo))
                    .build()
        }

        fun create(p2pInfo: WifiP2pInfo): Protos.WifiP2pInfo {
            return Protos.WifiP2pInfo.newBuilder()
                    .setGroupFormed(p2pInfo.groupFormed)
                    .setIsGroupOwner(p2pInfo.isGroupOwner)
                    .setGroupOwnerAddress(p2pInfo.groupOwnerAddress?.hostAddress ?: "")
                    .build()
        }

        fun create(networkInfo: NetworkInfo): Protos.NetworkInfo {
            return Protos.NetworkInfo.newBuilder()
                    .setSubType(networkInfo.subtype)
                    .setIsConnected(networkInfo.isConnected)
                    .setDetailedStateValue(networkInfo.detailedState.ordinal)
                    .setExtraInfo(networkInfo.extraInfo ?: "")
                    .build()
        }

        fun create(port: Int, data: ByteArray, dataAvailable: Int): Protos.SocketMessage {
            return Protos.SocketMessage.newBuilder()
                    .setData(ByteString.copyFrom(data))
                    .setPort(port)
                    .setDataAvailable(dataAvailable)
                    .build()
        }

        fun create(group: WifiP2pGroup): Protos.WifiP2pGroup {
            return Protos.WifiP2pGroup.newBuilder()
                    .setFrequency(group.frequency)
                    .setInterface(group.getInterface() ?: "")
                    .setNetworkId(group.networkId)
                    .setNetworkName(group.networkName ?: "")
                    .setOwner(create(group.owner))
                    .setPassphrase(group.passphrase ?: "")
                    .setIsGroupOwner(group.isGroupOwner)
                    .addAllClientList(group.clientList.map(Companion::create))
                    .build()
        }

    }
}
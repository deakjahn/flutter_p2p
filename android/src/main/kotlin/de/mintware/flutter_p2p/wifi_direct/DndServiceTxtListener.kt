/*
 * This file is part of the flutter_p2p package.
 *
 * Copyright 2019 by Julian Finkler <julian@mintware.de>
 *
 * For the full copyright and license information, please read the LICENSE
 * file that was distributed with this source code.
 *
 */

package de.mintware.flutter_p2p.wifi_direct

import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.DnsSdTxtRecordListener
import de.mintware.flutter_p2p.utility.ProtoHelper
import io.flutter.plugin.common.EventChannel

class DndServiceTxtListener(private val servicesChangedSink: EventChannel.EventSink?) : WifiP2pManager.DnsSdTxtRecordListener {
    override fun onDnsSdTxtRecordAvailable(fullDomainName: String, txtRecordMap: Map<String, String>, srcDevice: WifiP2pDevice) {
        servicesChangedSink?.success(ProtoHelper.create(srcDevice, null, null, fullDomainName, txtRecordMap, emptyList()).toByteArray())
    }
}
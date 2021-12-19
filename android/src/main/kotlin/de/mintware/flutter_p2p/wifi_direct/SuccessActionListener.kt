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

import android.net.wifi.p2p.WifiP2pManager
import kotlinx.coroutines.*
import kotlin.coroutines.*

class SuccessActionListener(private val cont: Continuation<Boolean>) : WifiP2pManager.ActionListener {
    override fun onSuccess() {
        cont.resume(true)
    }

    override fun onFailure(reasonCode: Int) {
        cont.resume(false)
    }
}
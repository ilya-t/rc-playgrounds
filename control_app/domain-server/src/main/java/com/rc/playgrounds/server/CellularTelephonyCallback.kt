package com.rc.playgrounds.server

import android.os.Build
import android.telephony.SignalStrength
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import java.util.concurrent.Executor

@RequiresApi(Build.VERSION_CODES.S)
internal class CellularCallbackRegistration(
    private val telephony: TelephonyManager,
    executor: Executor,
    onSignal: (SignalStrength) -> Unit,
    onNetworkType: () -> Unit,
) {
    private val callback = CellularTelephonyCallback(onSignal, onNetworkType)

    init {
        telephony.registerTelephonyCallback(executor, callback)
    }

    fun stop() {
        telephony.unregisterTelephonyCallback(callback)
    }
}

@RequiresApi(Build.VERSION_CODES.S)
private class CellularTelephonyCallback(
    private val onSignal: (SignalStrength) -> Unit,
    private val onNetworkType: () -> Unit,
) : TelephonyCallback(),
    TelephonyCallback.SignalStrengthsListener,
    TelephonyCallback.DisplayInfoListener {
    override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
        onSignal(signalStrength)
    }

    override fun onDisplayInfoChanged(telephonyDisplayInfo: android.telephony.TelephonyDisplayInfo) {
        onNetworkType()
    }
}

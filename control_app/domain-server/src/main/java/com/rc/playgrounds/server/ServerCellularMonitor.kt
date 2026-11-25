@file:Suppress("DEPRECATION")

package com.rc.playgrounds.server

import android.annotation.SuppressLint
import android.app.Application
import android.os.Build
import android.telephony.CellInfo
import android.telephony.CellSignalStrengthLte
import android.telephony.CellSignalStrengthNr
import android.telephony.PhoneStateListener
import android.telephony.SignalStrength
import android.telephony.TelephonyManager
import com.rc.playgrounds.telemetry.Telemetry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ServerCellularMonitor(
    app: Application,
) {
    private val telephony = app.getSystemService(TelephonyManager::class.java)
    private val mainExecutor = app.mainExecutor
    private val readings = MutableStateFlow<Telemetry.Cellular?>(null)
    val cellular: StateFlow<Telemetry.Cellular?> = readings

    private var api31Registration: Any? = null
    private var listener: PhoneStateListener? = null
    private var networkType: Int = TelephonyManager.NETWORK_TYPE_UNKNOWN
    private var lteRsrp: List<Int> = emptyList()
    private var nrRsrp: List<Int> = emptyList()

    fun setPermissionGranted(granted: Boolean) {
        if (!granted || telephony == null) {
            stop()
            clear()
            return
        }
        if (api31Registration == null && listener == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                listenApi31(telephony)
            } else {
                listenApi30(telephony)
            }
        }
        publishFromTelephony(telephony)
    }

    private fun listenApi31(telephony: TelephonyManager) {
        api31Registration = CellularCallbackRegistration(
            telephony = telephony,
            executor = mainExecutor,
            onSignal = ::applySignal,
            onNetworkType = {
                networkType = runCatching { telephony.dataNetworkType }
                    .getOrDefault(TelephonyManager.NETWORK_TYPE_UNKNOWN)
                publish()
            },
        )
    }

    @Suppress("DEPRECATION")
    private fun listenApi30(telephony: TelephonyManager) {
        val next = object : PhoneStateListener() {
            override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                applySignal(signalStrength)
            }

            override fun onDisplayInfoChanged(telephonyDisplayInfo: android.telephony.TelephonyDisplayInfo) {
                networkType = runCatching { telephony.dataNetworkType }
                    .getOrDefault(telephonyDisplayInfo.networkType)
                publish()
            }
        }
        telephony.listen(
            next,
            PhoneStateListener.LISTEN_SIGNAL_STRENGTHS or PhoneStateListener.LISTEN_DISPLAY_INFO_CHANGED,
        )
        listener = next
    }

    @SuppressLint("MissingPermission")
    private fun publishFromTelephony(telephony: TelephonyManager) {
        try {
            networkType = telephony.dataNetworkType
            applySignal(telephony.signalStrength)
        } catch (_: SecurityException) {
            clear()
        }
    }

    private fun applySignal(signal: SignalStrength?) {
        lteRsrp = signal
            ?.getCellSignalStrengths(CellSignalStrengthLte::class.java)
            .orEmpty()
            .map { it.rsrp }
            .filter { it != CellInfo.UNAVAILABLE }
        nrRsrp = signal
            ?.getCellSignalStrengths(CellSignalStrengthNr::class.java)
            .orEmpty()
            .map { it.ssRsrp }
            .filter { it != CellInfo.UNAVAILABLE }
        publish()
    }

    private fun publish() {
        readings.value = cellularSnapshot(networkType, lteRsrp, nrRsrp)
    }

    private fun clear() {
        networkType = TelephonyManager.NETWORK_TYPE_UNKNOWN
        lteRsrp = emptyList()
        nrRsrp = emptyList()
        readings.value = null
    }

    @Suppress("DEPRECATION")
    private fun stop() {
        val radio = telephony ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            stopApi31()
        }
        listener?.let { radio.listen(it, PhoneStateListener.LISTEN_NONE) }
        listener = null
    }

    private fun stopApi31() {
        (api31Registration as? CellularCallbackRegistration)?.stop()
        api31Registration = null
    }
}

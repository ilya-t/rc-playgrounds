package com.rc.playgrounds.server

import android.telephony.TelephonyManager
import com.rc.playgrounds.telemetry.LTE_RSRP_MAX_DBM
import com.rc.playgrounds.telemetry.LTE_RSRP_MIN_DBM
import com.rc.playgrounds.telemetry.NR_RSRP_MAX_DBM
import com.rc.playgrounds.telemetry.NR_RSRP_MIN_DBM
import com.rc.playgrounds.telemetry.Telemetry
import com.rc.playgrounds.telemetry.signalStrengthPercent

@Suppress("DEPRECATION")
internal fun cellularGeneration(networkType: Int): String? {
    return when (networkType) {
        TelephonyManager.NETWORK_TYPE_GPRS,
        TelephonyManager.NETWORK_TYPE_EDGE,
        TelephonyManager.NETWORK_TYPE_CDMA,
        TelephonyManager.NETWORK_TYPE_1xRTT,
        TelephonyManager.NETWORK_TYPE_IDEN,
        TelephonyManager.NETWORK_TYPE_GSM,
        -> "2G"

        TelephonyManager.NETWORK_TYPE_UMTS,
        TelephonyManager.NETWORK_TYPE_EVDO_0,
        TelephonyManager.NETWORK_TYPE_EVDO_A,
        TelephonyManager.NETWORK_TYPE_HSDPA,
        TelephonyManager.NETWORK_TYPE_HSUPA,
        TelephonyManager.NETWORK_TYPE_HSPA,
        TelephonyManager.NETWORK_TYPE_EVDO_B,
        TelephonyManager.NETWORK_TYPE_EHRPD,
        TelephonyManager.NETWORK_TYPE_HSPAP,
        TelephonyManager.NETWORK_TYPE_TD_SCDMA,
        -> "3G"

        TelephonyManager.NETWORK_TYPE_LTE -> "4G"
        TelephonyManager.NETWORK_TYPE_NR -> "5G"
        else -> null
    }
}

internal fun cellularSnapshot(
    networkType: Int,
    lteRsrpDbm: List<Int>,
    nrSsRsrpDbm: List<Int>,
): Telemetry.Cellular? {
    val generation = cellularGeneration(networkType) ?: return null
    val strength = when (generation) {
        "4G" -> lteRsrpDbm.maxOrNull()?.let {
            signalStrengthPercent(it, LTE_RSRP_MIN_DBM, LTE_RSRP_MAX_DBM)
        }
        "5G" -> nrSsRsrpDbm.maxOrNull()?.let {
            signalStrengthPercent(it, NR_RSRP_MIN_DBM, NR_RSRP_MAX_DBM)
        }
        else -> null
    }
    return Telemetry.Cellular(strength = strength, generation = generation)
}

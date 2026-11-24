package com.rc.playgrounds.telemetry

const val LTE_RSRP_MIN_DBM = -140
const val LTE_RSRP_MAX_DBM = -44
const val NR_RSRP_MIN_DBM = -156
const val NR_RSRP_MAX_DBM = -31

fun signalStrengthPercent(rsrpDbm: Int, minDbm: Int, maxDbm: Int): Int {
    val span = maxDbm - minDbm
    if (span <= 0) {
        return 0
    }
    if (rsrpDbm <= minDbm) {
        return 0
    }
    if (rsrpDbm >= maxDbm) {
        return 100
    }
    return (((rsrpDbm - minDbm).toLong() * 100L + span / 2) / span).toInt()
}

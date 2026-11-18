package com.rc.playgrounds.server.controllink

import com.rc.playgrounds.server.ServerControlState
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/** UART command frames for MicroView protocol v2. */
object MicroviewProtocol {
    const val NEUTRAL_PWM = 1500
    const val MIN_PWM = 1000
    const val MAX_PWM = 2000

    fun command(seq: Int, control: ServerControlState): ByteArray {
        val payload = buildString {
            append("C,")
            append(seq and 0xffff)
            append(',')
            append(axisToPwm(control.long))
            append(',')
            append(axisToPwm(control.steer))
            append(',')
            append(axisToPwm(control.yaw))
            append(',')
            append(axisToPwm(control.pitch))
            append(',')
            append(NEUTRAL_PWM)
            append(',')
            append(NEUTRAL_PWM)
        }
        return wrap(payload).toByteArray(Charsets.US_ASCII)
    }

    fun wrap(payload: String): String {
        var checksum = 0
        for (byte in payload.toByteArray(Charsets.US_ASCII)) {
            checksum = checksum xor (byte.toInt() and 0xff)
        }
        return payload + "*" + String.Companion.format(Locale.US, "%02X", checksum) + "\n"
    }

    fun parseStatus(received: String?): Status? {
        if (received == null || received.length > 95) {
            return null
        }
        val line = received.trim()
        val star = line.lastIndexOf('*')
        if (star < 0 || line.length != star + 3) {
            return null
        }
        val actual = line.substring(star + 1).toIntOrNull(16) ?: return null
        var checksum = 0
        for (index in 0 until star) {
            val symbol = line[index]
            if (symbol.code > 127) {
                return null
            }
            checksum = checksum xor symbol.code
        }
        if (checksum != actual) {
            return null
        }
        val fields = line.substring(0, star).split(',')
        if (fields.size != 5 || fields[0] != "S") {
            return null
        }
        val seq = fields[1].toIntOrNull() ?: return null
        val uptime = fields[2].toLongOrNull() ?: return null
        val age = fields[3].toIntOrNull() ?: return null
        val state = fields[4].toIntOrNull() ?: return null
        if (seq !in 0..65535 || uptime !in 0..4294967295L || age !in 0..65535 ||
            (state != 1 && state != 2)
        ) {
            return null
        }
        return Status(seq, uptime, age, state)
    }

    data class Status(
        val sequence: Int,
        val uptimeMs: Long,
        val commandAgeMs: Int,
        /** 1 = receiving, 2 = failsafe. */
        val deviceState: Int,
    )

    private fun axisToPwm(axis: Float): Int {
        val centered = if (abs(axis) < DEADZONE) 0f else axis.coerceIn(-1f, 1f)
        return (NEUTRAL_PWM + (centered * PWM_SPAN).roundToInt()).coerceIn(MIN_PWM, MAX_PWM)
    }

    private const val DEADZONE = 0.045f
    private const val PWM_SPAN = 500f
}
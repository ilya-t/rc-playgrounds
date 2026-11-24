package com.rc.playgrounds.telemetry

import org.json.JSONException
import org.json.JSONObject

data class Telemetry(
    val control: Control,
    val cellular: Cellular? = null,
    val battery: Battery? = null,
) {
    data class Control(
        val status: String,
        val error: Boolean,
    )

    data class Cellular(
        val strength: Int?,
        val generation: String,
    )

    data class Battery(
        val charge: Int,
        val charging: Boolean,
    )

    fun toJson(): String {
        val json = JSONObject().put(
            CONTROL,
            JSONObject()
                .put(STATUS, control.status)
                .put(ERROR, control.error),
        )
        cellular?.let { cell ->
            val section = JSONObject().put(GENERATION, cell.generation)
            cell.strength?.let { section.put(STRENGTH, it) }
            json.put(CELLULAR, section)
        }
        battery?.let { batt ->
            json.put(
                BATTERY,
                JSONObject()
                    .put(CHARGE, batt.charge)
                    .put(CHARGING, batt.charging),
            )
        }
        return json.toString()
    }

    companion object {
        fun fromJson(text: String): Telemetry? {
            val json = try {
                JSONObject(text)
            } catch (_: JSONException) {
                return null
            }
            val control = json.optJSONObject(CONTROL) ?: return null
            if (!control.has(STATUS) || control.isNull(STATUS)) {
                return null
            }
            val status = control.get(STATUS)
            if (status !is String || status.isEmpty()) {
                return null
            }
            if (!control.has(ERROR) || control.isNull(ERROR)) {
                return null
            }
            val error = control.get(ERROR)
            if (error !is Boolean) {
                return null
            }
            return Telemetry(
                control = Control(status = status, error = error),
                cellular = optionalCellular(json),
                battery = optionalBattery(json),
            )
        }
    }
}

private fun optionalCellular(json: JSONObject): Telemetry.Cellular? {
    if (!json.has(CELLULAR) || json.isNull(CELLULAR)) {
        return null
    }
    val section = json.optJSONObject(CELLULAR) ?: return null
    val generation = section.opt(GENERATION)
    if (generation !is String || generation !in GENERATIONS) {
        return null
    }
    if (!section.has(STRENGTH) || section.isNull(STRENGTH)) {
        return Telemetry.Cellular(strength = null, generation = generation)
    }
    val strength = intInRange(section.opt(STRENGTH), 0, 100) ?: return null
    return Telemetry.Cellular(strength = strength, generation = generation)
}

private fun optionalBattery(json: JSONObject): Telemetry.Battery? {
    if (!json.has(BATTERY) || json.isNull(BATTERY)) {
        return null
    }
    val section = json.optJSONObject(BATTERY) ?: return null
    if (!section.has(CHARGE) || section.isNull(CHARGE) ||
        !section.has(CHARGING) || section.isNull(CHARGING)
    ) {
        return null
    }
    val charge = intInRange(section.opt(CHARGE), 0, 100) ?: return null
    val charging = section.opt(CHARGING)
    if (charging !is Boolean) {
        return null
    }
    return Telemetry.Battery(charge = charge, charging = charging)
}

private fun intInRange(value: Any?, min: Int, max: Int): Int? {
    val parsed = when (value) {
        is Int -> value
        is Long -> value.takeIf { it in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong() }?.toInt()
        is Double -> value.takeIf { it % 1.0 == 0.0 }?.toInt()
        else -> null
    } ?: return null
    return parsed.takeIf { it in min..max }
}

private const val CONTROL = "control"
private const val STATUS = "status"
private const val ERROR = "error"
private const val CELLULAR = "cellular"
private const val STRENGTH = "strength"
private const val GENERATION = "generation"
private const val BATTERY = "battery"
private const val CHARGE = "charge"
private const val CHARGING = "charging"

private val GENERATIONS = setOf("2G", "3G", "4G", "5G")

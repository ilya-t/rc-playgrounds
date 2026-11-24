package com.rc.playgrounds.telemetry

import org.json.JSONException
import org.json.JSONObject

data class Telemetry(
    val control: Control,
) {
    data class Control(
        val status: String,
        val error: Boolean,
    )

    fun toJson(): String {
        return JSONObject()
            .put(
                CONTROL,
                JSONObject()
                    .put(STATUS, control.status)
                    .put(ERROR, control.error),
            )
            .toString()
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
            return Telemetry(Control(status = status, error = error))
        }
    }
}

private const val CONTROL = "control"
private const val STATUS = "status"
private const val ERROR = "error"

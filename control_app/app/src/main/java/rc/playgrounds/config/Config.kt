package rc.playgrounds.config

import android.graphics.PointF
import org.json.JSONObject
import rc.playgrounds.config.model.ControlOffsets
import rc.playgrounds.config.model.ControlTuning
import rc.playgrounds.config.model.Telemetry

class Config(
    val rawJson: String,
) {
    private val json by lazy {
        runCatching {
            JSONObject(rawJson)
        }.getOrElse { JSONObject() }
    }

    val streamUrl: String?
        get() = runCatching {
                json.getJSONObject("stream").getString("url")
            }.getOrNull()

    val telemetry: Telemetry? by lazy {
        runCatching {
            val t = json.getJSONObject("telemetry")
            Telemetry(
                address = t.getString("address"),
                port = t.getInt("port")
            )

        }.getOrNull()
    }

    val controlOffsets: ControlOffsets by lazy {
        runCatching {
            val t = json.getJSONObject("control_offsets")
            ControlOffsets(
                pitch = t.getDouble("pitch").toFloat(),
                yaw = t.getDouble("yaw").toFloat(),
                steer = t.getDouble("steer").toFloat(),
                long = t.getDouble("long").toFloat(),
            )
        }.getOrElse {
            ControlOffsets(
                pitch = 0f,
                yaw = 0f,
                steer = 0f,
                long = 0f,
            )
        }
    }

    val controlTuning: ControlTuning by lazy {
        runCatching {
            val t = json.getJSONObject("control_tuning")
            ControlTuning(
                pitchFactor = t.optDouble("pitch_factor").toFloat(),
                pitchZone = parseZone(t.optString("pitch_zone")),
                yawFactor = t.optDouble("yaw_factor").toFloat(),
                yawZone = parseZone(t.optString("yaw_zone")),
                steerFactor = t.optDouble("steer_factor").toFloat(),
                steerZone = parseZone(t.optString("steer_zone")),
                longFactor = t.optDouble("long_factor").toFloat(),
                longZone = parseZone(t.optString("long_zone")),
            )
        }.getOrElse {
            ControlTuning(
                pitchFactor = null,
                pitchZone = null,
                yawFactor = null,
                yawZone = null,
                steerFactor = null,
                steerZone = null,
                longFactor = null,
                longZone = null,
            )
        }
    }
}

private fun parseZone(zone: String): PointF? {
    val minAndMax = zone.split("..")
    if (minAndMax.size != 2) {
        return null
    }

    val rawPoint = PointF(
        minAndMax[0].toFloat(),
        minAndMax[1].toFloat(),
    )

    if (rawPoint.x >= rawPoint.y) {
        return null
    }

    return rawPoint
}
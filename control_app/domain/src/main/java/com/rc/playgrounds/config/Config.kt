package com.rc.playgrounds.config

import android.graphics.PointF
import org.json.JSONObject
import com.rc.playgrounds.config.model.ControlOffsets
import com.rc.playgrounds.config.model.ControlServer
import com.rc.playgrounds.config.model.ControlTuning
import com.rc.playgrounds.config.model.MappingZone

class Config(
    val rawJson: String,
    val errorCollector: (e: Throwable) -> Unit = {},
) {
    private val json by lazy {
        runCatching {
            JSONObject(rawJson)
        }.getOrElse { JSONObject() }
    }
    val remoteStreamCmd: String
        get() = runCatching {
            json.getJSONObject("stream").getString("remote_cmd")
        }.getOrElse { "" }

    val streamLocalCmd: String
        get() = runCatching {
            json.getJSONObject("stream").getString("local_cmd")
        }.getOrElse { "" }

    val controlServer: ControlServer? by lazy {
        runCatching {
            val t = json.getJSONObject("control_server")
            ControlServer(
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
        }.onFailure {
            errorCollector(it)
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
                longZones = parseZones(t.optJSONObject("long_zones")),
            )
        }
        .onFailure { errorCollector.invoke(it) }
        .getOrElse {
            ControlTuning(
                pitchFactor = null,
                pitchZone = null,
                yawFactor = null,
                yawZone = null,
                steerFactor = null,
                steerZone = null,
                longFactor = null,
                longZone = null,
                longZones = emptyList(),
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

private fun parseZones(zones: JSONObject?): List<MappingZone> {
    if (zones == null) {
        return emptyList()
    }
    val unsortedMapping = mutableListOf<PointF>()
    zones.keys().forEach { key ->
        val value = zones.getString(key)
        unsortedMapping.add(PointF(key.toFloat(), value.toFloat()))
    }

    val results = mutableListOf<MappingZone>()
    var srcZone = PointF(Float.NaN, Float.NaN)
    var dstZone = PointF(Float.NaN, Float.NaN)

    val mapping = unsortedMapping.sortedBy { it.x }
    mapping.forEach {
        if (results.isEmpty()) {
            results.add(MappingZone(
                src = PointF(it.x, Float.NaN),
                dst = PointF(it.y, Float.NaN),
            ))
            return@forEach
        }

        val lastResult = results.last()

        if (lastResult.src.y.isNaN()) {
            lastResult.src.y = it.x
            lastResult.dst.y = it.y
            return@forEach
        }

        results.add(MappingZone(
            src = PointF(lastResult.src.y, it.x),
            dst = PointF(lastResult.dst.y, it.y),
        ))
    }

    return results
}


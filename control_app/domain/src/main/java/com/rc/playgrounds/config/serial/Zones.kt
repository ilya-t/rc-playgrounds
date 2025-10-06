package com.rc.playgrounds.config.serial

import android.graphics.PointF
import com.rc.playgrounds.config.model.MappingZone

internal object Zones {
    fun parseZone(zone: String?): PointF? {
        if (zone == null) {
            return null
        }
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

    fun parseZones(zonesStr: String?): List<MappingZone> {
        if (zonesStr == null) {
            return emptyList()
        }

        val unsortedMapping: List<PointF> = zonesStr.replace(" ", "").split(";").mapNotNull { kvp ->
            if (kvp.isEmpty()) {
                return@mapNotNull null
            }
            val keyAndValue = kvp.split(":")
            if (keyAndValue.size != 2) {
                throw IllegalArgumentException(
                    "Invalid format! Excepting pair of numbers at '$zonesStr', got: '$kvp'")
            }

            PointF(keyAndValue[0].toFloat(), keyAndValue[1].toFloat())
        }

        val results = mutableListOf<MappingZone>()

        val mapping = unsortedMapping.sortedBy { it.x }
        mapping.forEach {
            if (results.isEmpty()) {
                results.add(
                    MappingZone(
                    src = PointF(it.x, Float.NaN),
                    dst = PointF(it.y, Float.NaN),
                )
                )
                return@forEach
            }

            val lastResult = results.last()

            if (lastResult.src.y.isNaN()) {
                lastResult.src.y = it.x
                lastResult.dst.y = it.y
                return@forEach
            }

            results.add(
                MappingZone(
                src = PointF(lastResult.src.y, it.x),
                dst = PointF(lastResult.dst.y, it.y),
            )
            )
        }

        return results
    }
}
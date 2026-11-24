package com.rc.playgrounds.config.stream

import kotlinx.serialization.Serializable

@Serializable
data class QualityProfile(
    val width: Int,
    val height: Int,
    val framerate: Int,
    val bitrate: Int,
) {
    companion object {
        private val LOW = QualityProfile(
            width = 320,
            height = 240,
            framerate = 30,
            bitrate = 800_000, // H.264 (Baseline)
        )

        private val MEDIUM = QualityProfile(
            width = 640,
            height = 480,
            framerate = 30,
            bitrate = 1_600_000, // H.264 (Baseline)
        )

        private val HIGH = QualityProfile(
            width = 1024,
            height = 778,
            framerate = 30,
            bitrate = 3_000_000, // H.264 (Baseline)
        )

        private val HD_READY = QualityProfile(
            width = 1280,
            height = 720,
            framerate = 30,
            bitrate = 4_200_000, // H.264 (Baseline)
        )

        private val FULL_HD = QualityProfile(
            width = 1920,
            height = 1080,
            framerate = 30,
            bitrate = 8_000_000, // H.264 (Baseline)
        )

        val DEFAULT_PROFILES = listOf(LOW, MEDIUM, HIGH, HD_READY, FULL_HD)
    }
}
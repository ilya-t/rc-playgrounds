package com.rc.playgrounds.remote.stream

data class StreamParameters(
    val width: Int,
    val height: Int,
    val framerate: Int,
    val bitrate: Int,
) {
    companion object {
        val LOW = StreamParameters(
            width = 320,
            height = 240,
            framerate = 30,
            bitrate = 800_000, // H.264 (Baseline)
        )

        val MEDIUM = StreamParameters(
            width = 640,
            height = 480,
            framerate = 30,
            bitrate = 1_600_000, // H.264 (Baseline)
        )

        val HIGH = StreamParameters(
            width = 1024,
            height = 778,
            framerate = 30,
            bitrate = 3_000_000, // H.264 (Baseline)
        )

        val HD_READY = StreamParameters(
            width = 1280,
            height = 720,
            framerate = 30,
            bitrate = 4_200_000, // H.264 (Baseline)
        )

        val FULL_HD = StreamParameters(
            width = 1920,
            height = 1080,
            framerate = 30,
            bitrate = 8_000_000, // H.264 (Baseline)
        )

        val H264_OPTIONS = listOf(LOW, MEDIUM, HIGH, HD_READY, FULL_HD)
    }
}
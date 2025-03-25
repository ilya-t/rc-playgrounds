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
            bitrate = 1_000_000,
        )
    }
}
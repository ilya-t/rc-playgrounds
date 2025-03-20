package com.rc.playgrounds.status.gstreamer

sealed interface Event {
    val time: Long

    class Error(
        val error: Throwable,
        override val time: Long = System.currentTimeMillis(),
    ) : Event

    class Message(
        val message: String,
        override val time: Long = System.currentTimeMillis(),
    ) : Event
}
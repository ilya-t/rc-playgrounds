package com.rc.playgrounds.common

fun interface Clock {
    fun millis(): Long
}

object SystemClock : Clock {
    override fun millis(): Long = System.currentTimeMillis()
}

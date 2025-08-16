package com.rc.playgrounds.control.steering

typealias SteerValue = Float

fun SteerValue.trim(): SteerValue {
    return this.coerceIn(-1f, 1f)
}


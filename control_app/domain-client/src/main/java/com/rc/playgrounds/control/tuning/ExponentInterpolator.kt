package com.rc.playgrounds.control.tuning

import android.view.animation.BaseInterpolator
import kotlin.math.exp

/**
 * @param factor The curvature of the exponential function.
 * At [factor] 1.0, the acceleration is subtle (near linear).
 * Around 5.0, it creates a classic "slow start, fast finish" feel.
 * At 10.0 or higher, the acceleration becomes explosive.
 */
class ExponentInterpolator(private val factor: Float = 5.0f) : BaseInterpolator() {
    override fun getInterpolation(input: Float): Float {
        return (exp(factor * input) - 1) / (exp(factor) - 1)
    }
}

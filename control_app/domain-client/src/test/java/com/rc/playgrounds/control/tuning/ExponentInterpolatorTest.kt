package com.rc.playgrounds.control.tuning

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExponentInterpolatorTest {
    private val delta = 0.0001f

    @Test
    fun `test boundaries`() {
        val interpolator = ExponentInterpolator(factor = 5f)

        assertEquals("Start should be 0",
            /*expected=*/ 0f,
            /*actual=*/ interpolator.getInterpolation(0f), delta)
        assertEquals("End should be 1",
            /*expected=*/ 1f,
            /*actual=*/ interpolator.getInterpolation(1f), delta)
    }

    @Test
    fun `test monotonicity`() {
        val interpolator = ExponentInterpolator(factor = 5f)
        var previousValue = 0f

        for (i in 1..100) {
            val input = i / 100f
            val currentValue = interpolator.getInterpolation(input)
            assertTrue("Value at $input should be greater than $previousValue",
                currentValue >= previousValue)
            previousValue = currentValue
        }
    }

    @Test
    fun `test exponential nature compared to parabola`() {
        val factor = 5f
        val interpolator = ExponentInterpolator(factor)

        // At the midpoint (0.5), a parabola (t^2) yields 0.25.
        // An exponent with a factor of 5 yields (~e^2.5 - 1) / (e^5 - 1) ≈ 0.075.
        // This confirms the exponential "dwell" effect where it lags behind a parabola.
        val midValue = interpolator.getInterpolation(0.5f)

        assertTrue("Exponent should be much slower than parabola in the first half",
            midValue < 0.1f)
    }
}

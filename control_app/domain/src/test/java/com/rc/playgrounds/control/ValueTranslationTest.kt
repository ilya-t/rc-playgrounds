package com.rc.playgrounds.control

import org.junit.Assert
import org.junit.Test

class ValueTranslationTest {
    @Test
    fun `translate to reversed values`() {
        val value = translate(0.5f, 0f, 1f, 0.7f, 0.5f)
        Assert.assertEquals(0.6f, value)
    }
}
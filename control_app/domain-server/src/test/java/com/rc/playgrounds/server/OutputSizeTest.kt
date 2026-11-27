package com.rc.playgrounds.server

import org.junit.Assert.assertEquals
import org.junit.Test

class OutputSizeTest {
    @Test
    fun `16 by 9 keeps aspect at 0 and 180`() {
        assertEquals(640 to 360, outputSize(1920, 1080, rotation = 0, targetHeight = 360))
        assertEquals(640 to 360, outputSize(1920, 1080, rotation = 180, targetHeight = 360))
    }

    @Test
    fun `16 by 9 swaps aspect at 90`() {
        assertEquals(202 to 360, outputSize(1920, 1080, rotation = 90, targetHeight = 360))
    }

    @Test
    fun `4 by 3 keeps aspect at 0 and 180`() {
        assertEquals(640 to 480, outputSize(800, 600, rotation = 0, targetHeight = 480))
        assertEquals(640 to 480, outputSize(800, 600, rotation = 180, targetHeight = 480))
    }

    @Test
    fun `4 by 3 swaps aspect at 90`() {
        assertEquals(360 to 480, outputSize(800, 600, rotation = 90, targetHeight = 480))
    }

    @Test
    fun `odd and tiny heights become even and at least 16`() {
        assertEquals(1066 to 600, outputSize(1920, 1080, rotation = 0, targetHeight = 601))
        assertEquals(28 to 16, outputSize(1920, 1080, rotation = 0, targetHeight = 15))
        assertEquals(16 to 16, outputSize(100, 1000, rotation = 0, targetHeight = 16))
    }
}
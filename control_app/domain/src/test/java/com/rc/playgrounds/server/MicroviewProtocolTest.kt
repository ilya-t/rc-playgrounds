package com.rc.playgrounds.server

import com.rc.playgrounds.server.controllink.MicroviewProtocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MicroviewProtocolTest {
    @Test
    fun `neutral frame centers every channel`() {
        val text = ascii(0, ServerControlState())
        assertTrue(text.startsWith("C,0,1500,1500,1500,1500,1500,1500*"))
        assertTrue(text.endsWith("\n"))
        assertEquals(checksum(text), hex(text))
    }

    @Test
    fun `full throttle maps long to motor 2000`() {
        val text = ascii(2, ServerControlState(long = 1f))
        assertTrue(text.startsWith("C,2,2000,1500,1500,1500,1500,1500*"))
        assertEquals(checksum(text), hex(text))
    }

    @Test
    fun `steering extremes and axis order`() {
        val left = ascii(3, ServerControlState(steer = -1f))
        assertTrue(left.startsWith("C,3,1500,1000,1500,1500,1500,1500*"))

        val mapped = ascii(
            9,
            ServerControlState(yaw = 1f, pitch = -1f, steer = 1f, long = -1f),
        )
        assertTrue(mapped.startsWith("C,9,1000,2000,2000,1000,1500,1500*"))
    }

    @Test
    fun `small stick deflection stays at neutral`() {
        val text = ascii(4, ServerControlState(steer = 0.04f, long = -0.044f))
        assertTrue(text.startsWith("C,4,1500,1500,1500,1500,1500,1500*"))
    }

    @Test
    fun `sequence wraps to 16 bits`() {
        assertTrue(ascii(65536, ServerControlState()).startsWith("C,0,"))
        assertTrue(ascii(-1, ServerControlState()).startsWith("C,65535,"))
    }

    @Test
    fun `status checksum must match`() {
        val status = MicroviewProtocol.wrap("S,77,123456,32,1")
        val parsed = MicroviewProtocol.parseStatus(status)
        assertEquals(77, parsed?.sequence)
        assertEquals(123456L, parsed?.uptimeMs)
        assertEquals(32, parsed?.commandAgeMs)
        assertEquals(1, parsed?.deviceState)
        assertNull(MicroviewProtocol.parseStatus(status.replace(",32,", ",33,")))
        assertNull(MicroviewProtocol.parseStatus(MicroviewProtocol.wrap("S,99,12,10,0")))
        assertNull(MicroviewProtocol.parseStatus(MicroviewProtocol.wrap("S,99,12,10,3")))
    }

    private fun ascii(seq: Int, control: ServerControlState): String {
        return MicroviewProtocol.command(seq, control).toString(Charsets.US_ASCII)
    }

    private fun checksum(frame: String): String {
        val payload = frame.substring(0, frame.lastIndexOf('*'))
        var xor = 0
        for (symbol in payload) {
            xor = xor xor symbol.code
        }
        return "%02X".format(xor)
    }

    private fun hex(frame: String): String {
        val star = frame.lastIndexOf('*')
        return frame.substring(star + 1, star + 3)
    }
}

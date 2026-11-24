package com.rc.playgrounds.config.env

import org.junit.Assert.assertEquals
import org.junit.Test

class ApplyEnvTest {

    @Test
    fun `replaces single placeholder`() {
        val env = mapOf("USER" to "alice")
        val result = "Hello @{USER}!".applyEnv(env)
        assertEquals("Hello alice!", result)
    }

    @Test
    fun `replaces multiple placeholders`() {
        val env = mapOf("A" to "1", "B" to "2")
        val result = "A=@{A}, B=@{B}".applyEnv(env)
        assertEquals("A=1, B=2", result)
    }

    @Test
    fun `replaces adjacent placeholders`() {
        val env = mapOf("X" to "foo", "Y" to "bar")
        val result = "@{X}@{Y}".applyEnv(env)
        assertEquals("foobar", result)
    }

    @Test
    fun `leaves missing keys intact`() {
        val env = mapOf("PRESENT" to "ok")
        val result = "x=@{MISSING}, y=@{PRESENT}".applyEnv(env)
        assertEquals("x=@{MISSING}, y=ok", result)
    }

    @Test
    fun `string without placeholders stays unchanged`() {
        val env = emptyMap<String, String>()
        val input = "no placeholders here"
        assertEquals(input, input.applyEnv(env))
    }

    @Test
    fun `unclosed placeholder is preserved`() {
        val env = mapOf("KEY" to "value")
        val result = "start @{KEY and more".applyEnv(env)
        assertEquals("start @{KEY and more", result)
    }

    @Test
    fun `empty key placeholder is preserved`() {
        val env = mapOf("" to "shouldNotMatch")
        val result = "weird @{ } case".applyEnv(env) // note: space prevents empty key anyway
        assertEquals("weird @{ } case", result)
    }

    @Test
    fun `handles unicode keys and values`() {
        val env = mapOf("город" to "∆-city", "emoji" to "😀")
        val result = "City=@{город} @{emoji}".applyEnv(env)
        assertEquals("City=∆-city 😀", result)
    }

    @Test
    fun `repeated occurrences of the same key`() {
        val env = mapOf("N" to "7")
        val result = "@{N} + @{N} = 14".applyEnv(env)
        assertEquals("7 + 7 = 14", result)
    }

    @Test
    fun `placeholder braces in text aren't touched`() {
        val env = mapOf("K" to "v")
        val result = "literal @{ not a key } and @{K}".applyEnv(env)
        assertEquals("literal @{ not a key } and v", result)
    }

    @Test
    fun `large input with many placeholders`() {
        val env = mapOf("A" to "x", "B" to "y", "C" to "z")
        val input = buildString {
            repeat(1000) { append("@{A}@{B}@{C};") }
        }
        val result = input.applyEnv(env)
        assertEquals("xyz;".repeat(1000), result)
    }
}
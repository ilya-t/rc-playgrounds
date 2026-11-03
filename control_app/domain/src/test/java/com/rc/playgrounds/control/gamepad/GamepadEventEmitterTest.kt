package com.rc.playgrounds.control.gamepad

import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@OptIn(ExperimentalCoroutinesApi::class)
class GamepadEventEmitterTest {
    private val scope = TestScope()
    private val eventStream = GamepadEventStream(scope)
    private val emitter = GamepadEventEmitter(eventStream)
    private val motionEvents = mutableListOf<MotionEvent>()

    @After
    fun tearDown() {
        motionEvents.forEach { it.recycle() }
        scope.cancel()
    }

    @Test
    fun `hat motion remains available for Android dpad navigation`() {
        val event = createMotionEvent(axis = MotionEvent.AXIS_HAT_X, value = 1f)

        assertFalse(emitter.onGenericMotionEvent(event))
        assertEquals(GamepadEvent.INITIAL, eventStream.events.value)
    }

    @Test
    fun `stick motion updates controls and remains available for menu navigation`() {
        val event = createMotionEvent(axis = MotionEvent.AXIS_X, value = 0.8f)

        assertFalse(emitter.onGenericMotionEvent(event))
        assertEquals(GamepadEvent.INITIAL.copy(leftStickX = 0.8f), eventStream.events.value)
    }

    @Test
    fun `mouse motion is left to Android without updating gamepad controls`() {
        val event = createMotionEvent(
            axis = MotionEvent.AXIS_X,
            value = 1f,
            source = InputDevice.SOURCE_MOUSE,
        )

        assertFalse(emitter.onGenericMotionEvent(event))
        assertEquals(GamepadEvent.INITIAL, eventStream.events.value)
    }

    @Test
    fun `left stick click and release do not reach Android views`() {
        assertTrue(emitter.onKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_THUMBL)))
        assertTrue(emitter.onKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_THUMBL)))
    }

    @Test
    fun `right stick click and release do not reach Android views`() {
        assertTrue(emitter.onKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_THUMBR)))
        assertTrue(emitter.onKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_THUMBR)))
    }

    @Test
    fun `A click is emitted once and both key phases are consumed`() = scope.runTest {
        // Arrange
        val buttons = mutableListOf<GamepadButtonPress>()
        val collector = launch { eventStream.buttonEvents.collect { buttons.add(it) } }
        runCurrent()

        // Act
        val pressHandled = emitter.onKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_A))
        val releaseHandled = emitter.onKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_A))
        runCurrent()
        collector.cancel()

        // Assert
        assertTrue(pressHandled)
        assertTrue(releaseHandled)
        assertEquals(listOf(GamepadButtonPress.A), buttons)
    }

    @Test
    fun `keyboard enter remains available to Android views`() {
        assertFalse(emitter.onKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER)))
        assertFalse(emitter.onKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER)))
    }

    private fun createMotionEvent(
        axis: Int,
        value: Float,
        source: Int = InputDevice.SOURCE_JOYSTICK,
    ): MotionEvent {
        val pointer = MotionEvent.PointerProperties().apply { id = 0 }
        val coordinates = MotionEvent.PointerCoords().apply { setAxisValue(axis, value) }
        return MotionEvent.obtain(
            0L, 0L, MotionEvent.ACTION_MOVE, 1,
            arrayOf(pointer), arrayOf(coordinates),
            0, 0, 1f, 1f, 0, 0, source, 0,
        ).also { motionEvents.add(it) }
    }
}

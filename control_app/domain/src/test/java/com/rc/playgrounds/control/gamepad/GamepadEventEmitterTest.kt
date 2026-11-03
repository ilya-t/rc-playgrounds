package com.rc.playgrounds.control.gamepad

import android.view.InputDevice
import android.view.MotionEvent
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestScope
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
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

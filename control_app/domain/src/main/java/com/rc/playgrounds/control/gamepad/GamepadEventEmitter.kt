package com.rc.playgrounds.control.gamepad

import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent


class GamepadEventEmitter(
    private val eventStream: GamepadEventStream
) {

    fun restart() {
        eventStream.emit(GamepadEvent.INITIAL)
    }

    fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if ((event.source and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK &&
            event.action == MotionEvent.ACTION_MOVE
        ) {
            val e = GamepadEvent(
                leftStickX = event.getAxisValue(MotionEvent.AXIS_X),
                leftStickY = event.getAxisValue(MotionEvent.AXIS_Y),

                rightStickX = event.getAxisValue(MotionEvent.AXIS_Z),
                rightStickY = event.getAxisValue(MotionEvent.AXIS_RZ),

                leftTrigger = event.getAxisValue(MotionEvent.AXIS_LTRIGGER),
                rightTrigger = event.getAxisValue(MotionEvent.AXIS_RTRIGGER),
            )
            eventStream.emit(e)

            // Log or use the values
//            Static.output(
//                "- Left Stick: (${e.leftStickX}, ${e.leftStickY})"+
//                "\n- Right Stick: (${e.rightStickX}, ${e.rightStickY})"+
//                "\n- Triggers: L=${e.leftTrigger} R=${e.rightTrigger}"
//            )
            return false
        }
//        Static.output(MotionEvent.actionToString(event.action))
        return false
    }

    fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> {
                eventStream.emit(GamepadButtonPress.Up)
                return true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                // you may add event.isFromSource(InputDevice.SOURCE_DPAD) but
                // not every gamepad with dpad has this source.
                eventStream.emit(GamepadButtonPress.Down)
                return true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                eventStream.emit(GamepadButtonPress.Left)
                return true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                eventStream.emit(GamepadButtonPress.Right)
                return true
            }
            KeyEvent.KEYCODE_BUTTON_A -> {
                eventStream.emit(GamepadButtonPress.A)
                return true
            }
            KeyEvent.KEYCODE_BUTTON_B -> {
                eventStream.emit(GamepadButtonPress.B)
                return true
            }
            KeyEvent.KEYCODE_BUTTON_X -> {
                //Static.output("Button [X]")
                return true
            }
            KeyEvent.KEYCODE_BUTTON_Y -> {
                //Static.output("Button [Y]")
                return true
            }
            KeyEvent.KEYCODE_BUTTON_START -> {
                eventStream.emit(GamepadButtonPress.START)
                return true
            }
            KeyEvent.KEYCODE_BUTTON_SELECT -> {
                eventStream.emit(GamepadButtonPress.SELECT)
                return true
            }
            KeyEvent.KEYCODE_BUTTON_L1 -> {
                eventStream.emit(GamepadButtonPress.LeftBumper)
                return true
            }
            KeyEvent.KEYCODE_BUTTON_R1 -> {
                eventStream.emit(GamepadButtonPress.RightBumper)
                return true
            }
        }
        return false
    }

}

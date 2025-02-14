package rc.playgrounds.telemetry.gamepad

import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import com.testspace.core.Static


class GamepadEventEmitter(
    private val eventStream: GamepadEventStream
) {

    fun restart() {
//        TODO("Not yet implemented")
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
            Static.output(
                "- Left Stick: (${e.leftStickX}, ${e.leftStickY})"+
                "\n- Right Stick: (${e.rightStickX}, ${e.rightStickY})"+
                "\n- Triggers: L=${e.leftTrigger} R=${e.rightTrigger}"
            )
            return false
        }
        Static.output(MotionEvent.actionToString(event.action))
        return false
    }

    fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_BUTTON_A -> {
                Static.output("Button [A]")
                return true
            }
            KeyEvent.KEYCODE_BUTTON_B -> {
                Static.output("Button [B]")
                return true
            }
            KeyEvent.KEYCODE_BUTTON_X -> {
                Static.output("Button [X]")
                return true
            }
            KeyEvent.KEYCODE_BUTTON_Y -> {
                Static.output("Button [Y]")
                return true
            }
        }
        return false
    }

}

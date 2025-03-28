package com.rc.playgrounds.control.lock

import com.rc.playgrounds.control.gamepad.GamepadButtonPress
import com.rc.playgrounds.control.gamepad.GamepadEventStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class LockModel(
    private val controlLock: ControlLock,
    private val gamepadEventStream: GamepadEventStream,
    private val scope: CoroutineScope,
) {
    fun onBackPress() {
        controlLock.unlock()
    }

    val viewModel: Flow<LockViewModel> = controlLock.locked.map { locked ->
        LockViewModel(
            visible = locked,
        )
    }

    init {
        scope.launch {
            gamepadEventStream.buttonEvents.collect {
                when (it) {
                    GamepadButtonPress.DpadDown -> Unit
                    GamepadButtonPress.DpadUp -> Unit
                    GamepadButtonPress.B,
                    GamepadButtonPress.SELECT,
                    GamepadButtonPress.START -> controlLock.toggle()
                }
            }
        }
    }
}

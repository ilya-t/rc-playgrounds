package com.rc.playgrounds.presentation.lock

import com.rc.playgrounds.control.gamepad.GamepadButtonPress
import com.rc.playgrounds.control.gamepad.access.EventConsumer
import com.rc.playgrounds.control.gamepad.access.GamepadEventsByConsumer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class LockView(
    private val lockModel: LockModel,
    private val scope: CoroutineScope,
    private val gamepadEventStream: GamepadEventsByConsumer,
) {
    private var job: Job? = null

    init {
        scope.launch {
            lockModel.viewModel.collect { viewModel ->
                job?.cancel()
                job = null
                when (viewModel) {
                    LockViewModel.Hidden -> {
                        gamepadEventStream.releaseFocus(EventConsumer.LockView)
                    }
                    LockViewModel.Visible -> {
                        gamepadEventStream.acquireFocus(EventConsumer.LockView)
                        job = scope.launch {
                            gamepadEventStream.buttonEventsFor(EventConsumer.LockView).collect { buttonPress ->
                                when (buttonPress) {
                                    GamepadButtonPress.B -> lockModel.onBackPress()
                                    else -> Unit
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
package com.rc.playgrounds.control.lock

import com.rc.playgrounds.control.gamepad.GamepadButtonPress
import com.rc.playgrounds.control.gamepad.GamepadEventStream
import com.rc.playgrounds.navigation.ActiveScreenProvider
import com.rc.playgrounds.navigation.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class LockModel(
    private val activeScreenProvider: ActiveScreenProvider,
    private val controlLock: ControlLock,
    private val gamepadEventStream: GamepadEventStream,
    private val scope: CoroutineScope,
) {
    private var job: Job? = null
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
            activeScreenProvider.screen.collect {
                job?.cancel()
                job = null
                when (it) {
                    Screen.LOCK_SCREEN -> {
                        job = scope.launch {
                            gamepadEventStream.buttonEvents.collect {
                                when (it) {
                                    GamepadButtonPress.SELECT,
                                    GamepadButtonPress.START,
                                    GamepadButtonPress.B -> controlLock.toggle()
                                    else -> Unit
                                }
                            }

                        }
                    }
                    else -> Unit
                }

            }
        }
    }
}

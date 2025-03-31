package com.rc.playgrounds.presentation.main

import com.rc.playgrounds.control.gamepad.GamepadButtonPress
import com.rc.playgrounds.control.gamepad.GamepadEventStream
import com.rc.playgrounds.control.lock.ControlLock
import com.rc.playgrounds.navigation.ActiveScreenProvider
import com.rc.playgrounds.navigation.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainModel(
    private val activeScreenProvider: ActiveScreenProvider,
    private val scope: CoroutineScope,
    private val gamepadEventStream: GamepadEventStream,
    private val lock: ControlLock,
) {
    private val _viewModel = MutableStateFlow<MainViewModel>(MainViewModel.Visible)
    val viewModel: Flow<MainViewModel> = _viewModel
    private var job: Job? = null

    init {
        scope.launch {
            activeScreenProvider.screen.collect {
                when (it) {
                    Screen.MAIN -> _viewModel.value = MainViewModel.Visible
                    else -> _viewModel.value = MainViewModel.Hidden
                }
            }
        }

        scope.launch {
            _viewModel.collect { viewModel ->
                job?.cancel()
                job = null
                when (viewModel) {
                    MainViewModel.Hidden -> {
                        Unit
                    }
                    is MainViewModel.Visible -> {
                        activeScreenProvider.switchTo(Screen.MAIN)
                        job = scope.launch {
                            gamepadEventStream.buttonEvents.collect { button ->
                                when (button) {
                                    GamepadButtonPress.B -> {
                                        lock.lock()
                                    }
                                    GamepadButtonPress.START,
                                    GamepadButtonPress.SELECT -> {
                                        activeScreenProvider.switchTo(Screen.QUICK_CONFIG)
                                    }
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
package com.rc.playgrounds.presentation.main

import com.rc.playgrounds.control.gamepad.GamepadButtonPress
import com.rc.playgrounds.control.gamepad.GamepadEventStream
import com.rc.playgrounds.control.lock.ControlLock
import com.rc.playgrounds.navigation.ActiveScreenProvider
import com.rc.playgrounds.navigation.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

private const val SHOW_DURATION = 2000L

class MainModel(
    private val activeScreenProvider: ActiveScreenProvider,
    private val scope: CoroutineScope,
    private val gamepadEventStream: GamepadEventStream,
    private val lock: ControlLock,
) {
    private val userActivity = MutableStateFlow(System.currentTimeMillis())

    fun onScreenClick() {
        userActivity.value = System.currentTimeMillis()
    }

    private val _viewModel = MutableStateFlow<MainViewModel>(
        MainViewModel.Visible(showControls = true)
    )
    val viewModel: Flow<MainViewModel> = _viewModel.filterNotNull()
    private var job: Job? = null

    init {
        scope.launch {
            combine(
                activeScreenProvider.screen,
                userActivity,
            ) { a, b -> a to b }
                .collect { (screen, userClickTime) ->
                    when (screen) {
                        Screen.MAIN -> {
                            val timePassed: Long = System.currentTimeMillis() - userClickTime
                            _viewModel.value = MainViewModel.Visible(
                                showControls = timePassed < SHOW_DURATION
                            )

                            scope.launch {
                                delay(SHOW_DURATION)
                                if (_viewModel.value is MainViewModel.Visible) {
                                    _viewModel.value = MainViewModel.Visible(
                                        showControls = false
                                    )
                                }
                            }
                        }
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
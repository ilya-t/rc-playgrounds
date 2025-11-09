package com.rc.playgrounds.presentation.main

import com.rc.playgrounds.control.gamepad.GamepadButtonPress
import com.rc.playgrounds.control.gamepad.access.EventConsumer
import com.rc.playgrounds.control.gamepad.access.GamepadEventsByConsumer
import com.rc.playgrounds.control.lock.ControlLock
import com.rc.playgrounds.control.quick.QuickConfigState
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
    private val gamepadEventStream: GamepadEventsByConsumer,
    private val lock: ControlLock,
    private val quickConfig: QuickConfigState,
) {
    private val userActivity = MutableStateFlow(System.currentTimeMillis())

    fun onScreenClick() {
        userActivity.value = System.currentTimeMillis()
    }

    private val _viewModel = MutableStateFlow<MainViewModel>(
        createVisibleViewModel(showControls = true)
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
                    _viewModel.value = when (screen) {
                        Screen.MAIN -> {
                            val timePassed: Long = System.currentTimeMillis() - userClickTime

                            scope.launch {
                                delay(SHOW_DURATION)
                                if (_viewModel.value is MainViewModel.Visible) {
                                    _viewModel.value = createVisibleViewModel(showControls = false)
                                }
                            }

                            createVisibleViewModel(showControls = timePassed < SHOW_DURATION)
                        }

                        else -> MainViewModel.Hidden
                    }
                }
        }

        scope.launch {
            _viewModel.collect { viewModel ->
                job?.cancel()
                job = null
                when (viewModel) {
                    MainViewModel.Hidden -> {
                        gamepadEventStream.releaseFocus(EventConsumer.MainView)
                    }
                    is MainViewModel.Visible -> {
                        activeScreenProvider.switchTo(Screen.MAIN)
                        gamepadEventStream.acquireFocus(EventConsumer.MainView)
                        job = scope.launch {
                            gamepadEventStream.buttonEventsFor(EventConsumer.MainView).collect { button ->
                                when (button) {
                                    GamepadButtonPress.B -> {
                                        viewModel.onBKeyPressed()
                                    }
                                    GamepadButtonPress.START,
                                    GamepadButtonPress.SELECT -> {
                                        viewModel.onSelectStartPressed()
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

    private fun createVisibleViewModel(showControls: Boolean): MainViewModel.Visible {
        return MainViewModel.Visible(
            showControls = showControls,
            onSelectStartPressed = {
                if (!quickConfig.opened.value) {
                    quickConfig.toggle()
                }
            },
            onBKeyPressed = {
                if (!quickConfig.opened.value) {
                    lock.lock()
                }
            },
        )
    }
}
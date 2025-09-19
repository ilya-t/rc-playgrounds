package com.rc.playgrounds.presentation.lock

import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import com.rc.playgrounds.control.gamepad.GamepadButtonPress
import com.rc.playgrounds.control.gamepad.GamepadEventStream
import com.rc.playgrounds.navigation.ActiveScreenProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class LockView(
    private val activity: AppCompatActivity,
    private val lockModel: LockModel,
    private val scope: CoroutineScope,
    private val gamepadEventStream: GamepadEventStream,
    activeScreenProvider: ActiveScreenProvider,
) {
    private var job: Job? = null

    init {
        scope.launch {
            lockModel.viewModel.collect { viewModel ->
                job?.cancel()
                job = null
                when (viewModel) {
                    LockViewModel.Hidden -> Unit // Do nothing. This is a job for OverlayView.
                    is LockViewModel.Visible -> {
                        job = scope.launch {
                            gamepadEventStream.buttonEvents.collect { buttonPress ->
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

        activity.onBackPressedDispatcher.addCallback {
            lockModel.onBackPress()
        }
    }
}
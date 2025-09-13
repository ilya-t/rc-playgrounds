package com.rc.playgrounds.presentation.quickconfig

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import com.rc.playgrounds.control.gamepad.GamepadButtonPress
import com.rc.playgrounds.control.gamepad.GamepadEventStream
import com.rc.playgrounds.domain.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class QuickConfigView(
    private val activity: AppCompatActivity,
    private val quickConfigModel: QuickConfigModel,
    private val scope: CoroutineScope,
    private val gamepadEventStream: GamepadEventStream,
) {
    private val currentResolution: AppCompatTextView = activity.findViewById(R.id.current_resolution)
    private val changeHint: AppCompatTextView = activity.findViewById(R.id.change_hint)
    private val steerOffset: AppCompatTextView = activity.findViewById(R.id.steer_offset)
    private val steerOffsetHint: AppCompatTextView = activity.findViewById(R.id.steer_offset_hint)
    private var job: Job? = null

    init {
        scope.launch {
            quickConfigModel.viewModel
                .collect { viewModel: QuickConfigViewModel ->
                    job?.cancel()
                    job = null
                    when (viewModel) {
                        QuickConfigViewModel.Hidden -> Unit
                        is QuickConfigViewModel.Visible -> {
                            currentResolution.text = viewModel.resolution
                            steerOffset.text = viewModel.steeringOffset
                            job = scope.launch {
                                gamepadEventStream.buttonEvents.collect {
                                    processGampadButtonPress(viewModel, it)
                                }
                            }
                        }
                    }
                }
        }
    }

    private fun processGampadButtonPress(
        viewModel: QuickConfigViewModel.Visible,
        buttonPress: GamepadButtonPress
    ) {
        when (buttonPress) {
            GamepadButtonPress.A -> viewModel.onBackButton()
            GamepadButtonPress.B -> viewModel.onBackButton()
            GamepadButtonPress.Down -> viewModel.onButtonDownPressed()
            GamepadButtonPress.Up -> viewModel.onButtonUpPressed()
            GamepadButtonPress.SELECT -> viewModel.onBackButton()
            GamepadButtonPress.START -> viewModel.onBackButton()
            GamepadButtonPress.Left -> viewModel.onButtonLeftPressed()
            GamepadButtonPress.Right -> viewModel.onButtonRightPressed()
            else -> Unit
        }
    }
}
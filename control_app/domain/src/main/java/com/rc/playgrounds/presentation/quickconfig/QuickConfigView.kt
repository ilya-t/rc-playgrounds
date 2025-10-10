package com.rc.playgrounds.presentation.quickconfig

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
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
    private val composeView: ComposeView = activity.findViewById(R.id.quick_config_compose_view)
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
                            composeView.setContent {
                                Render(viewModel)
                            }
                            job = scope.launch {
                                gamepadEventStream.buttonEvents.collect {
                                    processGamepadButtonPress(viewModel, it)
                                }
                            }
                        }
                    }
                }
        }
    }

    private fun processGamepadButtonPress(
        viewModel: QuickConfigViewModel.Visible,
        buttonPress: GamepadButtonPress
    ) {
        when (buttonPress) {
            GamepadButtonPress.A -> viewModel.onApplyButton()
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

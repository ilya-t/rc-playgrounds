package com.rc.playgrounds.presentation.quickconfig

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                                QuickConfigContent(viewModel)
                            }
                            job = scope.launch {
                                gamepadEventStream.buttonEvents.collect {
                                    processGamepadButtonPress(viewModel, it)
                                }
                            }
                        }

                        is QuickConfigViewModel.DashboardVisible -> {
                            composeView.setContent {
                                RenderDashboard(viewModel)
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

    private fun processGamepadButtonPress(
        viewModel: QuickConfigViewModel.DashboardVisible,
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

@Composable
private fun QuickConfigContent(viewModel: QuickConfigViewModel.Visible) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x88000000)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 24.dp),
        ) {
            QuickConfigPrimaryText(viewModel.resolution)
            QuickConfigHintText("up/down to change")
            QuickConfigPrimaryText(viewModel.steeringOffset)
            QuickConfigHintText("left/right to change")
        }
    }
}

@Composable
private fun QuickConfigPrimaryText(text: String) {
    Text(
        text = text,
        fontSize = 28.sp,
        color = Color.White,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun QuickConfigHintText(text: String) {
    Text(
        text = text,
        fontSize = 16.sp,
        color = Color(0xFFD3D3D3),
        textAlign = TextAlign.Center,
    )
}
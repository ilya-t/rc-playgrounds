package com.rc.playgrounds.presentation.quickconfig

import com.rc.playgrounds.config.ActiveConfigProvider
import com.rc.playgrounds.control.quick.QuickConfigState
import com.rc.playgrounds.navigation.ActiveScreenProvider
import com.rc.playgrounds.navigation.Screen
import com.rc.playgrounds.remote.stream.StreamQualityProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class QuickConfigModel(
    scope: CoroutineScope,
    activeScreenProvider: ActiveScreenProvider,
    private val activeConfigProvider: ActiveConfigProvider,
    qualityProvider: StreamQualityProvider,
    quickConfig: QuickConfigState,
) {
    private val _viewModel = MutableStateFlow<QuickConfigViewModel>(QuickConfigViewModel.Hidden)
    val viewModel: StateFlow<QuickConfigViewModel> = _viewModel

    init {
        scope.launch {
            combine(
                activeConfigProvider.configFlow,
                quickConfig.opened,
                qualityProvider.currentQuality,
            ) { config, opened, p ->
                if (opened) {
                    QuickConfigViewModel.Visible(
                        resolution = "${p.width}x${p.height} ${p.framerate}fps (${p.bitrate / 1_000_000f}mbit/s)",
                        steeringOffset = "steer offset: %.3f".format(config.controlOffsets.steer),
                        onButtonUpPressed = {
                            qualityProvider.nextQuality()
                        },
                        onButtonDownPressed = {
                            qualityProvider.prevQuality()
                        },
                        onButtonLeftPressed = {
                            shiftSteerOffset(-STEER_OFFSET_STEP)
                        },
                        onButtonRightPressed = {
                            shiftSteerOffset(STEER_OFFSET_STEP)
                        },
                        onBackButton = { activeScreenProvider.switchTo(Screen.MAIN) }
                    )
                } else {
                    QuickConfigViewModel.Hidden
                }
            }
            .collect {
                _viewModel.value = it
            }
        }
    }

    private fun shiftSteerOffset(value: Float) {
        activeConfigProvider.update { config ->
            config.copy(
                controlOffsets = config.controlOffsets.copy(
                    steer = (config.controlOffsets.steer + value).coerceIn(-1f, 1f)
                )
            )
        }
    }
}


private const val STEER_OFFSET_STEP = 0.005f
package com.rc.playgrounds.presentation.quickconfig

import android.graphics.Point
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
    private val focusState = MutableStateFlow<Point>(Point())

    init {
        scope.launch {
            combine(
                activeConfigProvider.configFlow,
                quickConfig.opened,
                qualityProvider.currentQuality,
                focusState,
            ) { config, opened, p, focus ->
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
                    val envProfiles = listOf(
                        EnvironmentProfiles(
                            title = "modes",
                            profiles = listOf(
                                EnvironmentProfile(
                                    title = "normal",
                                ),
                                EnvironmentProfile(
                                    title = "crawling",
                                ),
                                EnvironmentProfile(
                                    title = "max long",
                                ),
                            ),
                        ),
                        EnvironmentProfiles(
                            title = "resolution",
                            profiles = listOf(
                                EnvironmentProfile(
                                    title = "320x240",
                                ),
                                EnvironmentProfile(
                                    title = "640x480",
                                ),
                                EnvironmentProfile(
                                    title = "800x600",
                                ),
                                EnvironmentProfile(
                                    title = "1024x768",
                                ),
                            ),
                        )
                    )

                    val maxX = envProfiles.lastIndex
                    val maxY = (envProfiles.getOrNull(focus.x)?.profiles?.lastIndex ?: -1) + 1

                    QuickConfigViewModel.DashboardVisible(
                        elementGroups = envProfiles.mapIndexed { x, profiles ->
                            ElementGroup(
                                title = profiles.title,
                                elements = profiles.profiles.mapIndexed { i, profile ->
                                    val y = i + 1
                                    Element(
                                        active = false,
                                        focused = focus.x == x && focus.y == y,
                                        title = profile.title,
                                    )
                                },
                                active = false,
                                focused = focus.x == x && focus.y == 0
                            )
                        },
                        onButtonUpPressed = {
                            moveFocus(y = (focus.y - 1).coerceIn(0, maxY))
                        },
                        onButtonDownPressed = {
                            moveFocus(y = (focus.y + 1).coerceIn(0, maxY))
                        },
                        onButtonLeftPressed = {
                            moveFocus(x = (focus.x - 1).coerceIn(0, maxX))
                        },
                        onButtonRightPressed = {
                            moveFocus(x = (focus.x + 1).coerceIn(0, maxX))
                        },
                        onApplyButton = { },
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

    private fun moveFocus(x: Int = focusState.value.x, y: Int = focusState.value.y) {
        focusState.value = Point(x, y)
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
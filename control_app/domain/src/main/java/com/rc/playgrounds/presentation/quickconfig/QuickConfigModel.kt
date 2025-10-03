package com.rc.playgrounds.presentation.quickconfig

import a.debug.stuff.Log
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
                    val envOverrides: List<EnvironmentOverrides> = config.envOverrides
                    val maxX = envOverrides.lastIndex
                    val maxY = (envOverrides.getOrNull(focus.x)?.profiles?.lastIndex ?: -1) + 1
                    Log.that("making profile from config:${config.hashCode()} opened: $opened: quality: $p focus: $focus")

                    val elementGroups: List<ElementGroup> = envOverrides.mapIndexed { x, override ->
                        ElementGroup(
                            title = override.name,
                            elements = override.profiles.mapIndexed { i, profile ->
                                val y = i + 1
                                Element(
                                    active = false,
                                    focused = focus.x == x && focus.y == y,
                                    title = profile.name,
                                    onClick = {
                                        toggleElement(
                                            overrideName = override.name,
                                            profileName = profile.name,
                                        )
                                    }
                                )
                            },
                            active = false,
                            focused = focus.x == x && focus.y == 0
                        )
                    }
                    val focusPoint = focusState.value
                    QuickConfigViewModel.DashboardVisible(
                        elementGroups = elementGroups,
                        onButtonUpPressed = {
                            moveFocus(elementGroups, y = (focus.y - 1).coerceIn(0, maxY))
                        },
                        onButtonDownPressed = {
                            moveFocus(elementGroups, y = (focus.y + 1).coerceIn(0, maxY))
                        },
                        onButtonLeftPressed = {
                            moveFocus(elementGroups, x = (focus.x - 1).coerceIn(0, maxX))
                        },
                        onButtonRightPressed = {
                            moveFocus(elementGroups, x = (focus.x + 1).coerceIn(0, maxX))
                        },
                        onApplyButton = { onTileClicked(elementGroups, focusPoint) },
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

    private fun toggleElement(overrideName: String, profileName: String) {
        activeConfigProvider.update { config ->
            val override = config.envOverrides.find { it.name == overrideName } ?: return@update config
            val p = override.profiles.find { it.name == profileName } ?: return@update config
            val targetProfileIndex = override.profiles.indexOf(p)
            val newActiveIndex = if (override.lastActiveIndex == targetProfileIndex) {
                (override.lastActiveIndex - 1).coerceAtLeast(-1)
            } else {
                targetProfileIndex.coerceAtMost(override.profiles.lastIndex)
            }

            if (newActiveIndex == override.lastActiveIndex) {
                return@update config
            }

            val overrides = config.envOverrides.toMutableList()
            overrides[overrides.indexOf(override)] = override.copy(
                lastActiveIndex = newActiveIndex
            )

            config.copy(envOverrides = overrides)
        }

    }

    private fun onTileClicked(elementGroups: List<ElementGroup>, focusPoint: Point) {
        elementGroups.getOrNull(focusPoint.x)
            ?.elements?.getOrNull(focusPoint.y)?.onClick()
    }
    private fun moveFocus(elementGroups: List<ElementGroup>,
                          x: Int = focusState.value.x,
                          y: Int = focusState.value.y) {
        //TODO: trim point coords according to current column.
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
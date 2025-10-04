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
    private val focusState = MutableStateFlow(FocusPoint())

    init {
        scope.launch {
            combine(
                activeConfigProvider.configFlow,
                quickConfig.opened,
                qualityProvider.currentQuality,
                focusState,
            ) { config, opened, p, focus ->
                if (opened) {
                    val envOverrides: List<EnvironmentOverrides> = config.envOverrides
                    val builtInGroups = listOf(
                        ElementGroup(
                            title = "steer offset: %.3f".format(config.controlOffsets.steer),
                            active = false,
                            focused = focus.x == 0 && focus.y == 0,
                            elements = listOf(
                                Element(
                                    active = false,
                                    focused = focus.x == 0 && focus.y == 1,
                                    title = "-$STEER_OFFSET_STEP",
                                    onClick = {
                                        shiftSteerOffset(-STEER_OFFSET_STEP)
                                    }
                                ),
                                Element(
                                    active = false,
                                    focused = focus.x == 0 && focus.y == 2,
                                    title = "+$STEER_OFFSET_STEP",
                                    onClick = {
                                        shiftSteerOffset(STEER_OFFSET_STEP)
                                    }
                                )
                            )
                        )
                    )
                    val elementGroups: List<ElementGroup> = builtInGroups + envOverrides.mapIndexed { x, override: EnvironmentOverrides ->
                        val x = x + builtInGroups.size
                        ElementGroup(
                            title = override.name,
                            elements = override.profiles.mapIndexed { i, profile ->
                                val y = i + 1
                                Element(
                                    active = i < (override.lastActiveIndex ?: -1),
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
                            moveFocus(elementGroups, focusPoint, y = focus.y - 1)
                        },
                        onButtonDownPressed = {
                            moveFocus(elementGroups, focusPoint, y = focus.y + 1)
                        },
                        onButtonLeftPressed = {
                            moveFocus(elementGroups, focusPoint, x = focus.x - 1)
                        },
                        onButtonRightPressed = {
                            moveFocus(elementGroups, focusPoint, x = focus.x + 1)
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

    private fun onTileClicked(elementGroups: List<ElementGroup>, focusPoint: FocusPoint) {
        elementGroups.getOrNull(focusPoint.x)
            ?.elements?.getOrNull(focusPoint.y)?.onClick()
    }

    private fun moveFocus(
        elementGroups: List<ElementGroup>,
        current: FocusPoint,
        x: Int = focusState.value.x,
        y: Int = focusState.value.y,
    ) {
        val trimmedX = x.coerceIn(0, elementGroups.lastIndex)
        val lastColumnElementIndex = elementGroups.getOrNull(trimmedX)?.elements?.lastIndex ?: -1
        val trimmedY = if (lastColumnElementIndex == -1) {
            0
        } else {
            y.coerceIn(0, lastColumnElementIndex + 1)
        }
        val focusPoint = if (current.x != trimmedX) {
            FocusPoint(trimmedX, 0)
        } else {
            FocusPoint(trimmedX, trimmedY)
        }

        focusState.value = focusPoint
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

private data class FocusPoint(
    val x: Int = 0,
    val y: Int = 0,
)

private const val STEER_OFFSET_STEP = 0.005f

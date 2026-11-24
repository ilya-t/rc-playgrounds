package com.rc.playgrounds.presentation.announce

import com.rc.playgrounds.config.ActiveConfigProvider
import com.rc.playgrounds.control.gamepad.GamepadButtonPress
import com.rc.playgrounds.control.gamepad.GamepadEventStream
import com.rc.playgrounds.presentation.quickconfig.Element
import com.rc.playgrounds.presentation.quickconfig.ElementGroup
import com.rc.playgrounds.presentation.quickconfig.QuickConfigModel
import com.rc.playgrounds.presentation.quickconfig.QuickConfigViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private const val ANNOUNCE_DURATION = 1000L

class AnnounceModel(
    private val activeConfigProvider: ActiveConfigProvider,
    private val quickConfigModel: QuickConfigModel,
    gamepadEventStream: GamepadEventStream,
    private val scope: CoroutineScope,
) {
    private val profileSwitchEvent = MutableStateFlow<ProfileSwitch?>(null)

    private val _viewModel = MutableStateFlow<AnnounceViewModel>(AnnounceViewModel.Hidden)
    private val profilesInnerViewModel = MutableStateFlow<ProfilesViewModel?>(null)
    val viewModel: Flow<AnnounceViewModel> = _viewModel

    init {
        scope.launch {
            quickConfigModel._visibleViewModel
                .filterNotNull()
                .map { toProfilesViewModel(it) }
                .filterNotNull()
                .collect {
                    profilesInnerViewModel.value = it
                }
        }
        scope.launch {
            profilesInnerViewModel.filterNotNull()
                .collect { profiles: ProfilesViewModel ->
                if (profileSwitchEvent.value?.name == profiles.focused) {
                    return@collect
                }

                if (profileSwitchEvent.value?.lastActiveElement == profiles.lastActiveElement) {
                    // Looks like you're changing offsets which do not have toggle-like buttons.
                    return@collect
                }

                profileSwitchEvent.value = ProfileSwitch(
                    title = profiles.title,
                    name = profiles.focused,
                    lastActiveElement = profiles.lastActiveElement,
                    time = System.currentTimeMillis(),
                )
            }
        }

        scope.launch {
            gamepadEventStream.buttonEvents.collect {
                when (it) {
                    GamepadButtonPress.LeftBumper -> profilesInnerViewModel.value?.onLeftBumper()
                    GamepadButtonPress.RightBumper -> profilesInnerViewModel.value?.onRightBumper()
                    else -> Unit
                }
            }
        }

        scope.launch {
            profileSwitchEvent.filterNotNull()
                .drop(1)
                .collect {
                val now = System.currentTimeMillis()
                val timePassed = now - it.time
                if (timePassed > 1000L) {
                    return@collect
                }

                tryMakeAnnounce(it.title + ":\n"+ it.name.uppercase())
            }
        }

        scope.launch {
            activeConfigProvider.configFlow
                .map { it.controlOffsets.steer }
                .drop(1)
                .distinctUntilChanged()
                .collect { tryMakeAnnounce("steering offset:\n%.3f".format(it)) }
        }
    }

    private fun tryMakeAnnounce(announce: String) {
        when (quickConfigModel.viewModel.value) {
            QuickConfigViewModel.Hidden -> Unit
            // Quick config visible so do nothing
            is QuickConfigViewModel.Visible -> return
        }

        val visible = AnnounceViewModel.Visible(
            title = announce.uppercase()
        )
        _viewModel.value = visible
        scope.launch {
            delay(ANNOUNCE_DURATION)
            _viewModel.compareAndSet(visible, AnnounceViewModel.Hidden)
        }

    }

    private fun toProfilesViewModel(quickViewModel: QuickConfigViewModel.Visible): ProfilesViewModel? {
        val focusedGroup: ElementGroup = quickViewModel.elementGroups.find {
            group -> group.elements.any { it.focused } || group.focused
        } ?: return null
        val focused: Element = focusedGroup.elements.find { it.focused }
            ?: focusedGroup.elements.firstOrNull() ?: return null
        val focusedIndex = focusedGroup.elements.indexOf(focused)

        return ProfilesViewModel(
            title = focusedGroup.title,
            focused = focused.title,
            lastActiveElement = focusedGroup.elements.indexOfLast { it.active },
            onLeftBumper = {
                val nextElement = (focusedGroup.elements.getOrNull(focusedIndex - 1)
                    ?: focusedGroup.elements.firstOrNull())
                nextElement?.onClick()
                // Emulate move of focus to prev. tile.
                quickViewModel.onButtonUpPressed()
            },
            onRightBumper = {
                // Emulate click on prev. tile
                val nextElement = (focusedGroup.elements.getOrNull(focusedIndex + 1)
                    ?: focusedGroup.elements.lastOrNull())
                nextElement?.onClick()
                // Emulate move of focus to prev. tile.
                quickViewModel.onButtonDownPressed()
            },
        )
    }
}

private data class ProfileSwitch(
    val title: String,
    val name: String,
    val lastActiveElement: Int,
    val time: Long,
)

private class ProfilesViewModel(
    val title: String,
    val focused: String,
    val lastActiveElement: Int,
    val onLeftBumper: () -> Unit,
    val onRightBumper: () -> Unit,
)

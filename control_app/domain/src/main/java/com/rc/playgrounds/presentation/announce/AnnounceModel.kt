package com.rc.playgrounds.presentation.announce

import com.rc.playgrounds.config.ActiveConfigProvider
import com.rc.playgrounds.control.ControlTuningProvider
import com.rc.playgrounds.control.gamepad.GamepadButtonPress
import com.rc.playgrounds.control.gamepad.GamepadEventStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

private const val ANNOUNCE_DURATION = 500L

class AnnounceModel(
    private val controlTuningProvider: ControlTuningProvider,
    private val activeConfigProvider: ActiveConfigProvider,
    gamepadEventStream: GamepadEventStream,
    private val scope: CoroutineScope,
) {
    private val profileSwitchEvent = MutableStateFlow<ProfileSwitch?>(null)

    private val _viewModel = MutableStateFlow<AnnounceViewModel>(AnnounceViewModel.Hidden)
    val viewModel: Flow<AnnounceViewModel> = _viewModel

    init {
        scope.launch {
            //TODO: tuning profiles are dead now.
//            controlTuningProvider.activeControlProfile.filterNotNull().collect {
//                if (profileSwitchEvent.value?.name == it) {
//                    return@collect
//                }
//
//                profileSwitchEvent.value = ProfileSwitch(
//                    it, System.currentTimeMillis()
//                )
//            }
        }

        scope.launch {
            gamepadEventStream.buttonEvents.collect {
                when (it) {
                    GamepadButtonPress.LeftBumper -> moveProfileIndexBy(-1)
                    GamepadButtonPress.RightBumper -> moveProfileIndexBy(+1)
                    else -> Unit
                }
            }
        }

        scope.launch {
            profileSwitchEvent.filterNotNull().collect {
                val now = System.currentTimeMillis()
                val timePassed = now - it.time
                if (timePassed > 1000L) {
                    return@collect
                }

                val visible = AnnounceViewModel.Visible(it.name.uppercase())
                _viewModel.value = visible
                scope.launch {
                    delay(ANNOUNCE_DURATION)
                    _viewModel.compareAndSet(visible, AnnounceViewModel.Hidden)
                }
            }
        }
    }

    private suspend fun moveProfileIndexBy(amount: Int) {
//        val currentProfile = controlTuningProvider.activeControlProfile.first()
//        val allProfiles = activeConfigProvider.configFlow.first().controlTuning
//        val currentIndex = allProfiles.indexOfFirst { it.name == currentProfile }
//        val newProfile = allProfiles.getOrNull(currentIndex + amount)?.name ?: return
//        controlTuningProvider.changeProfile(newProfile)
    }
}

private data class ProfileSwitch(
    val name: String,
    val time: Long,
)
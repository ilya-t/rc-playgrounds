package com.rc.playgrounds.control.gamepad

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

data class SessionGamepadEvent(
    val event: GamepadEvent,
    /**
     * Technically this is the moment of any trigger touched.
     */
    val sessionStart: GamepadEvent?,
)

class GamePadEventSessionProvider(
    scope: CoroutineScope,
    gamepadEventStream: GamepadEventStream,
) {
    private val sessionFlow = MutableStateFlow<SessionGamepadEvent?>(null)
    val events: Flow<SessionGamepadEvent> = sessionFlow.filterNotNull()

    init {
        scope.launch {
            gamepadEventStream.events.collect { event ->

                val isSessionRunning = event.leftTrigger.absoluteValue != 0f ||
                        event.rightTrigger.absoluteValue != 0f

                if (!isSessionRunning) {
                    sessionFlow.value = SessionGamepadEvent(
                        event = event,
                        sessionStart = null,
                    )
                    return@collect
                }

                val lastSessionStart = sessionFlow.value?.sessionStart

                if (lastSessionStart != null) {
                    sessionFlow.value = SessionGamepadEvent(
                        event = event,
                        sessionStart = lastSessionStart,
                    )
                } else {
                    sessionFlow.value = SessionGamepadEvent(
                        event = event,
                        sessionStart = event,
                    )
                }
            }
        }
    }
}

package com.rc.playgrounds.control.gamepad

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GamepadEventStream(
    private val scope: CoroutineScope,
) {
    private val _events = MutableStateFlow(
        GamepadEvent.INITIAL
    )

    val events: StateFlow<GamepadEvent> = _events
    private val _buttonEvents = MutableSharedFlow<GamepadButtonPress>()

    /**
     * Consider using `GamepadEventsByConsumer` to avoid event conflicts between multiple consumers.
     */
    internal val buttonEvents: Flow<GamepadButtonPress> = _buttonEvents

    fun emit(event: GamepadEvent) {
        _events.value = event
    }

    fun emit(event: GamepadButtonPress) {
        scope.launch {
            _buttonEvents.emit(event)
        }
    }
}
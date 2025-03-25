package com.rc.playgrounds.remote.stream

import com.rc.playgrounds.control.gamepad.GamepadButtonPress
import com.rc.playgrounds.control.gamepad.GamepadEventStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class StreamQualityProvider(
    private val eventStream: GamepadEventStream,
    private val scope: CoroutineScope,
) {
    private val _quality = MutableStateFlow(StreamParameters.LOW)
    private var profileIndex = 0

    val currentQuality: Flow<StreamParameters> = _quality

    init {
        scope.launch {
            eventStream.buttonEvents.collect {
                when (it) {
                    GamepadButtonPress.DpadDown -> {
                        profileIndex--
                        invalidate()
                    }
                    GamepadButtonPress.DpadUp -> {
                        profileIndex++
                        invalidate()
                    }
                }
            }
        }
    }

    private fun invalidate() {
        val current = StreamParameters.H264_OPTIONS.getOrNull(profileIndex)

        if (current != null) {
            _quality.value = current
            return
        }

        if (profileIndex < 0) {
            profileIndex = 0
            invalidate()
            return
        }

        profileIndex = StreamParameters.H264_OPTIONS.lastIndex
        invalidate()
    }
}
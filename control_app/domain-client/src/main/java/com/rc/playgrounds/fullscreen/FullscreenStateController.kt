package com.rc.playgrounds.fullscreen

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FullscreenStateController(
    scope: CoroutineScope,
) {
    private val _inFullScreen = MutableStateFlow<Boolean>(false)
    val inFullScreen: StateFlow<Boolean> = _inFullScreen

    private val _wantFullScreen = MutableStateFlow<Boolean>(true)
    val wantFullScreen: StateFlow<Boolean> = _wantFullScreen

    init {
        scope.launch {
            _inFullScreen.collect {
                if (!it) {
                    _wantFullScreen.value = false
                    delay(3000L)
                    _wantFullScreen.value = true
                }
            }
        }
    }

    fun changeState(fullscreen: Boolean) {
        _inFullScreen.value = fullscreen
    }
}

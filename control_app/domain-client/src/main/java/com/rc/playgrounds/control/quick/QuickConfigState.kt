package com.rc.playgrounds.control.quick

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class QuickConfigState {
    private val _opened = MutableStateFlow(false)
    val opened: StateFlow<Boolean> = _opened

    fun close() {
        _opened.value = false
    }

    fun toggle() {
        _opened.value = !_opened.value
    }
}
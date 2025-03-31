package com.rc.playgrounds.control.lock

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ControlLock() {
    private val _locked = MutableStateFlow(false)
    val locked: StateFlow<Boolean> = _locked

    fun unlock() {
        _locked.value = false
    }

    fun lock() {
        _locked.value = !_locked.value
    }
}
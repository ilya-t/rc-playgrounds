package com.rc.playgrounds.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ActiveScreenProvider {
    private val _screen = MutableStateFlow<Screen>(Screen.MAIN)
    val screen: StateFlow<Screen> = _screen

    fun switchTo(screen: Screen) {
        _screen.value = screen
    }
}
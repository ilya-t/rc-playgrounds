package com.rc.playgrounds.presentation.main

sealed interface MainViewModel {
    data object Hidden : MainViewModel
    data class Visible(
        val showControls: Boolean,
        val onSelectStartPressed: () -> Unit,
    ) : MainViewModel
}
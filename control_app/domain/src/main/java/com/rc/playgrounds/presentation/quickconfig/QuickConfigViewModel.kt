package com.rc.playgrounds.presentation.quickconfig

sealed interface QuickConfigViewModel {
    data object Hidden : QuickConfigViewModel
    data class Visible(
        val resolution: String,
        val steeringOffset: String,
        val onButtonUpPressed: () -> Unit,
        val onButtonDownPressed: () -> Unit,
        val onButtonLeftPressed: () -> Unit,
        val onButtonRightPressed: () -> Unit,
        val onBackButton: () -> Unit,
    ) : QuickConfigViewModel
}

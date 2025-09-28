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
    data class DashboardVisible(
        val elements: List<Element>,
        val onButtonUpPressed: () -> Unit,
        val onButtonDownPressed: () -> Unit,
        val onButtonLeftPressed: () -> Unit,
        val onButtonRightPressed: () -> Unit,
        val onApplyButton: () -> Unit,
        val onBackButton: () -> Unit,
    ) : QuickConfigViewModel
}

sealed interface Element {
    val title: String
    val active: Boolean

    class ToggleGroup(
        override val title: String,
        val toggles: List<Toggle>,
        override val active: Boolean,
    ) : Element
}

class Toggle(
    val active: Boolean,
    val title: String,
)

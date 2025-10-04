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
        val description: String,
        val elementGroups: List<ElementGroup>,
        val onButtonUpPressed: () -> Unit,
        val onButtonDownPressed: () -> Unit,
        val onButtonLeftPressed: () -> Unit,
        val onButtonRightPressed: () -> Unit,
        val onApplyButton: () -> Unit,
        val onBackButton: () -> Unit,
    ) : QuickConfigViewModel
}

data class ElementGroup(
    val title: String,
    val active: Boolean,
    val focused: Boolean,
    val elements: List<Element>,
)

data class Element(
    val active: Boolean,
    val focused: Boolean,
    val title: String,
    internal val onClick: () -> Unit = {},
)

package com.rc.playgrounds.config.view

sealed interface DraftState {
    val text: String

    class Unsaved(
        override val text: String,
    ) : DraftState

    class UnsavedWithErrors(
        override val text: String,
        val error: Throwable,
    ) : DraftState
}

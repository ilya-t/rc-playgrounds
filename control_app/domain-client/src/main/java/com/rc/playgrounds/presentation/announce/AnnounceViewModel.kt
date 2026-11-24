package com.rc.playgrounds.presentation.announce

sealed interface AnnounceViewModel {
    data class Visible(
        val title: String
    ) : AnnounceViewModel
    data object Hidden : AnnounceViewModel
}

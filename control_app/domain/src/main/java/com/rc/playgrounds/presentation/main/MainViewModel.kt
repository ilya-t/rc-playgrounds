package com.rc.playgrounds.presentation.main

sealed interface MainViewModel {
    data object Hidden : MainViewModel
    data object Visible : MainViewModel
}
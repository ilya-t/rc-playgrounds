package com.rc.playgrounds.presentation.lock

sealed interface LockViewModel {
    data object Visible : LockViewModel
    data object Hidden : LockViewModel
}

package com.rc.playgrounds.presentation.lock

import com.rc.playgrounds.control.lock.ControlLock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LockModel(
    private val controlLock: ControlLock,
) {
    fun onBackPress() {
        controlLock.unlock()
    }

    val viewModel: Flow<LockViewModel> = controlLock.locked.map { locked ->
        if (locked) {
            LockViewModel.Visible
        } else {
            LockViewModel.Hidden
        }
    }
}

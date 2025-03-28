package com.rc.playgrounds.control.lock

import android.view.View
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.rc.playgrounds.domain.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class LockView(
    private val activity: AppCompatActivity,
    private val lockModel: LockModel,
    private val scope: CoroutineScope,
) {
    private val lockContainer = activity.findViewById<View>(R.id.controls_lock_container)

    init {
        scope.launch {
            lockModel.viewModel.collect {
                lockContainer.isVisible = it.visible
            }
        }

        activity.onBackPressedDispatcher.addCallback {
            lockModel.onBackPress()
        }
    }
}
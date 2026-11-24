package com.rc.playgrounds.presentation.overlay

import android.app.Activity
import android.view.View
import androidx.core.view.isVisible
import com.rc.playgrounds.domain.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class OverlayView(
    overlayModel: OverlayModel,
    scope: CoroutineScope,
    activity: Activity,
) {
    private val lockRoot = activity.findViewById<View>(R.id.controls_lock_container)
    private val quickConfig = activity.findViewById<View>(R.id.quick_config_container)
    private val announceRoot = activity.findViewById<View>(R.id.announce_container)

    init {
        scope.launch {
            overlayModel.state.collect { viewModel: OverlayViewModel? ->
                hideAll()
                when (viewModel) {
                    OverlayViewModel.QUICK_CONFIG -> {
                        quickConfig.isVisible = true
                    }
                    OverlayViewModel.LOCK_SCREEN -> {
                        lockRoot.isVisible = true
                    }
                    OverlayViewModel.ANNOUNCE -> {
                        announceRoot.isVisible = true
                    }
                    null -> Unit
                }

            }
        }
    }

    private fun hideAll() {
        lockRoot.isVisible = false
        quickConfig.isVisible = false
        announceRoot.isVisible = false

    }
}
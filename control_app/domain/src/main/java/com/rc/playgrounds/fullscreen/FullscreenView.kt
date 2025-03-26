package com.rc.playgrounds.fullscreen

import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.core.view.ViewCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FullscreenView(
    private val view: View,
    private val insetsController: WindowInsetsController,
    private val scope: CoroutineScope,
    private val fullscreenStateController: FullscreenStateController,
) {
    init {
        scope.launch {
            fullscreenStateController.wantFullScreen.collect { wantFullscreen ->
                if (wantFullscreen) {
                    withContext(Dispatchers.Main) {
                        hideBars()
                    }
                }
            }
        }
        ViewCompat.setOnApplyWindowInsetsListener(
            view
        ) { v, insets ->
            invalidate()
            insets
        }
    }

    private fun invalidate() {
        val insets: WindowInsets = view.rootWindowInsets ?: return
        val areBarsVisible = insets.isVisible(WindowInsets.Type.statusBars()) ||
                insets.isVisible(WindowInsets.Type.navigationBars())
        fullscreenStateController.changeState(fullscreen = !areBarsVisible)
    }

    private fun hideBars() {
        insetsController.hide(WindowInsets.Type.statusBars())
        insetsController.hide(WindowInsets.Type.navigationBars())
    }
}

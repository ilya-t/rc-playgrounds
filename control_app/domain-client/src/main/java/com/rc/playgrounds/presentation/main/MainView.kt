package com.rc.playgrounds.presentation.main

import android.animation.ValueAnimator
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.rc.playgrounds.domain.R
import com.rc.playgrounds.navigation.ActiveScreenProvider
import com.rc.playgrounds.navigation.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class MainView(
    activity: AppCompatActivity,
    private val mainModel: MainModel,
    activeScreenProvider: ActiveScreenProvider,
    scope: CoroutineScope,
) {
    private val configureButton = activity.findViewById<Button>(R.id.configure_button)
    private val mainControls = activity.findViewById<View>(R.id.main_controls_container)
    private val surface = activity.findViewById<View>(R.id.surface_container)
    private val root = activity.findViewById<View>(R.id.layer_main)
    private val showControlsAnimator = ValueAnimator.ofFloat(0.2f, 1f)

    init {
        activeScreenProvider.switchTo(Screen.MAIN)
        configureButton.setOnClickListener {
            activeScreenProvider.switchTo(Screen.CONFIG)
            mainModel.onScreenClick()
        }

        root.setOnClickListener {
            mainModel.onScreenClick()
        }

        surface.setOnClickListener {
            mainModel.onScreenClick()
        }

        showControlsAnimator.addUpdateListener {
            mainControls.alpha = it.animatedValue as Float
        }

        scope.launch {
            mainModel.viewModel.collect { vm ->
                when (vm) {
                    MainViewModel.Hidden -> Unit

                    is MainViewModel.Visible -> {
                        moveAnimationTo(show = vm.showControls)
                    }
                }
            }
        }
    }

    private fun moveAnimationTo(show: Boolean) {
        val dst: Float = if (show) 1f else 0f
        if (dst == showControlsAnimator.animatedFraction) {
            mainControls.alpha = showControlsAnimator.animatedValue as Float
            return
        }

        if (showControlsAnimator.animatedFraction < dst) {
            showControlsAnimator.start()
            return
        }

        showControlsAnimator.reverse()
    }
}
package com.rc.playgrounds.navigation

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.rc.playgrounds.domain.R
import kotlinx.coroutines.launch

class NaiveNavigator(a: AppCompatActivity,
                     private val activeScreenProvider: ActiveScreenProvider,) {
    private val mainRoot = a.findViewById<View>(R.id.layer_main)
    private val configRoot = a.findViewById<View>(R.id.layer_config)

    init {
        activeScreenProvider.switchTo(Screen.MAIN)
        a.lifecycleScope.launch {
            activeScreenProvider.screen.collect { screen ->
                hideAllLayers()

                when (screen) {
                    Screen.MAIN -> {
                        mainRoot.isVisible = true
                    }
                    Screen.CONFIG -> {
                        configRoot.isVisible = true
                    }
                }
            }
        }
    }

    private fun hideAllLayers() {
        mainRoot.isVisible = false
        configRoot.isVisible = false
    }

    fun openMain() {
        activeScreenProvider.switchTo(Screen.MAIN)
    }
}
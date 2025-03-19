package com.rc.playgrounds.navigation

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.rc.playgrounds.domain.R

class NaiveNavigator(a: AppCompatActivity) {
    private val mainRoot = a.findViewById<View>(R.id.layer_main)
    private val configRoot = a.findViewById<View>(R.id.layer_config)
    private val tvOutput = a.findViewById<View>(R.id.tv_output)
    fun openMain() {
        mainRoot.isVisible = true
        configRoot.isVisible = false
        tvOutput.isVisible = true
    }

    fun openConfig() {
        mainRoot.isVisible = false
        configRoot.isVisible = true
        tvOutput.isVisible = false
    }
}
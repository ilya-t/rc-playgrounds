package com.rc.playgrounds.presentation.main

import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.rc.playgrounds.domain.R
import com.rc.playgrounds.navigation.ActiveScreenProvider
import com.rc.playgrounds.navigation.Screen
import kotlinx.coroutines.CoroutineScope

class MainView(
    activity: AppCompatActivity,
    private val mainModel: MainModel,
    activeScreenProvider: ActiveScreenProvider,
    scope: CoroutineScope,
) {
    private val configureButton = activity.findViewById<Button>(R.id.configure_button)

    init {
        activeScreenProvider.switchTo(Screen.MAIN)
        configureButton.setOnClickListener {
            activeScreenProvider.switchTo(Screen.CONFIG)
        }
    }
}
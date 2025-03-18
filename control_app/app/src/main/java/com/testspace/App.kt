package com.testspace

import android.app.Application
import com.rc.playgrounds.AppComponent
import com.rc.playgrounds.AppComponent.Companion.instance

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = AppComponent(this)
    }
}

package com.testspace

import android.app.Application
import rc.playgrounds.AppComponent
import rc.playgrounds.AppComponent.Companion.instance

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = AppComponent(this)
    }
}

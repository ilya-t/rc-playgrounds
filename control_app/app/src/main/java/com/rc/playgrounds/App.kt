package com.rc.playgrounds

import android.app.Application
import com.rc.playgrounds.server.ServerAppComponent

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        AppComponent.app = this
        ServerAppComponent.app = this
    }
}

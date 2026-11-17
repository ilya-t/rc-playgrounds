package com.rc.playgrounds.server

import android.app.Application
import com.rc.playgrounds.AppComponent
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob

class ServerAppComponent(app: Application) {
    val scope: CoroutineScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default + CoroutineName("ServerAppScope"),
    )
    val controlReceiver = ServerControlReceiver(scope)

    companion object {
        lateinit var app: Application
        val instance: ServerAppComponent by lazy {
            ServerAppComponent(app)
        }
    }
}

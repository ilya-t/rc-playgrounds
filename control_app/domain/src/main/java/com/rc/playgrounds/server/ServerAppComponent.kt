package com.rc.playgrounds.server

import android.app.Application
import com.rc.playgrounds.server.controllink.AndroidMicroviewLink
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class ServerAppComponent(app: Application) {
    val scope: CoroutineScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default + CoroutineName("ServerAppScope"),
    )
    val controlReceiver = ServerControlReceiver(scope)
    val usbPermissionHandler = UsbPermissionHandler()
    val microviewLink = AndroidMicroviewLink(
        app = app,
        controls = controlReceiver.controls,
        scope = scope,
        usbPermissionHandler = usbPermissionHandler,
    )

    companion object {
        lateinit var app: Application
        val instance: ServerAppComponent by lazy {
            ServerAppComponent(app)
        }
    }
}

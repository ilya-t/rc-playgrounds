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
    val errorLogger = ErrorLogger(scope)
    val controlReceiver = ServerControlReceiver(scope, errorLogger)
    val usbPermissionHandler = UsbPermissionHandler()
    val microviewLink = AndroidMicroviewLink(
        app = app,
        controls = controlReceiver.controls,
        scope = scope,
        usbPermissionHandler = usbPermissionHandler,
        errors = errorLogger,
    )
    val batteryMonitor = ServerBatteryMonitor(app)
    val cellularMonitor = ServerCellularMonitor(app)
    val telemetrySender = ServerTelemetrySender(
        target = controlReceiver.telemetryTarget,
        status = microviewLink.status,
        cellular = cellularMonitor.cellular,
        battery = batteryMonitor.battery,
        errorLogger = errorLogger,
        scope = scope,
    )

    companion object {
        lateinit var app: Application
        val instance: ServerAppComponent by lazy {
            ServerAppComponent(app)
        }
    }
}

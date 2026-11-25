package com.rc.playgrounds.server

import com.rc.playgrounds.server.controllink.DeviceState
import com.rc.playgrounds.server.controllink.MicroviewProtocol
import com.rc.playgrounds.telemetry.Telemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class ServerTelemetrySender(
    private val target: StateFlow<TelemetryTarget?>,
    private val status: StateFlow<MicroviewProtocol.Status?>,
    private val cellular: StateFlow<Telemetry.Cellular?>,
    private val battery: StateFlow<Telemetry.Battery?>,
    private val errorLogger: ErrorLogger,
    scope: CoroutineScope,
) {
    init {
        val ready = MutableStateFlow<Pair<TelemetryTarget, Telemetry>?>(null)
        scope.launch {
            combine(target, status, cellular, battery) { destination, current, cell, batt ->
                destination?.let { it to serverTelemetry(current, cell, batt) }
            }.collect { ready.value = it }
        }
        scope.launch(Dispatchers.IO) {
            val socket = DatagramSocket()
            try {
                while (isActive) {
                    ready.value?.let { (destination, payload) ->
                        send(socket, destination, payload)
                    }
                    delay(INTERVAL_MS)
                }
            } finally {
                socket.close()
            }
        }
    }

    private fun send(
        socket: DatagramSocket,
        destination: TelemetryTarget,
        payload: Telemetry,
    ) {
        try {
            val bytes = payload.toJson().toByteArray(Charsets.UTF_8)
            val address = InetAddress.getByName(destination.host)
            socket.send(DatagramPacket(bytes, bytes.size, address, destination.port))
        } catch (e: Exception) {
            if (!socket.isClosed) {
                errorLogger.report(SERVICE, e)
            }
        }
    }

    private companion object {
        const val SERVICE = "telemetry"
        const val INTERVAL_MS = 500L
    }
}

internal fun serverTelemetry(
    current: MicroviewProtocol.Status?,
    cellular: Telemetry.Cellular?,
    battery: Telemetry.Battery?,
): Telemetry {
    val control = when (current?.deviceState) {
        DeviceState.RECEIVING -> Telemetry.Control(status = "receiving", error = false)
        DeviceState.FAILSAFE -> Telemetry.Control(status = "failsafe", error = true)
        else -> Telemetry.Control(status = "no link", error = true)
    }
    return Telemetry(control = control, cellular = cellular, battery = battery)
}

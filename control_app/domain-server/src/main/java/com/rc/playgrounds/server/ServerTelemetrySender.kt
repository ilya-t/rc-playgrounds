package com.rc.playgrounds.server

import com.rc.playgrounds.server.controllink.DeviceState
import com.rc.playgrounds.server.controllink.MicroviewProtocol
import com.rc.playgrounds.telemetry.Telemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
        scope.launch(Dispatchers.IO) {
            val socket = DatagramSocket()
            try {
                combine(target, status, cellular, battery) { destination, current, cell, batt ->
                    val payload = serverTelemetry(current, cell, batt) ?: return@combine null
                    destination?.let { it to payload }
                }.collect { ready ->
                    if (ready == null) {
                        return@collect
                    }
                    send(socket, ready.first, ready.second)
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
    }
}

internal fun serverTelemetry(
    current: MicroviewProtocol.Status?,
    cellular: Telemetry.Cellular?,
    battery: Telemetry.Battery?,
): Telemetry? {
    val control = when (current?.deviceState) {
        DeviceState.RECEIVING -> Telemetry.Control(status = "receiving", error = false)
        DeviceState.FAILSAFE -> Telemetry.Control(status = "failsafe", error = true)
        else -> return null
    }
    return Telemetry(control = control, cellular = cellular, battery = battery)
}

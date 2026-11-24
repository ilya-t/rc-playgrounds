package com.rc.playgrounds.server

import com.rc.playgrounds.server.controllink.DeviceState
import com.rc.playgrounds.server.controllink.MicroviewProtocol
import com.rc.playgrounds.telemetry.Telemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class ServerTelemetrySender(
    private val target: StateFlow<TelemetryTarget?>,
    private val status: StateFlow<MicroviewProtocol.Status?>,
    private val errorLogger: ErrorLogger,
    scope: CoroutineScope,
) {
    init {
        scope.launch(Dispatchers.IO) {
            val socket = DatagramSocket()
            try {
                coroutineScope {
                    launch {
                        status.collect { current ->
                            val destination = target.value ?: return@collect
                            val payload = telemetry(current) ?: return@collect
                            send(socket, destination, payload)
                        }
                    }
                    launch {
                        errorLogger.errors.collect { reported ->
                            if (reported.service != ServerControlReceiver.SERVICE) {
                                return@collect
                            }
                            val destination = target.value ?: return@collect
                            val message = reported.error.message?.takeIf { it.isNotEmpty() }
                                ?: reported.error.javaClass.simpleName
                            send(
                                socket,
                                destination,
                                Telemetry(Telemetry.Control(status = message, error = true)),
                            )
                        }
                    }
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

        fun telemetry(current: MicroviewProtocol.Status?): Telemetry? {
            val control = when (current?.deviceState) {
                DeviceState.RECEIVING -> Telemetry.Control(status = "receiving", error = false)
                DeviceState.FAILSAFE -> Telemetry.Control(status = "failsafe", error = true)
                else -> return null
            }
            return Telemetry(control)
        }
    }
}

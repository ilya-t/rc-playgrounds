package com.rc.playgrounds.server

import android.util.Log
import com.rc.playgrounds.common.ControlSignal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.SocketException
import java.net.SocketTimeoutException
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction

data class ServerControlState(
    val yaw: Float = 0f,
    val pitch: Float = 0f,
    val steer: Float = 0f,
    val long: Float = 0f,
    val streamCmd: String = "",
    val streamCmdHash: String = "",
    val live: Boolean = false,
)

data class VideoTarget(
    val host: String,
    val port: Int,
)

class ServerControlReceiver(
    scope: CoroutineScope,
    private val errors: ErrorLogger,
) {
    private val state = MutableStateFlow(ServerControlState())
    private val target = MutableStateFlow<VideoTarget?>(null)
    private val telemetry = MutableStateFlow<TelemetryTarget?>(null)
    val controls: StateFlow<ServerControlState> = state
    val videoTarget: StateFlow<VideoTarget?> = target
    val telemetryTarget: StateFlow<TelemetryTarget?> = telemetry

    init {
        scope.launch(Dispatchers.IO) {
            val socket = try {
                DatagramSocket(null).apply {
                    reuseAddress = true
                    bind(InetSocketAddress(PORT))
                    soTimeout = TIMEOUT_MS
                }
            } catch (e: Exception) {
                Log.e(TAG, "bind $PORT", e)
                return@launch
            }
            Log.i(TAG, "udp 0.0.0.0:$PORT")
            val buffer = ByteArray(BUFFER_SIZE)
            val packet = DatagramPacket(buffer, buffer.size)
            try {
                while (isActive) {
                    try {
                        packet.length = buffer.size
                        socket.receive(packet)
                        val text = decode(packet) ?: continue
                        val json = try {
                            JSONObject(text)
                        } catch (e: JSONException) {
                            errors.report(SERVICE, e)
                            continue
                        }
                        val next = parse(json) ?: continue
                        state.value = next
                        val destination = parseVideoTarget(next.streamCmd)
                        if (destination != null) {
                            target.value = destination
                        }
                        val telemetryDestination = parseTelemetryTarget(json)
                        if (telemetryDestination != null) {
                            telemetry.value = telemetryDestination
                        }
                    } catch (_: SocketTimeoutException) {
                        if (state.value.live) {
                            state.value = ServerControlState()
                        }
                    } catch (e: SocketException) {
                        if (isActive && !socket.isClosed) {
                            Log.e(TAG, "receive", e)
                        }
                        return@launch
                    } catch (e: Exception) {
                        Log.e(TAG, "receive", e)
                    }
                }
            } finally {
                socket.close()
            }
        }
    }

    companion object {
        const val SERVICE = "control"
        private const val TAG = "ServerControl"
        private const val PORT = 12346
        private const val BUFFER_SIZE = 1024
        private const val TIMEOUT_MS = 250
    }
}

private fun parse(json: JSONObject): ServerControlState? {
    val signal = ControlSignal.fromJson(json) ?: return null
    return ServerControlState(
        yaw = signal.yaw,
        pitch = signal.pitch,
        steer = signal.steer,
        long = signal.long,
        streamCmd = signal.streamCmd,
        streamCmdHash = signal.streamCmdHash,
        live = true,
    )
}

private fun parseVideoTarget(cmd: String): VideoTarget? {
    val url = UDP_URL.find(cmd)
    if (url != null) {
        return target(url.groupValues[1], url.groupValues[2])
    }
    val host = SINK_HOST.find(cmd)?.groupValues?.get(1) ?: return null
    val port = SINK_PORT.find(cmd)?.groupValues?.get(1) ?: return null
    return target(host, port)
}

private fun target(host: String, portText: String): VideoTarget? {
    val port = portText.toIntOrNull() ?: return null
    if (host.isEmpty() || port !in 1..65535) {
        return null
    }
    return VideoTarget(host, port)
}

private val UDP_URL = Regex("""udp://([^:/\s'"]+):(\d+)""")
private val SINK_HOST = Regex("""host=([^\s'"]+)""")
private val SINK_PORT = Regex("""port=(\d+)""")

private fun decode(packet: DatagramPacket): String? {
    return try {
        val decoder = Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        decoder.decode(ByteBuffer.wrap(packet.data, packet.offset, packet.length)).toString()
    } catch (_: CharacterCodingException) {
        null
    }
}

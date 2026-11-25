package com.rc.playgrounds.telemetry

import android.util.Log
import com.rc.playgrounds.config.ActiveConfigProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.StandardProtocolFamily
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction

class TelemetryReceiver(
    scope: CoroutineScope,
    config: ActiveConfigProvider,
    private val provider: TelemetryProvider,
) {
    init {
        scope.launch(Dispatchers.IO) {
            var listenJob: Job? = null
            try {
                config.configFlow
                    .map { it.mobileClient?.telemetryPort(it.env) }
                    .distinctUntilChanged()
                    .collect { port ->
                        listenJob?.cancelAndJoin()
                        listenJob = null
                        if (port == null || port !in 1..65535) {
                            return@collect
                        }
                        listenJob = this@launch.launch { listen(port) }
                    }
            } finally {
                listenJob?.cancelAndJoin()
            }
        }
    }

    private suspend fun listen(port: Int) {
        val socket = try {
            ipv4Socket(port)
        } catch (e: Exception) {
            Log.e(TAG, "bind $port", e)
            return
        }
        Log.i(TAG, "udp 0.0.0.0:$port")
        val buffer = ByteArray(BUFFER_SIZE)
        val packet = DatagramPacket(buffer, buffer.size)
        try {
            coroutineScope {
                launch {
                    try {
                        awaitCancellation()
                    } finally {
                        socket.close()
                    }
                }
                while (isActive) {
                    try {
                        packet.length = buffer.size
                        socket.receive(packet)
                        val text = decode(packet) ?: continue
                        val next = Telemetry.fromJson(text) ?: continue
                        provider.update(next)
                    } catch (_: SocketTimeoutException) {
                    } catch (e: SocketException) {
                        if (isActive && !socket.isClosed) {
                            Log.e(TAG, "receive", e)
                        }
                        return@coroutineScope
                    } catch (e: Exception) {
                        if (e is CancellationException) {
                            throw e
                        }
                        Log.e(TAG, "receive", e)
                    }
                }
            }
        } finally {
            socket.close()
        }
    }

    private fun ipv4Socket(port: Int): DatagramSocket {
        val channel = DatagramChannel.open(StandardProtocolFamily.INET)
        channel.configureBlocking(true)
        val socket = channel.socket()
        socket.reuseAddress = true
        channel.bind(InetSocketAddress(InetAddress.getByName(IPV4_WILDCARD), port))
        socket.soTimeout = TIMEOUT_MS
        return socket
    }

    private companion object {
        const val TAG = "Telemetry"
        const val IPV4_WILDCARD = "0.0.0.0"
        const val BUFFER_SIZE = 1024
        const val TIMEOUT_MS = 250
    }
}

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

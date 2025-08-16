package com.rc.playgrounds.remote

import com.rc.playgrounds.config.ActiveConfigProvider
import com.rc.playgrounds.config.Config
import com.rc.playgrounds.config.model.NetworkTarget
import com.rc.playgrounds.control.RcEvent
import com.rc.playgrounds.control.RcEventStream
import com.rc.playgrounds.remote.stream.RemoteStreamConfig
import com.rc.playgrounds.remote.stream.RemoteStreamConfigController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

@OptIn(ExperimentalCoroutinesApi::class)
class OutputEventStream(
    private val rcEventStream: RcEventStream,
    private val scope: CoroutineScope,
    private val activeConfigProvider: ActiveConfigProvider,
    private val streamCmdHash: StreamCmdHash,
    private val remoteStreamConfigController: RemoteStreamConfigController,
) {

    private val controlServer = MutableStateFlow<NetworkTarget?>(null)
    private var emitter: EventEmitter? = null

    init {
        scope.launch {
            activeConfigProvider.configFlow.collect {
                if (controlServer.value != it.controlServer) {
                    controlServer.value = it.controlServer
                }
            }
        }

        scope.launch(Dispatchers.IO.limitedParallelism(1)) {
            controlServer.collect {
                restart(it)
            }
        }
    }

    private fun restart(c: NetworkTarget?) {
        emitter?.stop()
        emitter = null
        c?.let {
            emitter = EventEmitter(
                c,
                scope,
                rcEventStream,
                activeConfigProvider.configFlow,
                streamCmdHash.hash,
                remoteStreamConfigController,
            )
        }
    }
}

private class EventEmitter(
    val config: NetworkTarget,
    private val scope: CoroutineScope,
    private val rcEventStream: RcEventStream,
    private val configFlow: Flow<Config>,
    private val streamCmdHash: Flow<String>,
    private val remoteStreamConfigController: RemoteStreamConfigController,
) {
    private val messages: Flow<JSONObject> = combine(
        remoteStreamConfigController.state,
        configFlow,
        rcEventStream.events,
        streamCmdHash,
    ) { streamConfig: RemoteStreamConfig?, config: Config, event: RcEvent, streamHash: String ->
        asJson(
            event,
            streamCmd = streamConfig?.remoteCmd ?: config.stream.remoteCmd,
            streamCmdHash = streamHash,
        )
    }

    private var messageStream: Job? = null
    private val job = scope.launch {
        messages.collect { m ->
            messageStream?.cancel()
            messageStream = scope.launch {
                while (isActive) {
                    m.put("time", System.currentTimeMillis().toString())
                    send(m.toString())
                    //TODO: to config
                    delay(50L) // 20hz
                }
            }
        }
    }

    private fun send(message: String) {
        try {
            val socket = DatagramSocket()
            val address = InetAddress.getByName(config.address)
            val buffer = message.toByteArray()
            val packet = DatagramPacket(buffer, buffer.size, address, config.port)
            socket.send(packet)
            socket.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun asJson(
        event: RcEvent,
        streamCmd: String,
        streamCmdHash: String,
    ): JSONObject {
        val json = JSONObject()
            .put("pitch", event.pitch) // -1..1
            .put("yaw", event.yaw)
            .put("steer", event.steer) // -1..1
            .put("long", event.long) // -1..1
            .put("stream_cmd", streamCmd)
            .put("stream_cmd_hash", streamCmdHash)
        return json
    }

    fun stop() {
        messageStream?.cancel()
        messageStream = null
        job.cancel()
    }
}


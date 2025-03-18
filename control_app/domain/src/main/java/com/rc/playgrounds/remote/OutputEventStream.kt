package com.rc.playgrounds.remote

import com.rc.playgrounds.control.SteeringEvent
import com.rc.playgrounds.control.SteeringEventStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import rc.playgrounds.config.ConfigModel
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

@OptIn(ExperimentalCoroutinesApi::class)
class OutputEventStream(
    private val steeringEventStream: SteeringEventStream,
    private val scope: CoroutineScope,
    private val configModel: ConfigModel,
    private val streamCmdHash: StreamCmdHash,
) {

    private val controlServer = MutableStateFlow<com.rc.playgrounds.config.model.ControlServer?>(null)
    private var emitter: EventEmitter? = null

    init {
        scope.launch {
            configModel.configFlow.collect {
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

    private fun restart(c: com.rc.playgrounds.config.model.ControlServer?) {
        emitter?.stop()
        emitter = null
        c?.let {
            emitter = EventEmitter(
                c,
                scope,
                steeringEventStream,
                configModel.configFlow,
                streamCmdHash.hash,
            )
        }
    }
}

private class EventEmitter(
    val config: com.rc.playgrounds.config.model.ControlServer,
    private val scope: CoroutineScope,
    private val steeringEventStream: SteeringEventStream,
    private val configFlow: StateFlow<com.rc.playgrounds.config.Config>,
    private val streamCmdHash: Flow<String>,
) {
    private val messages: Flow<JSONObject> = combine(
        configFlow,
        steeringEventStream.events,
        streamCmdHash,
    ) { config: com.rc.playgrounds.config.Config, event: SteeringEvent, streamHash: String ->
        asJson(
            event,
            streamCmd = config.remoteStreamCmd,
            streamCmdHash = streamHash,
        )
    }

    private val job = scope.launch {
        var messageStream: Job? = null
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
        event: SteeringEvent,
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
        job.cancel()
    }
}


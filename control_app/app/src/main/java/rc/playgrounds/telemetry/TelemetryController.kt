package rc.playgrounds.telemetry

import com.testspace.core.Static
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import rc.playgrounds.config.Config
import rc.playgrounds.config.ConfigModel
import rc.playgrounds.config.model.Telemetry
import rc.playgrounds.telemetry.gamepad.GamepadEvent
import rc.playgrounds.telemetry.gamepad.GamepadEventStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

@OptIn(ExperimentalCoroutinesApi::class)
class TelemetryController(
    private val gamepadEventStream: GamepadEventStream,
    private val scope: CoroutineScope,
    private val configModel: ConfigModel,
    ) {

    private val telemetry = MutableStateFlow<Telemetry?>(null)
    private var emitter: TelemetryEmitter? = null

    init {
        scope.launch {
            configModel.configFlow.collect {
                if (telemetry.value != it.telemetry) {
                    telemetry.value = it.telemetry
                }
            }
        }

        scope.launch(Dispatchers.IO.limitedParallelism(1)) {
            telemetry.collect {
                restart(it)
            }
        }
    }

    private fun restart(t: Telemetry?) {
        emitter?.stop()
        emitter = null
        t?.let {
            emitter = TelemetryEmitter(
                t,
                scope,
                gamepadEventStream,
                configModel.configFlow,
            )
        }
    }
}

private class TelemetryEmitter(
    val config: Telemetry,
    private val scope: CoroutineScope,
    private val gamepadEventStream: GamepadEventStream,
    private val configFlow: StateFlow<Config>,
) {
    private val job = scope.launch {
        gamepadEventStream.events.collect {
            val message = asTelemetryEvent(it)
            Static.output(JSONObject(message).toString(4))
            send(message)
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
    private fun asTelemetryEvent(event: GamepadEvent): String {
//            yaw = msg.get("yaw", 0)       # -1..1
//            pitch = msg.get("pitch", 0)
//            steer = msg.get("steer", 0)
//            long = msg.get("long", 0) + 0.18    #
        val offsets = configFlow.value.controlOffsets

        val json = JSONObject()
            .put("pitch", -event.rightStickY + offsets.pitch) // -1..1
            .put("yaw", -event.rightStickX + offsets.yaw)
            .put("steer", event.leftStickX + offsets.steer) // -1..1
            .put("long", event.rightTrigger + offsets.long) // -1..1

        if (event.leftTrigger > event.rightTrigger) {
            json.put("long", -event.leftTrigger) // -1..1
        }
        return json.toString()
    }

    fun stop() {
        job.cancel()
    }
}
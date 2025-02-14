package rc.playgrounds.telemetry

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import rc.playgrounds.config.ConfigModel
import rc.playgrounds.config.model.Telemetry
import rc.playgrounds.telemetry.gamepad.GamepadEvent
import rc.playgrounds.telemetry.gamepad.GamepadEventStream
import java.io.OutputStream
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket

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
            )
        }
    }
}

private class TelemetryEmitter(
    val config: Telemetry,
    private val scope: CoroutineScope,
    private val gamepadEventStream: GamepadEventStream,
) {
    private val job = scope.launch {
        gamepadEventStream.events.collect {
            send(asTelemetryEvent(it))
        }
    }

    private fun send(message: String) {
        try {
            val socket = Socket()
            socket.connect(InetSocketAddress(config.address, config.port), 5000) // Timeout in 5 sec
            val outputStream: OutputStream = socket.getOutputStream()
            val writer = PrintWriter(outputStream, true)
            writer.println(message) // Send the string
            writer.flush()
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
        val json = JSONObject()
            .put("pitch", event.rightStickY) // -1..1
            .put("yaw", event.rightStickX)
            .put("steer", event.leftStickX) // -1..1
            .put("long", event.rightTrigger) // -1..1
            if (event.leftTrigger > event.rightTrigger) {
                json.put("long", event.leftStickX) // -1..1
            }
        return json.toString()
    }

    fun stop() {
        job.cancel()
    }
}
package rc.playgrounds.telemetry

import android.view.animation.AccelerateInterpolator
import com.testspace.core.Static
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import rc.playgrounds.config.Config
import rc.playgrounds.config.ConfigModel
import rc.playgrounds.config.model.ControlOffsets
import rc.playgrounds.config.model.ControlTuning
import rc.playgrounds.config.model.Telemetry
import rc.playgrounds.telemetry.gamepad.GamepadEvent
import rc.playgrounds.telemetry.gamepad.GamepadEventStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.math.absoluteValue
import kotlin.math.sign

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
    private val messages: Flow<String> = combine(
        configFlow.map { it.controlOffsets },
        gamepadEventStream.events,
        configFlow.map {
            it.controlTuning.asInterpolation()
        }
    ) { offsets, event, interpolation -> asTelemetryEvent(event, offsets, interpolation) }
    private val job = scope.launch {
        var messageStream: Job? = null
        messages.collect { m ->
//            Log.i("_debug_", "===> NEW STATE: $m")
            Static.output(JSONObject(m).toString(4))
            messageStream?.cancel()
            messageStream = scope.launch {
                while (isActive) {
                    send(m)
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

    private fun asTelemetryEvent(
        event: GamepadEvent,
        offsets: ControlOffsets,
        interpolation: ControlInterpolation,
    ): String {
        val rawPitch = -event.rightStickY + offsets.pitch
        val rawYaw = -event.rightStickX + offsets.yaw
        val rawSteer = event.leftStickX + offsets.steer
        val longTrigger = if (event.leftTrigger > event.rightTrigger) {
            event.leftTrigger
        } else {
            event.rightTrigger
        }
        val rawLong = -longTrigger + offsets.long

        val json = JSONObject()
            .put("pitch", interpolation.fixPitch(rawPitch)) // -1..1
            .put("yaw", interpolation.fixYaw(rawYaw))
            .put("steer", interpolation.fixSteer(rawSteer)) // -1..1
            .put("long", interpolation.fixLong(rawLong)) // -1..1
        return json.toString()
    }

    fun stop() {
        job.cancel()
    }
}

private class ControlInterpolation(
    private val pitch: AccelerateInterpolator?,
    private val yaw: AccelerateInterpolator?,
    private val steer: AccelerateInterpolator?,
    private val long: AccelerateInterpolator?,
) {
    fun fixPitch(value: Float): Float {
        return fix(pitch, value)
    }

    fun fixYaw(value: Float): Float {
        return fix(yaw, value)
    }
    fun fixSteer(value: Float): Float {
        return fix(steer, value)
    }
    fun fixLong(value: Float): Float {
        return fix(long, value)
    }

    private fun fix(interpolator: AccelerateInterpolator?, value: Float): Float {
        if (interpolator == null) {
            return value
        }

        val sign = sign(value)
        val interpolated = interpolator.getInterpolation(value.absoluteValue)
        return interpolated * sign
    }
}

private fun ControlTuning.asInterpolation() = ControlInterpolation(
    pitch = create(pitchFactor),
    yaw = create(yawFactor),
    steer = create(steerFactor),
    long = create(longFactor),
)

private fun create(factor: Float?): AccelerateInterpolator? {
    if (factor == null) {
        return null
    }

    if (factor.isNaN()) {
        return null
    }

    return AccelerateInterpolator((factor))
}

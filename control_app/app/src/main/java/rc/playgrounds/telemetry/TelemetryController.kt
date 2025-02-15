package rc.playgrounds.telemetry

import android.graphics.PointF
import android.view.animation.AccelerateInterpolator
import com.testspace.core.Static
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
            Static.output(JSONObject(m).toString(4))
            messageStream?.cancel()
            messageStream = scope.launch {
                while (isActive) {
                    send(m)
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

    private fun asTelemetryEvent(
        event: GamepadEvent,
        offsets: ControlOffsets,
        interpolation: ControlInterpolation,
    ): String {
        val rawPitch = -event.rightStickY + offsets.pitch
        val rawYaw = event.rightStickX + offsets.yaw
        val rawSteer = -event.leftStickX + offsets.steer
        val breakTrigger = event.leftTrigger
        val rightTrigger = event.rightTrigger
        val longTrigger = if (breakTrigger > rightTrigger) {
            event.leftTrigger
        } else {
            -rightTrigger
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
    private val pitchTranslator: (Float) -> Float,
    private val yaw: AccelerateInterpolator?,
    private val yawTranslator: (Float) -> Float,
    private val steer: AccelerateInterpolator?,
    private val steerTranslator: (Float) -> Float,
    private val long: AccelerateInterpolator?,
    private val longTranslator: (Float) -> Float,
) {
    fun fixPitch(value: Float): Float {
        return fix(pitch, pitchTranslator, value)
    }

    fun fixYaw(value: Float): Float {
        return fix(yaw, yawTranslator, value)
    }
    fun fixSteer(value: Float): Float {
        return fix(steer, steerTranslator, value)
    }
    fun fixLong(value: Float): Float {
        return fix(long, longTranslator, value)
    }

    private fun fix(interpolator: AccelerateInterpolator?,
                    translator: (Float) -> Float,
                    value: Float): Float {
        val translatedValue = translator(value)
        if (interpolator == null) {
            return translatedValue
        }

        val sign = sign(translatedValue)
        val interpolated = interpolator.getInterpolation(translatedValue.absoluteValue)
        return interpolated * sign
    }
}

private fun ControlTuning.asInterpolation() = ControlInterpolation(
    pitch = create(pitchFactor),
    pitchTranslator = create(pitchZone),
    yaw = create(yawFactor),
    yawTranslator = create(yawZone),
    steer = create(steerFactor),
    steerTranslator = create(steerZone),
    long = create(longFactor),
    longTranslator = create(longZone),
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

private fun create(zone: PointF?): (Float) -> Float {
    if (zone == null) {
        return { it }
    }
    if (zone.x.isNaN() || zone.y.isNaN()) {
        return { it }
    }

    return { input ->
        val s = input.sign
        translate(valueX = input.absoluteValue, x1 = 0f, x2 = 1f, y1 = zone.x,  y2 = zone.y) * s
    }
}

fun translate(valueX: Float, x1: Float, x2: Float, y1: Float, y2: Float): Float {
    val raw = y1 + (valueX - x1) * (y2 - y1) / (x2 - x1)
    return raw.coerceIn(y1, y2)
}


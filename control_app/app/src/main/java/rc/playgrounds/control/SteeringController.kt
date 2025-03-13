package rc.playgrounds.control

import android.graphics.PointF
import android.view.animation.AccelerateInterpolator
import com.rc.playgrounds.config.model.MappingZone
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import rc.playgrounds.config.ConfigModel
import rc.playgrounds.control.gamepad.GamepadEvent
import rc.playgrounds.control.gamepad.GamepadEventStream
import rc.playgrounds.stream.StreamCmdHash
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.math.absoluteValue
import kotlin.math.sign
import kotlin.math.withSign

@OptIn(ExperimentalCoroutinesApi::class)
class SteeringController(
    private val gamepadEventStream: GamepadEventStream,
    private val scope: CoroutineScope,
    private val configModel: ConfigModel,
    private val streamCmdHash: StreamCmdHash,
) {

    private val controlServer = MutableStateFlow<com.rc.playgrounds.config.model.ControlServer?>(null)
    private var emitter: SteeringEmitter? = null

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
            emitter = SteeringEmitter(
                c,
                scope,
                gamepadEventStream,
                configModel.configFlow,
                streamCmdHash.hash,
            )
        }
    }
}

private class SteeringEmitter(
    val config: com.rc.playgrounds.config.model.ControlServer,
    private val scope: CoroutineScope,
    private val gamepadEventStream: GamepadEventStream,
    private val configFlow: StateFlow<com.rc.playgrounds.config.Config>,
    private val streamCmdHash: Flow<String>,
) {
    private val messages: Flow<String> = combine(
        configFlow,
        gamepadEventStream.events,
        streamCmdHash,
    ) { config: com.rc.playgrounds.config.Config, event: GamepadEvent, streamHash: String ->
        val steeringEvent: JSONObject = asSteeringEvent(
            event,
            offsets = config.controlOffsets,
            interpolation = config.controlTuning.asInterpolation(),
            streamCmd = config.remoteStreamCmd,
            streamCmdHash = streamHash,
        )
        printEvent(steeringEvent, event)
        steeringEvent.toString()
    }

    // long: 0.1 (raw: 0.5) steer: 0.0 (raw: 0.0) pitch: ... yaw: ...
    private fun printEvent(se: JSONObject, e: GamepadEvent) {
        val long: Double = se.optDouble("long")
        val text =
            "long: %.2f (raw: %.2f) ".format(long, if (long > 0) e.rightTrigger else e.leftTrigger) +
            "steer: %.2f (raw: %.2f) ".format(se.optDouble("steer"), -e.leftStickX) +
            "pitch: %.2f (raw: %.2f) ".format(se.optDouble("pitch"), -e.rightStickY) +
            "yaw: %.2f (raw: %.2f)".format(se.optDouble("yaw"), e.rightStickX)
        Static.output(text)
    }

    private val job = scope.launch {
        var messageStream: Job? = null
        messages.collect { m ->
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

    private fun asSteeringEvent(
        event: GamepadEvent,
        offsets: com.rc.playgrounds.config.model.ControlOffsets,
        interpolation: ControlInterpolation,
        streamCmd: String,
        streamCmdHash: String,
    ): JSONObject {
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
            .put("stream_cmd", streamCmd)
            .put("stream_cmd_hash", streamCmdHash)
        return json
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

    private fun fix(
        interpolator: AccelerateInterpolator?,
        translator: (Float) -> Float,
        value: Float
    ): Float {
        val translatedValue = translator(value)
        if (interpolator == null) {
            return translatedValue
        }

        val sign = sign(translatedValue)
        val interpolated = interpolator.getInterpolation(translatedValue.absoluteValue)
        return interpolated * sign
    }
}

private fun com.rc.playgrounds.config.model.ControlTuning.asInterpolation() = ControlInterpolation(
    pitch = create(pitchFactor),
    pitchTranslator = create(pitchZone),
    yaw = create(yawFactor),
    yawTranslator = create(yawZone),
    steer = create(steerFactor),
    steerTranslator = create(steerZone),
    long = create(longFactor),
    longTranslator = if (longZones.isNotEmpty()) {
        val zonesNegative = longZonesNegative.ifEmpty { longZones }
        create(negative = zonesNegative, positive = longZones)
    } else {
        create(PointF(0f,1f))
    },
)

fun create(negative: List<MappingZone>, positive: List<MappingZone>): (Float) -> Float {
    return { input ->
        val zones = if (input >= 0) {
            positive
        } else {
            negative
        }
        val absInput = input.absoluteValue
        zones
            .find { absInput >= it.src.x && absInput <= it.src.y }
            ?.let {
                translate(absInput, it.src.x, it.src.y, it.dst.x, it.dst.y)
            }
            ?.withSign(input)
            ?: input
    }
}

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
        translate(valueX = input.absoluteValue, x1 = 0f, x2 = 1f, y1 = zone.x, y2 = zone.y) * s
    }
}

fun translate(valueX: Float, x1: Float, x2: Float, y1: Float, y2: Float): Float {
    val raw = y1 + (valueX - x1) * (y2 - y1) / (x2 - x1)
    return raw.coerceIn(y1, y2)
}


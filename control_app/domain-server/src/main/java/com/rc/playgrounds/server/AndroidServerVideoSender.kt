package com.rc.playgrounds.server

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaCodec.BufferInfo
import android.os.Bundle
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicReference

class AndroidServerVideoSender(
    private val target: StateFlow<VideoTarget?>,
    private val scope: CoroutineScope,
) : ServerVideoSender {
    private val pending = AtomicReference<CameraFrame?>(null)
    @Volatile private var running = false
    private var encoder: MediaCodec? = null
    private var requestedWidth = 0
    private var requestedHeight = 0
    private var encodedWidth = 0
    private var encodedHeight = 0
    private var frameCount = 0L
    private var header: ByteArray? = null
    private var socket: DatagramSocket? = null
    private var address: InetAddress? = null
    private var resolvedHost: String? = null
    private var failedHost: String? = null
    private var loggedFormat = false
    private var loggedHost: String? = null
    private var loggedPort = 0

    override fun start() {
        if (running) {
            return
        }
        running = true
        scope.launch {
            socket = DatagramSocket()
            val current = target.value
            if (current == null) {
                Log.i(TAG, "udp waiting for client")
            } else {
                Log.i(TAG, "udp ${current.host}:${current.port}")
            }
        }
    }

    override fun stop() {
        running = false
        scope.launch {
            releaseEncoder()
            socket?.close()
            socket = null
            address = null
            resolvedHost = null
            failedHost = null
            loggedFormat = false
            loggedHost = null
            loggedPort = 0
        }
    }

    override fun onFrame(frame: ByteArray, width: Int, height: Int) {
        if (!running || width < 2 || height < 2) {
            return
        }
        pending.set(CameraFrame(frame, width, height))
        scope.launch {
            val next = pending.getAndSet(null) ?: return@launch
            if (!running) {
                return@launch
            }
            try {
                encode(next)
            } catch (e: Exception) {
                Log.e(TAG, "encode", e)
                releaseEncoder()
            }
        }
    }

    private fun encode(frame: CameraFrame) {
        val width = frame.width - frame.width % 2
        val height = frame.height - frame.height % 2
        val (targetWidth, targetHeight) = cappedSize(width, height)
        val codec = encoderFor(targetWidth, targetHeight) ?: return
        val index = codec.dequeueInputBuffer(ENQUEUE_TIMEOUT_US)
        if (index < 0) {
            return
        }
        val buffer = codec.getInputBuffer(index) ?: return
        buffer.clear()
        val size = writeNv12(
            buffer = buffer,
            data = frame.data,
            srcWidth = frame.width,
            srcHeight = frame.height,
            width = encodedWidth,
            height = encodedHeight,
        )
        if (frameCount == 0L) {
            Log.i(TAG, "frame ${frame.width}x${frame.height} -> ${encodedWidth}x${encodedHeight}")
        }
        val pts = frameCount * 1_000_000L / FRAME_RATE
        frameCount++
        codec.queueInputBuffer(index, 0, size, pts, 0)
        if (frameCount % SYNC_FRAME_INTERVAL == 0L) {
            requestSyncFrame(codec)
        }
        drain(codec)
    }

    private fun cappedSize(width: Int, height: Int): Pair<Int, Int> {
        if (width <= MAX_ENCODE_WIDTH && height <= MAX_ENCODE_HEIGHT) {
            return width to height
        }
        val scale = minOf(
            MAX_ENCODE_WIDTH.toFloat() / width,
            MAX_ENCODE_HEIGHT.toFloat() / height,
        )
        val targetWidth = (width * scale).toInt().coerceAtLeast(16) and 1.inv()
        val targetHeight = (height * scale).toInt().coerceAtLeast(16) and 1.inv()
        return targetWidth to targetHeight
    }

    private fun encoderFor(width: Int, height: Int): MediaCodec? {
        if (encoder != null && requestedWidth == width && requestedHeight == height) {
            return encoder
        }
        releaseEncoder()
        return try {
            val created = createEncoder(width, height)
            encoder = created.codec
            requestedWidth = width
            requestedHeight = height
            encodedWidth = created.width
            encodedHeight = created.height
            frameCount = 0
            header = null
            loggedFormat = false
            created.codec
        } catch (e: Exception) {
            Log.e(TAG, "encoder ${width}x${height}", e)
            null
        }
    }

    private fun createEncoder(width: Int, height: Int): StartedEncoder {
        val sizes = listOf(
            width to height,
            width - width % 16 to height - height % 16,
        ).distinct().filter { (w, h) -> w >= 16 && h >= 16 }
        var last: Exception? = null
        for ((candidateWidth, candidateHeight) in sizes) {
            for (withProfile in booleanArrayOf(true, false)) {
                for (color in COLOR_FORMATS) {
                    val codec = MediaCodec.createEncoderByType(MIME)
                    try {
                        Log.i(TAG, "configure ${candidateWidth}x${candidateHeight} color=$color profile=$withProfile")
                        codec.configure(
                            format(candidateWidth, candidateHeight, color, withProfile),
                            null,
                            null,
                            MediaCodec.CONFIGURE_FLAG_ENCODE,
                        )
                        codec.start()
                        return StartedEncoder(codec, candidateWidth, candidateHeight)
                    } catch (e: Exception) {
                        last = e
                        codec.release()
                    }
                }
            }
        }
        throw last ?: IllegalStateException("h264 encoder")
    }

    private fun format(width: Int, height: Int, color: Int, withProfile: Boolean): MediaFormat {
        return MediaFormat.createVideoFormat(MIME, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, color)
            setInteger(MediaFormat.KEY_BIT_RATE, BITRATE)
            setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            setInteger(MediaFormat.KEY_PREPEND_HEADER_TO_SYNC_FRAMES, 1)
            if (withProfile) {
                setInteger(
                    MediaFormat.KEY_PROFILE,
                    MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline,
                )
                setInteger(
                    MediaFormat.KEY_LEVEL,
                    MediaCodecInfo.CodecProfileLevel.AVCLevel4,
                )
            }
        }
    }

    private fun requestSyncFrame(codec: MediaCodec) {
        try {
            val params = Bundle()
            params.putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0)
            codec.setParameters(params)
        } catch (e: Exception) {
            Log.w(TAG, "sync frame", e)
        }
    }

    private fun drain(codec: MediaCodec) {
        val info = BufferInfo()
        while (true) {
            val index = codec.dequeueOutputBuffer(info, 0)
            when {
                index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> captureHeader(codec.outputFormat)
                index >= 0 -> {
                    val buffer = codec.getOutputBuffer(index)
                    if (buffer != null && info.size > 0) {
                        val bytes = ByteArray(info.size)
                        buffer.position(info.offset)
                        buffer.limit(info.offset + info.size)
                        buffer.get(bytes)
                        val configBuffer = info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0
                        val key = info.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME != 0
                        if (configBuffer) {
                            header = H264Packets.toAnnexB(bytes)
                            header?.let { send(it) }
                        } else {
                            send(accessUnit(bytes, key))
                        }
                    }
                    codec.releaseOutputBuffer(index, false)
                }
                else -> return
            }
        }
    }

    private fun captureHeader(format: MediaFormat) {
        val csd0 = format.getByteBuffer("csd-0")?.toByteArray() ?: return
        val csd1 = format.getByteBuffer("csd-1")?.toByteArray()
        header = if (csd1 == null) {
            H264Packets.toAnnexB(csd0)
        } else {
            H264Packets.toAnnexB(csd0) + H264Packets.toAnnexB(csd1)
        }
    }

    private fun accessUnit(bytes: ByteArray, key: Boolean): ByteArray {
        val annex = H264Packets.toAnnexB(bytes)
        val csd = header
        val idr = key || H264Packets.nalType(annex) == H264Packets.NAL_IDR
        if (!idr || csd == null || H264Packets.nalType(annex) == H264Packets.NAL_SPS) {
            return annex
        }
        return csd + annex
    }

    private fun send(annex: ByteArray) {
        val udp = socket ?: return
        val current = target.value ?: return
        val destination = resolve(current.host) ?: return
        if (!loggedFormat || loggedHost != current.host || loggedPort != current.port) {
            loggedFormat = true
            loggedHost = current.host
            loggedPort = current.port
            Log.i(TAG, "h264 ${encodedWidth}x${encodedHeight} -> ${current.host}:${current.port}")
        }
        for (chunk in H264Packets.packets(annex)) {
            udp.send(DatagramPacket(chunk, chunk.size, destination, current.port))
        }
    }

    private fun resolve(host: String): InetAddress? {
        if (resolvedHost == host) {
            return address
        }
        if (failedHost == host) {
            return null
        }
        return try {
            val resolved = InetAddress.getByName(host)
            address = resolved
            resolvedHost = host
            failedHost = null
            resolved
        } catch (e: Exception) {
            failedHost = host
            Log.w(TAG, "udp $host", e)
            null
        }
    }

    private fun releaseEncoder() {
        val codec = encoder ?: return
        encoder = null
        requestedWidth = 0
        requestedHeight = 0
        try {
            codec.stop()
        } catch (_: Exception) {
        }
        codec.release()
    }

    private data class CameraFrame(
        val data: ByteArray,
        val width: Int,
        val height: Int,
    )

    private data class StartedEncoder(
        val codec: MediaCodec,
        val width: Int,
        val height: Int,
    )

    private companion object {
        const val TAG = "ServerVideo"
        const val MIME = MediaFormat.MIMETYPE_VIDEO_AVC
        const val BITRATE = 2_000_000
        const val FRAME_RATE = 30
        const val ENQUEUE_TIMEOUT_US = 10_000L
        const val SYNC_FRAME_INTERVAL = 10L
        const val MAX_ENCODE_WIDTH = 640
        const val MAX_ENCODE_HEIGHT = 360
        val COLOR_FORMATS = intArrayOf(
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar,
        )
    }
}

private fun writeNv12(
    buffer: ByteBuffer,
    data: ByteArray,
    srcWidth: Int,
    srcHeight: Int,
    width: Int,
    height: Int,
): Int {
    val ySize = width * height
    val out = ByteArray(ySize * 3 / 2)
    for (row in 0 until height) {
        val sy = row * srcHeight / height
        val dst = row * width
        val src = sy * srcWidth
        for (col in 0 until width) {
            out[dst + col] = data[src + col * srcWidth / width]
        }
    }
    val chroma = srcWidth * srcHeight
    val chromaWidth = width / 2
    val chromaHeight = height / 2
    val srcChromaWidth = srcWidth / 2
    val srcChromaHeight = srcHeight / 2
    var dst = ySize
    for (row in 0 until chromaHeight) {
        val sy = row * srcChromaHeight / chromaHeight
        val srcRow = chroma + sy * srcWidth
        for (col in 0 until chromaWidth) {
            val sx = col * srcChromaWidth / chromaWidth
            val src = srcRow + sx * 2
            out[dst++] = data[src + 1]
            out[dst++] = data[src]
        }
    }
    buffer.put(out)
    return out.size
}

private fun ByteBuffer.toByteArray(): ByteArray {
    val duplicate = duplicate()
    val bytes = ByteArray(duplicate.remaining())
    duplicate.get(bytes)
    return bytes
}

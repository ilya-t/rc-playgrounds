package rc.playgrounds.stream

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class VLCStreamReceiver(context: Context,
                        private val surfaceView: SurfaceView,
                        private var uri: Uri,
) : StreamReceiver {
    private val libVLC: LibVLC
    private val mediaPlayer: MediaPlayer
    private val sizeChangeObservation: SurfaceHolder.Callback
    init {
        Log.d("VLC", "VLC INSTANTIATED!")

        // VLC configuration for low latency and UDP streaming
        val options = ArrayList<String>()
        val logFile = File(context.filesDir, "vlc.log")
        val logFilePath = logFile.absolutePath
        options.add("--verbose=3") // Enable verbose logging
        options.add("--log-verbose") // Ensure logs are detailed
        libVLC = LibVLC(context, options)
        mediaPlayer = MediaPlayer(libVLC)
        mediaPlayer.vlcVout.apply {
            setWindowSize(surfaceView.width, surfaceView.height)
            setVideoView(surfaceView)
            attachViews()
        }
        sizeChangeObservation = object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
                mediaPlayer.vlcVout.setWindowSize(width, height)
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
            }
        }

        surfaceView.holder.addCallback(sizeChangeObservation)
        mediaPlayer.aspectRatio = null
        mediaPlayer.scale = 0f // Automatic scaling
    }


    fun playFromAsset(c: Context, path: String) {
        val assetPath = copyAsset(c, path)
        uri = Uri.parse("file://$assetPath")
        play()
    }

    private fun copyAsset(context: Context, assetFileName: String): String? {
        val sdpFile = File(context.filesDir, assetFileName)
        if (sdpFile.exists()) return sdpFile.absolutePath

        try {
            context.assets.open(assetFileName).use { inputStream ->
                FileOutputStream(sdpFile).use { outputStream ->
                    val buffer = ByteArray(1024)
                    var length: Int
                    while ((inputStream.read(buffer).also { length = it }) > 0) {
                        outputStream.write(buffer, 0, length)
                    }
                    return sdpFile.absolutePath
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    override fun play() {
        val media = Media(libVLC, uri)

        // Ensure correct H.264 processing
        media.addOption(":demux=h264")

        // **Reduce Buffering & Improve Latency**
        media.addOption(":network-caching=0")   // Minimum possible caching (default: 1000ms)
        media.addOption(":live-caching=0")      // Lower live cache
        media.addOption(":file-caching=0")       // No file caching
        media.addOption(":disc-caching=0")       // No disc caching
        media.addOption(":rtsp-caching=0")       // No RTSP caching

        // **Fix Decoder Issues**
        media.addOption(":codec=avcodec")
//        media.addOption(":avcodec-hw=none")      // Disable hardware decoding (debug first)
//        media.addOption(":no-mediacodec-dr")     // Disable MediaCodec direct rendering
        media.addOption(":no-mediacodec-hurry-up") // Disable hardware decoder rush mode
        media.addOption(":no-opencl")            // Disable OpenCL (causes instability sometimes)

        // **Handle Sync & Frame Dropping**
        media.addOption(":clock-jitter=0")       // Disable jitter correction
        media.addOption(":clock-synchro=0")      // Prevent VLC from syncing frames unnecessarily
        media.addOption(":drop-late-frames")     // Drop delayed frames instead of buffering
        media.addOption(":skip-frames")          // Skip frames if needed to maintain playback speed

        // **Fix UDP Packet Truncation**
        media.addOption(":mtu=1500")             // Fix MTU size mismatch issue
        media.addOption(":udp-buffer=1048576")   // Increase UDP buffer size

        // **Enable Logging for Debugging**
        media.addOption(":verbose")
        media.addOption(":log-verbose")

        media.setHWDecoderEnabled(true, false)

        mediaPlayer.media = media
        media.release()
        mediaPlayer.play()
        Log.d("VLC", "VLC is playback started: $uri!")
    }


    override fun release() {
        Log.d("VLC", "VLC RELEASED! ${RuntimeException().stackTraceToString()}")
        surfaceView.holder.removeCallback(sizeChangeObservation)
        mediaPlayer.release()
        libVLC.release()
    }
}

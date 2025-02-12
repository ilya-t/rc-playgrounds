package rc.playgrounds

import android.content.Context
import android.net.Uri
import android.view.SurfaceHolder
import android.view.SurfaceView
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class StreamReceiver(context: Context,
                     private val surfaceView: SurfaceView) {
    private val libVLC: LibVLC
    private val mediaPlayer: MediaPlayer
    private val sizeChangeObservation: SurfaceHolder.Callback
    init {
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
        play(Uri.parse("file://$assetPath"))
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

    fun play(uri: Uri?) {
//        Uri sdpUri = Uri.parse(
//                "udp://@:12345" //WORKS when: ffmpeg -re -i ./sample.mp4 -c:v libx264 -preset ultrafast -tune zerolatency -b:v 1500k -maxrate 1500k -bufsize 3000k -c:a aac -b:a 128k -f mpegts udp://192.168.1.232:12345
////                "file://" + sdpFilePath
//        );
        val media = Media(libVLC, uri)
        media.addOption(":verbose")
        media.addOption(":log-verbose")
        //        media.addOption(":rtp-client-port=12345");
//        media.addOption(":network-caching=500");
//        media.addOption(":sout-rtp-caching=300");
//        media.addOption(":demux=live555");
        media.setHWDecoderEnabled(true, false)
        mediaPlayer.media = media
        media.release()
        mediaPlayer.play()
    }

    fun release() {
        mediaPlayer.release()
        libVLC.release()
        surfaceView.holder.removeCallback(sizeChangeObservation)

    }
}

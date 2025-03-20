package com.rc.playgrounds.stream

import android.view.SurfaceView
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.rc.playgrounds.gstreamer.GStreamerFacade
import com.rc.playgrounds.gstreamer.Logger
import com.rc.playgrounds.status.gstreamer.StreamerEvents

class GStreamerReceiver(
    private val streamerEvents: StreamerEvents,
    activity: AppCompatActivity,
    private val surfaceContainer: ViewGroup,
    pipeline: String,
) : StreamReceiver {
    private val logger = object : Logger {
        override fun logError(e: Exception) {
            streamerEvents.emit(e)
        }

        override fun logMessage(message: String) {
            streamerEvents.emit(message)
        }
    }

    private val surfaceView = createSurfaceAt(surfaceContainer)

    private val gStreamerFacade = GStreamerFacade(
        activity,
        surfaceView,
        logger,
        pipeline,
    )

    override fun play() {
        gStreamerFacade.play()
    }

    override fun release() {
        gStreamerFacade.close()
        surfaceContainer.removeAllViews()
    }
}

private fun createSurfaceAt(container: ViewGroup): SurfaceView {
    container.removeAllViews()
    val surfaceView = SurfaceView(container.context)
    container.addView(surfaceView, ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    return surfaceView
}
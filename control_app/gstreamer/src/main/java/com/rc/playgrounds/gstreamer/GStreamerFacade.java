package com.rc.playgrounds.gstreamer;

import android.content.Context;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

import org.freedesktop.gstreamer.GStreamer;

public class GStreamerFacade {
    private final Logger logger;
    private final SurfaceView surfaceView;
    private final SurfaceHolder.Callback surfaceCallbacks = new SurfaceHolder.Callback() {
        @Override
        public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width,
                                   int height) {
            logger.logMessage("Surface changed to format " + format + " width "
                    + width + " height " + height);
            nativeSurfaceInit(holder.getSurface());
        }

        @Override
        public void surfaceCreated(@NonNull SurfaceHolder holder) {
            logger.logMessage("Surface created: " + holder.getSurface());
        }

        @Override
        public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
            logger.logMessage("Surface destroyed");
            nativeSurfaceFinalize();
        }
    };

    private native void nativeInit(String pipelineDesc);// Initialize native code, build pipeline, etc
    private native void nativeFinalize(); // Destroy pipeline and shutdown native code
    private native void nativePlay();     // Set pipeline to PLAYING
    private native void nativePause();    // Set pipeline to PAUSED
    private static native boolean nativeClassInit(); // Initialize native class: cache Method IDs for callbacks
    private native void nativeSurfaceInit(Object surface);
    private native void nativeSurfaceFinalize();
    private long native_custom_data;      // Native code will use this to keep private data

    private boolean is_playing_desired;   // Whether the user asked to go to PLAYING

    // Called when the activity is first created.
    public GStreamerFacade(
            Context context,
            SurfaceView surfaceView,
            Logger logger,
            String pipeline) {
        this.logger = logger;
        this.surfaceView = surfaceView;
        // Initialize GStreamer and warn if it fails
        try {
            GStreamer.init(context);
        } catch (Exception e) {
            logger.logError(new RuntimeException("Error on Gstreamer init!", e));
            return;
        }

        surfaceView.getHolder().addCallback(surfaceCallbacks);
        nativeInit(pipeline);
    }

    public void play() {
        is_playing_desired = true;
        nativePlay();
    }

    public void pause() {
        is_playing_desired = false;
        nativePause();
    }

    public void close() {
        surfaceView.getHolder().removeCallback(surfaceCallbacks);
        nativeFinalize();
    }

    // Called from native code. This sets the content of the TextView from the UI thread.
    private void setMessage(final String message) {
        logger.logMessage(message);
    }

    // Called from native code. Native code calls this once it has created its pipeline and
    // the main loop is running, so it is ready to accept commands.
    private void onGStreamerInitialized () {
        logger.logMessage("Gst initialized. Restoring state, playing:" + is_playing_desired);
        // Restore previous playing state
        if (is_playing_desired) {
            nativePlay();
        } else {
            nativePause();
        }
    }

    static {
        System.loadLibrary("gstreamer_android");
        System.loadLibrary("rtsp-example");
        nativeClassInit();
    }
}

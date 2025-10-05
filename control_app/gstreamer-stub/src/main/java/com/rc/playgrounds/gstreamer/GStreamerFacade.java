package com.rc.playgrounds.gstreamer;

import android.content.Context;
import android.view.SurfaceView;

/**
 * Stub implementation used when native GStreamer is unavailable.
 */
public class GStreamerFacade {
    public GStreamerFacade(Context context, SurfaceView surfaceView, Logger logger, String pipeline) {
        // Intentionally left blank.
    }

    public void play() {
        // No-op stub implementation.
    }

    public void pause() {
        // No-op stub implementation.
    }

    public void close() {
        // No-op stub implementation.
    }
}

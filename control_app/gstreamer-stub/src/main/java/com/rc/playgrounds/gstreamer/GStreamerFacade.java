package com.rc.playgrounds.gstreamer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Stub implementation used when native GStreamer is unavailable.
 */
public class GStreamerFacade {
    public GStreamerFacade(Context context, SurfaceView surfaceView, Logger logger, String pipeline) {
        // Draw a simple message on the SurfaceView so developers can tell the
        // stub is active.
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                drawStubMessage(holder);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                drawStubMessage(holder);
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                // Nothing to clean up.
            }
        });

        if (surfaceHolder.getSurface().isValid()) {
            drawStubMessage(surfaceHolder);
        }
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

    private void drawStubMessage(SurfaceHolder holder) {
        Canvas canvas = holder.lockCanvas();
        if (canvas == null) {
            return;
        }

        try {
            canvas.drawColor(Color.BLACK);

            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(Color.WHITE);
            paint.setTextAlign(Paint.Align.LEFT);

            float textSize = canvas.getHeight() * 0.1f;
            paint.setTextSize(textSize);

            String message = "GStreamer Stub";

            Rect textBounds = new Rect();
            paint.getTextBounds(message, 0, message.length(), textBounds);

            float x = (canvas.getWidth() - textBounds.width()) / 2f - textBounds.left;
            float y = (canvas.getHeight() + textBounds.height()) / 2f - textBounds.bottom;

            canvas.drawText(message, x, y, paint);
        } finally {
            holder.unlockCanvasAndPost(canvas);
        }
    }
}

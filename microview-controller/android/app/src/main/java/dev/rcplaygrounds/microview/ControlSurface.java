package dev.rcplaygrounds.microview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

import java.util.Locale;

/** Full-screen two-stick multitouch interface with mode buttons and smoothness slider. */
public final class ControlSurface extends View {
    private static final int BACKGROUND = 0xff111a2c;
    private static final int PANEL = 0xff202d46;
    private static final int LINE = 0xff42516d;
    private static final int LABEL = 0xffc6d4eb;
    private static final int ACCENT = 0xff30dbc0;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ControlState controls;
    private int leftPointer = -1;
    private int rightPointer = -1;
    private int smoothPointer = -1;

    public ControlSurface(Context context, ControlState controls) {
        super(context);
        this.controls = controls;
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        setBackgroundColor(BACKGROUND);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private float centerY() { return getHeight() * .52f; }
    private float leftX() { return getWidth() * .21f; }
    private float rightX() { return getWidth() * .59f; }
    private float radius() { return Math.min(getHeight() * .32f, getWidth() * .15f); }
    private float sideLeft() { return getWidth() * .79f; }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        ControlState.Snapshot s = controls.snapshot();
        canvas.drawColor(BACKGROUND);
        text(canvas, "ГАЗ / РУЛЬ", leftX(), dp(26), dp(17), LABEL, true);
        text(canvas, "YAW / PITCH", rightX(), dp(26), dp(17), LABEL, true);
        drawStick(canvas, leftX(), centerY(), radius(), s.steer, s.throttle,
                ACCENT);
        drawStick(canvas, rightX(), centerY(), radius(), s.yaw, s.pitch, ACCENT);
        text(canvas, String.format(Locale.US, "M %d  S %d", s.pwm[0], s.pwm[1]),
                leftX(), getHeight() - dp(12), dp(16), LABEL, true);
        text(canvas, String.format(Locale.US, "Y %d  P %d", s.pwm[2], s.pwm[3]),
                rightX(), getHeight() - dp(12), dp(16), LABEL, true);
        drawModeAndSmoothness(canvas, s);
    }

    private void drawStick(Canvas c, float cx, float cy, float radius,
                           float x, float y, int knobColor) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(PANEL);
        c.drawCircle(cx, cy, radius, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1.5f));
        paint.setColor(LINE);
        c.drawCircle(cx, cy, radius, paint);
        c.drawLine(cx - radius, cy, cx + radius, cy, paint);
        c.drawLine(cx, cy - radius, cx, cy + radius, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(knobColor);
        c.drawCircle(cx + x * radius, cy - y * radius, Math.max(dp(17), radius * .2f), paint);
    }

    private void drawModeAndSmoothness(Canvas c, ControlState.Snapshot s) {
        float x1 = sideLeft();
        float x2 = getWidth() - dp(9);
        float h = getHeight();
        text(c, "GIMBAL MODE", (x1 + x2) * .5f, dp(26), dp(12), LABEL, true);
        for (int i = 0; i < 3; i++) {
            float top = modeTop(i);
            float bottom = modeBottom(i);
            roundedRect(c, x1, top, x2, bottom, s.mode == i ? ACCENT : PANEL);
            text(c, ControlState.MODE_LABELS[i], (x1 + x2) * .5f,
                    (top + bottom) * .5f + dp(5), dp(12),
                    s.mode == i ? BACKGROUND : LABEL, true);
        }
        text(c, "SMOOTHNESS", (x1 + x2) * .5f, h * .705f, dp(12), LABEL, true);
        float trackTop = h * .75f;
        float trackBottom = h * .94f;
        float trackY = trackBottom - s.smoothness * (trackBottom - trackTop);
        float trackX = (x1 + x2) * .5f;
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(LINE);
        paint.setStrokeWidth(dp(5));
        c.drawLine(trackX, trackTop, trackX, trackBottom, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(ACCENT);
        c.drawCircle(trackX, trackY, dp(12), paint);
        text(c, String.format(Locale.US, "%d", s.pwm[5]),
                x1 + dp(20), trackY + dp(5), dp(11), LABEL, false);
    }

    private float modeTop(int i) { return getHeight() * (.15f + i * .168f); }
    private float modeBottom(int i) { return getHeight() * (.15f + i * .168f + .135f); }

    private void roundedRect(Canvas c, float l, float t, float r, float b, int color) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        c.drawRoundRect(new RectF(l, t, r, b), dp(8), dp(8), paint);
    }

    private void text(Canvas c, String value, float x, float baseline, float size,
                      int color, boolean centered) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        paint.setTextSize(size);
        paint.setFakeBoldText(false);
        paint.setTextAlign(centered ? Paint.Align.CENTER : Paint.Align.LEFT);
        c.drawText(value, x, baseline, paint);
    }

    /** Release joystick pointers after leaving app focus. */
    public void resetTouches() {
        leftPointer = rightPointer = smoothPointer = -1;
        controls.emergencyNeutral();
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int action = event.getActionMasked();
        final int actionIndex = event.getActionIndex();
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            int id = event.getPointerId(actionIndex);
            float x = event.getX(actionIndex);
            float y = event.getY(actionIndex);
            if (x >= sideLeft()) {
                if (y >= getHeight() * .73f) {
                    if (smoothPointer < 0) smoothPointer = id;
                } else {
                    for (int i = 0; i < 3; i++) {
                        if (y >= modeTop(i) && y <= modeBottom(i)) controls.setMode(i);
                    }
                }
            } else if (near(x, y, leftX(), centerY(), radius() * 1.25f)) {
                if (leftPointer < 0) leftPointer = id;
            } else if (near(x, y, rightX(), centerY(), radius() * 1.25f)) {
                if (rightPointer < 0) rightPointer = id;
            }
        }
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
            int id = event.getPointerId(actionIndex);
            if (id == leftPointer) { leftPointer = -1; controls.setLeft(0f, 0f); }
            if (id == rightPointer) { rightPointer = -1; controls.setRight(0f, 0f); }
            if (id == smoothPointer) smoothPointer = -1;
        } else if (action == MotionEvent.ACTION_CANCEL) {
            leftPointer = rightPointer = smoothPointer = -1;
            controls.setLeft(0f, 0f);
            controls.setRight(0f, 0f);
        }
        // Also sample every active pointer on POINTER_DOWN and MOVE.
        if (action != MotionEvent.ACTION_CANCEL) {
            updatePointer(event, leftPointer, true);
            updatePointer(event, rightPointer, false);
            if (smoothPointer >= 0) {
                int index = event.findPointerIndex(smoothPointer);
                if (index >= 0) {
                    float value = (getHeight() * .94f - event.getY(index)) /
                            (getHeight() * (.94f - .75f));
                    controls.setSmoothness(value);
                }
            }
        }
        invalidate();
        return true;
    }

    private void updatePointer(MotionEvent event, int pointerId, boolean left) {
        if (pointerId < 0) return;
        int index = event.findPointerIndex(pointerId);
        if (index < 0) return;
        float radius = radius();
        float cx = left ? leftX() : rightX();
        float dx = (event.getX(index) - cx) / radius;
        float dy = (centerY() - event.getY(index)) / radius;
        float length = (float) Math.hypot(dx, dy);
        if (length > 1f) { dx /= length; dy /= length; }
        if (left) controls.setLeft(dx, dy);
        else controls.setRight(dx, dy);
    }

    private static boolean near(float x, float y, float cx, float cy, float r) {
        float dx = x - cx, dy = y - cy;
        return dx * dx + dy * dy <= r * r;
    }
}
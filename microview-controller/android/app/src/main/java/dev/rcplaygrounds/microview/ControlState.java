package dev.rcplaygrounds.microview;

/** Mutable joystick state, synchronized because the serial thread reads it at ~30 Hz. */
public final class ControlState {
    public static final int CHANNELS = 6;
    public static final int NEUTRAL = 1500;
    public static final int MIN_PWM = 1000;
    public static final int MAX_PWM = 2000;

    // Replace with verified CH5 PWM values and mode labels for your gimbal.
    public static final int[] MODE_PWM = {1100, 1500, 1900};
    public static final String[] MODE_LABELS = {"HORIZON", "FOLLOW", "LOCK"};

    private float steer = 0f;
    private float throttle = 0f;
    private float yaw = 0f;
    private float pitch = 0f;
    private float smoothness = 0.5f;
    private int mode = 0;

    public synchronized void setLeft(float steer, float throttle) {
        this.steer = clamp(steer);
        this.throttle = clamp(throttle);
    }

    public synchronized void setRight(float yaw, float pitch) {
        this.yaw = clamp(yaw);
        this.pitch = clamp(pitch);
    }

    public synchronized void setMode(int mode) {
        if (mode >= 0 && mode < MODE_PWM.length) this.mode = mode;
    }

    public synchronized void setSmoothness(float value) {
        this.smoothness = Math.max(0f, Math.min(1f, value));
    }

    /** Release the sticks when the app loses focus or the link closes. */
    public synchronized void emergencyNeutral() {
        steer = throttle = yaw = pitch = 0f;
    }

    public synchronized Snapshot snapshot() {
        int[] pwm = new int[]{
                axisToPwm(throttle),
                axisToPwm(steer),
                axisToPwm(yaw),
                axisToPwm(pitch),
                MODE_PWM[mode],
                MIN_PWM + Math.round(smoothness * (MAX_PWM - MIN_PWM))
        };
        return new Snapshot(pwm, steer, throttle, yaw, pitch, mode, smoothness);
    }

    public static final class Snapshot {
        public final int[] pwm;
        public final float steer, throttle, yaw, pitch, smoothness;
        public final int mode;

        private Snapshot(int[] pwm, float steer, float throttle,
                         float yaw, float pitch, int mode, float smoothness) {
            this.pwm = pwm;
            this.steer = steer;
            this.throttle = throttle;
            this.yaw = yaw;
            this.pitch = pitch;
            this.mode = mode;
            this.smoothness = smoothness;
        }
    }

    private static int axisToPwm(float axis) {
        if (Math.abs(axis) < 0.045f) axis = 0f;
        return NEUTRAL + Math.round(clamp(axis) * 500f);
    }

    private static float clamp(float v) {
        return Math.max(-1f, Math.min(1f, v));
    }
}
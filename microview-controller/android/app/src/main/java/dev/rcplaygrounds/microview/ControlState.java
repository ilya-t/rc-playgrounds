package dev.rcplaygrounds.microview;

import java.util.Arrays;

/** Mutable control state. Access is synchronized between the UI and serial I/O thread. */
public final class ControlState {
    public static final int CHANNELS = 6;
    public static final int NEUTRAL = 1500;
    public static final int MIN_PWM = 1000;
    public static final int MAX_PWM = 2000;
    public static final int TRIM_STEP_US = 5;
    public static final int MAX_TRIM_US = 200;

    public static final String[] CHANNEL_LABELS = {
            "CH1 GAS", "CH2 STEER", "CH3 YAW", "CH4 PITCH", "CH5 MODE", "CH6 SMOOTH"
    };
    // Replace with the verified PWM mode positions of the particular gimbal.
    public static final int[] MODE_PWM = {1100, 1500, 1900};
    public static final String[] MODE_LABELS = {"HORIZON", "FOLLOW", "LOCK"};

    private float steer = 0f;
    private float throttle = 0f;
    private float yaw = 0f;
    private float pitch = 0f;
    private float smoothness = 0.5f;
    private int mode = 0;
    // Pause/disconnect must send an absolute neutral ESC signal even with CH1 trim.
    private boolean motorNeutralOverride = false;
    private final int[] trimUs = new int[CHANNELS];
    private final boolean[] inverted = new boolean[CHANNELS];

    public synchronized void setLeft(float steer, float throttle) {
        this.steer = clamp(steer);
        this.throttle = clamp(throttle);
        motorNeutralOverride = false;
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

    /** On Android pause/disconnect, return sticks to their centered positions. */
    public synchronized void emergencyNeutral() {
        steer = throttle = yaw = pitch = 0f;
        motorNeutralOverride = true;
    }

    /** Trim is applied AFTER inversion, in physical PWM microseconds. */
    public synchronized void setTrim(int channel, int offsetUs) {
        requireChannel(channel);
        trimUs[channel] = Math.max(-MAX_TRIM_US, Math.min(MAX_TRIM_US, offsetUs));
    }

    public synchronized int adjustTrim(int channel, int steps) {
        requireChannel(channel);
        setTrim(channel, trimUs[channel] + steps * TRIM_STEP_US);
        return trimUs[channel];
    }

    public synchronized int getTrim(int channel) {
        requireChannel(channel);
        return trimUs[channel];
    }

    public synchronized void setInverted(int channel, boolean reverse) {
        requireChannel(channel);
        inverted[channel] = reverse;
    }

    public synchronized boolean isInverted(int channel) {
        requireChannel(channel);
        return inverted[channel];
    }

    public synchronized Snapshot snapshot() {
        int[] pwm = {
                axisToPwm(throttle),
                axisToPwm(steer),
                axisToPwm(yaw),
                axisToPwm(pitch),
                MODE_PWM[mode],
                MIN_PWM + Math.round(smoothness * (MAX_PWM - MIN_PWM))
        };
        for (int i = 0; i < CHANNELS; i++) {
            int delta = pwm[i] - NEUTRAL;
            pwm[i] = clampPwm(NEUTRAL + (inverted[i] ? -delta : delta) + trimUs[i]);
        }
        if (motorNeutralOverride) pwm[0] = NEUTRAL;
        return new Snapshot(pwm, Arrays.copyOf(trimUs, CHANNELS),
                Arrays.copyOf(inverted, CHANNELS), steer, throttle, yaw, pitch, mode, smoothness);
    }

    public static final class Snapshot {
        public final int[] pwm;
        public final int[] trimUs;
        public final boolean[] inverted;
        public final float steer, throttle, yaw, pitch, smoothness;
        public final int mode;

        private Snapshot(int[] pwm, int[] trimUs, boolean[] inverted,
                         float steer, float throttle, float yaw, float pitch,
                         int mode, float smoothness) {
            this.pwm = pwm;
            this.trimUs = trimUs;
            this.inverted = inverted;
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

    private static int clampPwm(int value) {
        return Math.max(MIN_PWM, Math.min(MAX_PWM, value));
    }

    private static float clamp(float value) {
        return Math.max(-1f, Math.min(1f, value));
    }

    private static void requireChannel(int channel) {
        if (channel < 0 || channel >= CHANNELS) {
            throw new IllegalArgumentException("Invalid channel: " + channel);
        }
    }
}
package dev.rcplaygrounds.microview;

/** No Android SDK needed; validate control snapshots and v2 UART frames. */
public final class ProtocolSelfTest {
    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }

    public static void main(String[] args) {
        ControlState state = new ControlState();
        check(state.snapshot().pwm[0] == 1500, "neutral motor on boot");
        state.setLeft(1f, 1f);
        check(state.snapshot().pwm[0] == 2000, "motor follows throttle immediately");
        check(state.snapshot().pwm[1] == 2000, "steering maximum");
        state.emergencyNeutral();
        check(state.snapshot().pwm[0] == 1500, "neutral on loss of focus");
        check(state.snapshot().pwm[1] == 1500, "steering neutral");
        state.setMode(2);
        check(state.snapshot().pwm[4] == 1900, "mode selection");
        state.setSmoothness(0f);
        check(state.snapshot().pwm[5] == 1000, "smoothness min");
        state.setSmoothness(1f);
        check(state.snapshot().pwm[5] == 2000, "smoothness max");
        byte[] packet = Protocol.command(65535, state.snapshot());
        String ascii = new String(packet, java.nio.charset.StandardCharsets.US_ASCII);
        check(ascii.startsWith("C,65535,1500,1500,1500,1500,1900,2000*"), "no ARM field");
        check(ascii.endsWith("\n"), "newline terminator");
        String status = Protocol.wrap("S,77,123456,32,1");
        Protocol.Status parsed = Protocol.parseStatus(status);
        check(parsed != null && parsed.sequence == 77 && parsed.deviceState == 1, "status parse");
        check(Protocol.parseStatus(status.replace(",32,", ",33,")) == null, "bad checksum rejected");
        check(Protocol.parseStatus(Protocol.wrap("S,99,12,10,3")) == null, "invalid state rejected");
        check(Protocol.parseStatus(Protocol.wrap("S,99,12,10,0")) == null, "reserved state rejected");
        check(state.snapshot().pwm[0] == 1500, "motor remains neutral before stick resumes");

        // Independent channel inversion, before applying trim.
        ControlState cal = new ControlState();
        cal.setLeft(0.4f, 0.6f);
        check(cal.snapshot().pwm[0] == 1800, "unmodified throttle");
        check(cal.snapshot().pwm[1] == 1700, "unmodified steer");
        cal.setInverted(0, true);
        check(cal.snapshot().pwm[0] == 1200, "throttle inversion");
        check(cal.snapshot().pwm[1] == 1700, "steering independently unaffected");
        cal.adjustTrim(0, 3);
        check(cal.getTrim(0) == 15, "three trim steps = 15 us");
        check(cal.snapshot().pwm[0] == 1215, "trim applied after inversion");
        cal.setInverted(1, true);
        cal.setTrim(1, -20);
        check(cal.snapshot().pwm[1] == 1280, "steering inverted and trimmed");
        cal.setLeft(0, 0);
        check(cal.snapshot().pwm[0] == 1515, "throttle trim shifts active center");
        cal.emergencyNeutral();
        check(cal.snapshot().pwm[0] == 1500, "pause overrides motor trim for safe ESC stop");
        cal.setLeft(0, 0);
        check(cal.snapshot().pwm[0] == 1515, "left stick restores active throttle trim");
        cal.setTrim(0, 500);
        check(cal.getTrim(0) == ControlState.MAX_TRIM_US, "trim clamped positive");
        cal.setTrim(0, -500);
        check(cal.getTrim(0) == -ControlState.MAX_TRIM_US, "trim clamped negative");
        cal.setInverted(0, false);
        cal.setLeft(0, -1f);
        check(cal.snapshot().pwm[0] == ControlState.MIN_PWM, "PWM clamped to 1000");
        cal.setLeft(0, 1f);
        cal.setTrim(0, 200);
        check(cal.snapshot().pwm[0] == ControlState.MAX_PWM, "PWM clamped to 2000");

        cal.setMode(0);
        cal.setInverted(4, true);
        cal.setTrim(4, 10);
        check(cal.snapshot().pwm[4] == 1910, "mode PWM reversed and trimmed");
        cal.setSmoothness(0f);
        cal.setInverted(5, true);
        check(cal.snapshot().pwm[5] == 2000, "smoothness reversed");
        cal.setTrim(5, -25);
        check(cal.snapshot().pwm[5] == 1975, "smoothness trim");
        check(cal.snapshot().trimUs[5] == -25 && cal.snapshot().inverted[4],
                "snapshot includes calibration for UI");

        byte[] adjustedPacket = Protocol.command(8, cal.snapshot());
        String adjusted = new String(adjustedPacket,
                java.nio.charset.StandardCharsets.US_ASCII);
        check(adjusted.startsWith("C,8,2000,1480,1500,1500,1910,1975*"),
                "calibration reflected in UART frame");
        System.out.println("OK: v2 control state / trim / inversion / UART protocol assertions");
    }
}
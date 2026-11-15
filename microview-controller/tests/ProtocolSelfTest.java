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
        System.out.println("OK: v2 control state / serial protocol assertions");
    }
}
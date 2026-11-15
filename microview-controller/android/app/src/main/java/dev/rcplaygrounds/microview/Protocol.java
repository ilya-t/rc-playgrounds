package dev.rcplaygrounds.microview;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Human-readable ASCII protocol with XOR checksum; see PROTOCOL.md. */
public final class Protocol {
    private Protocol() {}

    public static byte[] command(int seq, ControlState.Snapshot snapshot) {
        StringBuilder value = new StringBuilder(64);
        value.append("C,").append(seq & 0xffff);
        for (int pwm : snapshot.pwm) {
            value.append(',').append(pwm);
        }
        return wrap(value.toString()).getBytes(StandardCharsets.US_ASCII);
    }

    public static String wrap(String payload) {
        int checksum = 0;
        byte[] bytes = payload.getBytes(StandardCharsets.US_ASCII);
        for (byte b : bytes) checksum ^= b & 0xff;
        return payload + "*" + String.format(Locale.US, "%02X", checksum) + "\n";
    }

    public static Status parseStatus(String received) {
        if (received == null || received.length() > 95) return null;
        String line = received.trim();
        int star = line.lastIndexOf('*');
        if (star < 0 || line.length() != star + 3) return null;
        int actual;
        try {
            actual = Integer.parseInt(line.substring(star + 1), 16);
        } catch (NumberFormatException e) {
            return null;
        }
        int checksum = 0;
        for (int i = 0; i < star; i++) {
            if (line.charAt(i) > 127) return null;
            checksum ^= line.charAt(i);
        }
        if (checksum != actual) return null;
        String[] fields = line.substring(0, star).split(",", -1);
        if (fields.length != 5 || !fields[0].equals("S")) return null;
        try {
            int seq = Integer.parseInt(fields[1]);
            long uptime = Long.parseLong(fields[2]);
            int age = Integer.parseInt(fields[3]);
            int state = Integer.parseInt(fields[4]);
            if (seq < 0 || seq > 65535 || uptime < 0 || uptime > 4294967295L ||
                    age < 0 || age > 65535 || state != 1 && state != 2) return null;
            return new Status(seq, uptime, age, state);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static final class Status {
        public final int sequence;
        public final long uptimeMs;
        public final int commandAgeMs;
        /** 1=receiving, 2=failsafe. 0 is reserved. */
        public final int deviceState;

        public Status(int sequence, long uptimeMs, int commandAgeMs, int deviceState) {
            this.sequence = sequence;
            this.uptimeMs = uptimeMs;
            this.commandAgeMs = commandAgeMs;
            this.deviceState = deviceState;
        }
    }
}
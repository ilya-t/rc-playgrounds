package dev.rcplaygrounds.microview;

import android.os.SystemClock;
import android.hardware.usb.UsbDeviceConnection;

import com.hoho.android.usbserial.driver.UsbSerialPort;

import java.io.IOException;

/** Single IO thread, both TX and RX. USB permissions are handled by MainActivity. */
public final class SerialLink {
    public interface Listener {
        void onStatus(Protocol.Status status);
        void onClosed(String cause);
    }

    private final UsbDeviceConnection connection;
    private final UsbSerialPort port;
    private final ControlState controls;
    private final Listener listener;
    private final Thread worker;
    private volatile boolean stopRequested = false;
    private volatile boolean finished = false;
    private int sequence = 0;
    private final StringBuilder receiveLine = new StringBuilder();
    private boolean ignoreUntilNewline = false;

    public SerialLink(UsbDeviceConnection connection, UsbSerialPort port, ControlState controls, Listener listener) {
        this.connection = connection;
        this.port = port;
        this.controls = controls;
        this.listener = listener;
        this.worker = new Thread(this::ioLoop, "microview-uart");
    }

    public void start() {
        worker.start();
    }

    /** Non-blocking on UI thread. An explicit neutral packet is attempted before close. */
    public void shutdown() {
        controls.emergencyNeutral();
        stopRequested = true;
    }

    public boolean isFinished() {
        return finished;
    }

    private void ioLoop() {
        String reason = "Disconnected";
        try {
            port.open(connection);
            port.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            byte[] inputBuffer = new byte[128];
            long nextTransmissionMs = 0;
            while (true) {
                if (stopRequested) {
                    sendCommand();
                    break;
                }
                long now = SystemClock.elapsedRealtime();
                if (now >= nextTransmissionMs) {
                    sendCommand();
                    nextTransmissionMs = now + 33;
                }
                int count = port.read(inputBuffer, 20);
                for (int i = 0; i < count; ++i) {
                    appendIncoming((char) (inputBuffer[i] & 0xff));
                }
            }
        } catch (Exception e) {
            reason = e.getClass().getSimpleName() + ": " + e.getMessage();
        } finally {
            finished = true;
            controls.emergencyNeutral();
            try { port.close(); } catch (Exception ignored) {}
            connection.close();
            listener.onClosed(reason);
        }
    }

    private void sendCommand() throws IOException {
        byte[] data = Protocol.command(sequence++, controls.snapshot());
        port.write(data, 100);
    }

    private void appendIncoming(char value) {
        if (value == '\r') return;
        if (value == '\n') {
            if (!ignoreUntilNewline) {
                Protocol.Status status = Protocol.parseStatus(receiveLine.toString());
                if (status != null) listener.onStatus(status);
            }
            receiveLine.setLength(0);
            ignoreUntilNewline = false;
            return;
        }
        if (ignoreUntilNewline) return;
        if (value < 32 || value > 126 || receiveLine.length() >= 95) {
            receiveLine.setLength(0);
            ignoreUntilNewline = true;
            return;
        }
        receiveLine.append(value);
    }
}
package dev.rcplaygrounds.microview;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.util.List;
import java.util.Locale;

public final class MainActivity extends Activity {
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ControlState controls = new ControlState();
    private UsbManager usbManager;
    private SerialLink serialLink;
    private Protocol.Status lastStatus;
    private long lastStatusReceivedMs = 0;
    private ControlSurface surface;
    private TextView title;
    private Button usbButton;
    private String infoText = "USB отключён";

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            refreshUi();
            ui.postDelayed(this, 200);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        buildUi();
        handlePermissionIntent(getIntent());
        ui.post(tick);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handlePermissionIntent(intent);
    }

    private void handlePermissionIntent(Intent intent) {
        if (!intent.hasExtra(UsbPermissionReceiver.EXTRA_PERMISSION_RESULT)) return;
        boolean granted = intent.getBooleanExtra(UsbPermissionReceiver.EXTRA_PERMISSION_RESULT, false);
        intent.removeExtra(UsbPermissionReceiver.EXTRA_PERMISSION_RESULT);
        if (granted) connect();
        else { infoText = "Доступ к USB отклонён"; refreshUi(); }
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xff111a2c);
        int pad = dp(8);
        LinearLayout bar = new LinearLayout(this);
        bar.setPadding(pad, pad, pad, pad);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(bar, new LinearLayout.LayoutParams(-1, dp(64)));

        title = new TextView(this);
        title.setTextColor(Color.WHITE);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextSize(13);
        title.setText("RC MicroView · ожидание USB");
        bar.addView(title, new LinearLayout.LayoutParams(0, -2, 1));

        usbButton = new Button(this);
        usbButton.setText("USB CONNECT");
        usbButton.setTextSize(11);
        bar.addView(usbButton, new LinearLayout.LayoutParams(dp(150), dp(48)));
        usbButton.setOnClickListener(v -> {
            if (serialLink != null && !serialLink.isFinished()) disconnect();
            else connect();
        });

        surface = new ControlSurface(this, controls);
        root.addView(surface, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
        refreshUi();
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void connect() {
        if (serialLink != null && !serialLink.isFinished()) return;
        List<UsbSerialDriver> devices = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);
        if (devices.isEmpty()) {
            infoText = "Нет USB–UART. Проверьте OTG и адаптер";
            refreshUi();
            return;
        }
        // For multiple adapters, currently select the first detected one.
        UsbSerialDriver device = devices.get(0);
        UsbDevice usbDevice = device.getDevice();
        if (!usbManager.hasPermission(usbDevice)) {
            Intent requestIntent = new Intent(this, UsbPermissionReceiver.class)
                    .setAction(UsbPermissionReceiver.ACTION_PERMISSION);
            PendingIntent pending = PendingIntent.getBroadcast(this, 0, requestIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
            usbManager.requestPermission(usbDevice, pending);
            infoText = "Ожидание разрешения USB…";
            refreshUi();
            return;
        }
        UsbDeviceConnection connection = usbManager.openDevice(usbDevice);
        if (connection == null) {
            infoText = "Android не открыл USB порт";
            refreshUi();
            return;
        }
        if (device.getPorts().isEmpty()) {
            connection.close();
            infoText = "В адаптере нет serial-порта";
            refreshUi();
            return;
        }
        UsbSerialPort port = device.getPorts().get(0);
        lastStatus = null;
        lastStatusReceivedMs = 0;
        controls.emergencyNeutral();
        final SerialLink[] linkRef = new SerialLink[1];
        SerialLink link = new SerialLink(connection, port, controls, new SerialLink.Listener() {
            @Override public void onStatus(Protocol.Status status) {
                ui.post(() -> {
                    if (serialLink != linkRef[0]) return;
                    lastStatus = status;
                    lastStatusReceivedMs = SystemClock.elapsedRealtime();
                    refreshUi();
                });
            }
            @Override public void onClosed(String cause) {
                ui.post(() -> {
                    if (serialLink != linkRef[0]) return;
                    serialLink = null;
                    controls.emergencyNeutral();
                    lastStatus = null;
                    lastStatusReceivedMs = 0;
                    infoText = cause;
                    refreshUi();
                });
            }
        });
        linkRef[0] = link;
        serialLink = link;
        infoText = "Порт открыт. Ожидаю MicroView…";
        link.start();
        refreshUi();
    }

    private void disconnect() {
        controls.emergencyNeutral();
        if (serialLink != null) serialLink.shutdown();
        infoText = "Отключение…";
        refreshUi();
    }

    private boolean isFresh() {
        return serialLink != null && !serialLink.isFinished() &&
                lastStatus != null &&
                SystemClock.elapsedRealtime() - lastStatusReceivedMs < 1000 &&
                lastStatus.deviceState == 1;
    }

    private void refreshUi() {
        boolean linked = serialLink != null && !serialLink.isFinished();
        boolean fresh = isFresh();
        String status;
        if (fresh) {
            status = String.format(Locale.US, "MICROVIEW OK · seq=%d · rx=%dms",
                    lastStatus.sequence, lastStatus.commandAgeMs);
        } else if (linked) {
            status = "USB подключён · нет свежего heartbeat";
        } else {
            status = infoText;
        }
        title.setText(status);
        usbButton.setText(linked ? "DISCONNECT" : "USB CONNECT");
        surface.invalidate();
    }

    @Override
    protected void onPause() {
        // Lost focus, phone call, task switch: immediately re-center all axes.
        if (surface != null) surface.resetTouches();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        ui.removeCallbacks(tick);
        disconnect();
        super.onDestroy();
    }
}
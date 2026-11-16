package dev.rcplaygrounds.microview;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
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
    private Button trimToggleButton;
    private LinearLayout trimPanel;
    private final TextView[] trimValues = new TextView[ControlState.CHANNELS];
    private SharedPreferences calibration;
    private static final String CALIBRATION_PREFS = "channel_calibration_v1";
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
        calibration = getSharedPreferences(CALIBRATION_PREFS, MODE_PRIVATE);
        restoreCalibration();
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
        int pad = dp(6);
        LinearLayout bar = new LinearLayout(this);
        bar.setPadding(pad, pad, pad, pad);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(bar, new LinearLayout.LayoutParams(-1, dp(56)));

        title = new TextView(this);
        title.setTextColor(Color.WHITE);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextSize(12);
        title.setSingleLine(true);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        title.setText("RC MicroView · ожидание USB");
        bar.addView(title, new LinearLayout.LayoutParams(0, -2, 1));

        Button invertButton = new Button(this);
        invertButton.setText("INVERT");
        invertButton.setTextSize(11);
        bar.addView(invertButton, new LinearLayout.LayoutParams(dp(86), dp(44)));
        invertButton.setOnClickListener(v -> showInversionSettings());

        trimToggleButton = new Button(this);
        trimToggleButton.setTextSize(11);
        bar.addView(trimToggleButton, new LinearLayout.LayoutParams(dp(86), dp(44)));
        trimToggleButton.setOnClickListener(v -> {
            boolean visible = trimPanel.getVisibility() != View.VISIBLE;
            trimPanel.setVisibility(visible ? View.VISIBLE : View.GONE);
            calibration.edit().putBoolean("show_trims", visible).apply();
            updateTrimToggleLabel();
        });
        trimToggleButton.setOnLongClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Сбросить тримы всех каналов?")
                    .setMessage("Все смещения PWM вернутся к 0 мкс.")
                    .setNegativeButton("Отмена", null)
                    .setPositiveButton("Сбросить", (dialog, which) -> resetAllTrims())
                    .show();
            return true;
        });

        usbButton = new Button(this);
        usbButton.setText("USB CONNECT");
        usbButton.setTextSize(11);
        bar.addView(usbButton, new LinearLayout.LayoutParams(dp(135), dp(44)));
        usbButton.setOnClickListener(v -> {
            if (serialLink != null && !serialLink.isFinished()) disconnect();
            else connect();
        });

        surface = new ControlSurface(this, controls);
        root.addView(surface, new LinearLayout.LayoutParams(-1, 0, 1));

        trimPanel = new LinearLayout(this);
        trimPanel.setOrientation(LinearLayout.VERTICAL);
        trimPanel.setPadding(dp(4), dp(3), dp(4), dp(3));
        trimPanel.setBackgroundColor(0xff202d46);
        root.addView(trimPanel, new LinearLayout.LayoutParams(-1, -2));
        for (int row = 0; row < 2; row++) {
            LinearLayout strip = new LinearLayout(this);
            strip.setOrientation(LinearLayout.HORIZONTAL);
            trimPanel.addView(strip, new LinearLayout.LayoutParams(-1, dp(45)));
            for (int col = 0; col < 3; col++) {
                addTrimCell(strip, row * 3 + col);
            }
        }
        trimPanel.setVisibility(calibration.getBoolean("show_trims", true)
                ? View.VISIBLE : View.GONE);
        updateTrimToggleLabel();
        updateTrimValues();
        setContentView(root);
        refreshUi();
    }

    private void addTrimCell(LinearLayout parent, int channel) {
        LinearLayout cell = new LinearLayout(this);
        cell.setGravity(Gravity.CENTER_VERTICAL);
        cell.setOrientation(LinearLayout.HORIZONTAL);
        parent.addView(cell, new LinearLayout.LayoutParams(0, -1, 1));

        TextView label = new TextView(this);
        label.setText(ControlState.CHANNEL_LABELS[channel]);
        label.setTextSize(10);
        label.setSingleLine(true);
        label.setTextColor(0xffc6d4eb);
        cell.addView(label, new LinearLayout.LayoutParams(0, -2, 1));

        Button minus = new Button(this);
        minus.setText("−");
        minus.setTextSize(15);
        minus.setPadding(0, 0, 0, 0);
        minus.setMinWidth(0);
        minus.setMinimumWidth(0);
        cell.addView(minus, new LinearLayout.LayoutParams(dp(37), dp(40)));
        minus.setOnClickListener(v -> changeTrim(channel, -1));

        TextView value = new TextView(this);
        value.setGravity(Gravity.CENTER);
        value.setTextColor(0xff30dbc0);
        value.setTextSize(13);
        value.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        value.setSingleLine(true);
        cell.addView(value, new LinearLayout.LayoutParams(dp(48), dp(40)));
        trimValues[channel] = value;
        // Tap the number to zero that channel's trim; no additional menu required.
        value.setOnClickListener(v -> setTrim(channel, 0));

        Button plus = new Button(this);
        plus.setText("+");
        plus.setTextSize(15);
        plus.setPadding(0, 0, 0, 0);
        plus.setMinWidth(0);
        plus.setMinimumWidth(0);
        cell.addView(plus, new LinearLayout.LayoutParams(dp(37), dp(40)));
        plus.setOnClickListener(v -> changeTrim(channel, 1));
    }

    private void updateTrimToggleLabel() {
        trimToggleButton.setText(trimPanel.getVisibility() == View.VISIBLE
                ? "TRIMS ▲" : "TRIMS ▼");
    }

    private void updateTrimValues() {
        ControlState.Snapshot snapshot = controls.snapshot();
        for (int i = 0; i < ControlState.CHANNELS; i++) {
            if (trimValues[i] != null) {
                trimValues[i].setText(String.format(Locale.US, "%+d", snapshot.trimUs[i]));
            }
        }
        if (surface != null) surface.invalidate();
    }

    private void restoreCalibration() {
        for (int channel = 0; channel < ControlState.CHANNELS; channel++) {
            controls.setTrim(channel, calibration.getInt("trim_" + channel, 0));
            controls.setInverted(channel, calibration.getBoolean("invert_" + channel, false));
        }
    }

    private void changeTrim(int channel, int steps) {
        // A nonzero ESC trim is intentionally allowed, but it changes idle PWM.
        // Explain this once before activating it (not an ARM/DISARM control).
        if (channel == 0 && controls.getTrim(0) == 0 &&
                !calibration.getBoolean("gas_trim_warning_seen", false)) {
            new AlertDialog.Builder(this)
                    .setTitle("Трим газа изменит нейтраль ESC")
                    .setMessage("Смещение CH1 от 1500 мкс может привести к вращению колёс " +
                            "даже при отпущенном джойстике. Настраивайте с вывешенными колёсами.")
                    .setNegativeButton("Отмена", null)
                    .setPositiveButton("Продолжить", (dialog, which) -> {
                        calibration.edit().putBoolean("gas_trim_warning_seen", true).apply();
                        applyTrimStep(channel, steps);
                    })
                    .show();
            return;
        }
        applyTrimStep(channel, steps);
    }

    private void applyTrimStep(int channel, int steps) {
        int offset = controls.adjustTrim(channel, steps);
        calibration.edit().putInt("trim_" + channel, offset).apply();
        updateTrimValues();
    }

    private void setTrim(int channel, int offset) {
        controls.setTrim(channel, offset);
        calibration.edit().putInt("trim_" + channel, controls.getTrim(channel)).apply();
        updateTrimValues();
    }

    private void resetAllTrims() {
        SharedPreferences.Editor editor = calibration.edit();
        for (int i = 0; i < ControlState.CHANNELS; i++) {
            controls.setTrim(i, 0);
            editor.putInt("trim_" + i, 0);
        }
        editor.apply();
        updateTrimValues();
    }

    private void showInversionSettings() {
        surface.resetTouches();
        ControlState.Snapshot snapshot = controls.snapshot();
        // A checkmark means invert about the nominal 1500 us center; trim is added afterwards.
        String[] labels = new String[ControlState.CHANNELS];
        for (int i = 0; i < labels.length; i++) {
            labels[i] = ControlState.CHANNEL_LABELS[i];
        }
        new AlertDialog.Builder(this)
                .setTitle("Инверсия PWM-каналов")
                .setMultiChoiceItems(labels, snapshot.inverted, (dialog, channel, reverse) -> {
                    controls.setInverted(channel, reverse);
                    calibration.edit().putBoolean("invert_" + channel, reverse).apply();
                    updateTrimValues();
                })
                .setNeutralButton("Сбросить все", (dialog, which) -> {
                    SharedPreferences.Editor editor = calibration.edit();
                    for (int i = 0; i < ControlState.CHANNELS; i++) {
                        controls.setInverted(i, false);
                        editor.putBoolean("invert_" + i, false);
                    }
                    editor.apply();
                    updateTrimValues();
                })
                .setPositiveButton("Готово", null)
                .show();
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
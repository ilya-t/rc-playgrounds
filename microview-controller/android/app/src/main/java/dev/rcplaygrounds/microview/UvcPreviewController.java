package dev.rcplaygrounds.microview;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.SystemClock;
import com.jiangdg.ausbc.callback.IPreviewDataCallBack;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.view.TextureView;

import com.jiangdg.ausbc.MultiCameraClient;
import com.jiangdg.ausbc.callback.ICameraStateCallBack;
import com.jiangdg.ausbc.callback.IDeviceConnectCallBack;
import com.jiangdg.ausbc.camera.CameraUVC;
import com.jiangdg.ausbc.camera.bean.CameraRequest;
import com.jiangdg.usb.USBMonitor;

import java.util.ArrayList;
import java.util.List;

/**
 * USB-host UVC preview independent of the USB-UART MicroView connection.
 * Uses native UVC decoding straight to TextureView (no recording/encoding/audio).
 * All callbacks from AUSBC's client are marshaled to Android's main thread.
 */
public final class UvcPreviewController implements IDeviceConnectCallBack, ICameraStateCallBack {
    public interface Listener {
        void onVideoStatus(String value);
    }

    private final Context context;
    private final UsbManager usbManager;
    private final TextureView video;
    private final Listener listener;
    private final MultiCameraClient client;
    private UsbDevice selectedDevice;
    private CameraUVC camera;
    private CameraRequest currentRequest;
    private boolean started;
    private boolean enabled = true;
    private boolean requestInFlight;
    private boolean destroyed;
    private boolean useMjpeg = false;
    private int desiredWidth = 640;
    private int desiredHeight = 480;
    private String status = "Видеокамера не подключена";
    private volatile long decodedFrames;
    private volatile long renderedFrames;
    private volatile long lastFrameTimeMs;
    private long sampledDecoded, sampledRendered;
    private long sampleTimeMs = SystemClock.elapsedRealtime();
    private String deviceDetails = "no UVC device";
    private final IPreviewDataCallBack frameCallback = (data, width, height, format) -> {
        decodedFrames++;
        lastFrameTimeMs = SystemClock.elapsedRealtime();
    };

    public UvcPreviewController(Context context, TextureView video, Listener listener) {
        this.context = context;
        this.video = video;
        this.listener = listener;
        this.usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        this.client = new MultiCameraClient(context, this);
        video.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                if (camera != null && started && enabled) openPreview();
            }
            @Override public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int w, int h) {}
            @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                if (camera != null) camera.closeCamera();
                return true;
            }
            @Override public void onSurfaceTextureUpdated(SurfaceTexture surface) { renderedFrames++; }
        });
        updateStatus(status);
    }

    public void start() {
        if (destroyed || started) return;
        started = true;
        client.register();
        if (enabled) findCamera();
    }

    public void stop() {
        started = false;
        closeCamera();
        client.unRegister();
    }

    public void destroy() {
        if (destroyed) return;
        stop();
        destroyed = true;
        client.destroy();
    }

    public void setEnabled(boolean value) {
        enabled = value;
        video.setVisibility(value ? android.view.View.VISIBLE : android.view.View.GONE);
        if (!value) {
            closeCamera();
            updateStatus("VIDEO OFF");
        } else if (started) {
            findCamera();
        }
    }

    public boolean isEnabled() { return enabled; }

    public void setFormat(boolean mjpeg) {
        if (useMjpeg == mjpeg) return;
        useMjpeg = mjpeg;
        if (started && enabled) reconnect();
    }

    public boolean isMjpeg() { return useMjpeg; }

    public void setSize(int width, int height) {
        if (desiredWidth == width && desiredHeight == height) return;
        desiredWidth = width;
        desiredHeight = height;
        if (started && enabled) reconnect();
    }

    public void onCameraPermissionGranted() {
        if (started && enabled) findCamera();
    }

    private void findCamera() {
        if (destroyed || !started || !enabled || camera != null || requestInFlight) return;
        List<UsbDevice> devices = new ArrayList<>(usbManager.getDeviceList().values());
        for (UsbDevice device : devices) {
            if (isUvcDevice(device)) {
                selectedDevice = device;
                requestInFlight = true;
                updateStatus("UVC: запрашиваю доступ…");
                if (!client.requestPermission(device)) {
                    requestInFlight = false;
                    updateStatus("UVC: не удалось запросить USB-доступ");
                }
                return;
            }
        }
        updateStatus("UVC-камера не обнаружена");
    }

    static boolean isUvcDevice(UsbDevice device) {
        if (device == null) return false;
        if (device.getDeviceClass() == UsbConstants.USB_CLASS_VIDEO) return true;
        for (int i = 0; i < device.getInterfaceCount(); i++) {
            UsbInterface usbInterface = device.getInterface(i);
            if (usbInterface.getInterfaceClass() == UsbConstants.USB_CLASS_VIDEO) return true;
        }
        return false;
    }

    private void reconnect() {
        UsbDevice device = selectedDevice;
        closeCamera();
        if (device != null && started && enabled) {
            // Requesting permission again is safe; USBMonitor immediately reuses its grant.
            findCamera();
        }
    }

    private void closeCamera() {
        requestInFlight = false;
        if (camera != null) {
            camera.removePreviewDataCallBack(frameCallback);
            camera.setCameraStateCallBack(null);
            camera.closeCamera();
            camera = null;
        }
        selectedDevice = null;
    }

    @Override public void onAttachDev(UsbDevice device) {
        if (started && enabled && isUvcDevice(device)) findCamera();
    }

    @Override public void onDetachDec(UsbDevice device) {
        if (device != null && selectedDevice != null &&
                device.getDeviceId() == selectedDevice.getDeviceId()) {
            closeCamera();
            updateStatus("UVC отключена");
            if (started && enabled) findCamera();
        }
    }

    @Override public void onConnectDev(UsbDevice device, USBMonitor.UsbControlBlock block) {
        requestInFlight = false;
        if (!started || !enabled || !isUvcDevice(device) || block == null) return;
        if (selectedDevice != null && device.getDeviceId() != selectedDevice.getDeviceId()) return;
        if (camera != null) closeCamera();
        selectedDevice = device;
        camera = new CameraUVC(context, device);
        camera.setUsbControlBlock(block);
        camera.setCameraStateCallBack(this);
        camera.addPreviewDataCallBack(frameCallback);
        if (video.isAvailable()) {
            openPreview();
        } else {
            updateStatus("UVC: ожидание поверхности…");
        }
    }

    @Override public void onDisConnectDec(UsbDevice device, USBMonitor.UsbControlBlock block) {
        if (selectedDevice != null && device != null &&
                device.getDeviceId() == selectedDevice.getDeviceId()) {
            closeCamera();
            updateStatus("UVC: связь потеряна");
        }
    }

    @Override public void onCancelDev(UsbDevice device) {
        requestInFlight = false;
        updateStatus("UVC: разрешение отклонено");
    }

    private void openPreview() {
        if (camera == null || !video.isAvailable()) return;
        currentRequest = new CameraRequest.Builder()
                .setPreviewWidth(desiredWidth)
                .setPreviewHeight(desiredHeight)
                .setPreviewFormat(useMjpeg ? CameraRequest.PreviewFormat.FORMAT_MJPEG
                        : CameraRequest.PreviewFormat.FORMAT_YUYV)
                .setRawPreviewData(true)
                .setRenderMode(CameraRequest.RenderMode.NORMAL)
                .setAudioSource(CameraRequest.AudioSource.NONE)
                .create();
        updateStatus("UVC: запуск видео…");
        camera.openCamera(video, currentRequest);
    }

    @Override public void onCameraState(MultiCameraClient.ICamera self,
                                        ICameraStateCallBack.State state, String msg) {
        if (!started || !enabled || self != camera) return;
        if (state == ICameraStateCallBack.State.OPENED) {
            try {
                deviceDetails = "VID:PID=" + selectedDevice.getVendorId() + ":" + selectedDevice.getProductId()
                        + " modes=" + camera.getAllPreviewSizes().toString();
            } catch (Exception e) {
                deviceDetails = "modes unavailable: " + e.getMessage();
            }
            updateStatus("UVC LIVE • " + desiredWidth + "×" + desiredHeight
                    + (useMjpeg ? " MJPEG" : " YUYV"));
        } else if (state == ICameraStateCallBack.State.ERROR) {
            updateStatus("UVC ERROR: " + (msg == null ? "не удалось открыть камеру" : msg));
        } else {
            updateStatus("UVC остановлена");
        }
    }

    private void updateStatus(String value) {
        status = value;
        listener.onVideoStatus(value);
    }

    public String getStatus() { return status; }

    public String diagnostics() {
        long now = SystemClock.elapsedRealtime();
        long interval = Math.max(1, now - sampleTimeMs);
        long decodeFps = (decodedFrames - sampledDecoded) * 1000 / interval;
        long displayFps = (renderedFrames - sampledRendered) * 1000 / interval;
        sampledDecoded = decodedFrames;
        sampledRendered = renderedFrames;
        sampleTimeMs = now;
        long age = lastFrameTimeMs == 0 ? -1 : now - lastFrameTimeMs;
        return "RAW " + decodeFps + "fps (" + decodedFrames + ") DRAW "
                + displayFps + "fps (" + renderedFrames + ") age=" + age + "ms\n" + deviceDetails;
    }
}

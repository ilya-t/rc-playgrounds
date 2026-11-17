package dev.rcplaygrounds.microview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.jiangdg.ausbc.MultiCameraClient;
import com.jiangdg.ausbc.callback.ICameraStateCallBack;
import com.jiangdg.ausbc.callback.IDeviceConnectCallBack;
import com.jiangdg.ausbc.callback.IPreviewDataCallBack;
import com.jiangdg.ausbc.camera.CameraUVC;
import com.jiangdg.ausbc.camera.bean.CameraRequest;
import com.jiangdg.usb.USBMonitor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/** Two independent display paths: native SurfaceView and CPU NV21/RGBA to ImageView. */
public final class UvcPreviewController implements IDeviceConnectCallBack, ICameraStateCallBack {
    public interface Listener { void onVideoStatus(String status); }
    public enum Renderer { SURFACE, SOFTWARE }

    private final Context context;
    private final UsbManager usbManager;
    private final SurfaceView surface;
    private final ImageView softwareView;
    private final Listener listener;
    private final MultiCameraClient client;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final AtomicBoolean frameBusy = new AtomicBoolean(false);
    private final Object bitmapLock = new Object();
    private UsbDevice selectedDevice;
    private CameraUVC camera;
    private boolean started, enabled = true, requestInFlight, destroyed;
    private boolean useMjpeg;
    private int desiredWidth = 640, desiredHeight = 480;
    private Renderer renderer = Renderer.SURFACE;
    private volatile long rawFrames, drawnFrames, lastRawMs;
    private volatile String frameFormat = "?";
    private long sampledRaw, sampledDrawn, sampledAt = SystemClock.elapsedRealtime();
    private String modes = "unknown";
    private Bitmap bitmap;
    private int[] argb;
    private volatile boolean previewOpened;

    private final IPreviewDataCallBack callback = (data, width, height, format) -> {
        rawFrames++;
        lastRawMs = SystemClock.elapsedRealtime();
        frameFormat = String.valueOf(format);
        if (renderer != Renderer.SOFTWARE || !started || !enabled ||
                data == null || width <= 0 || height <= 0 || width > 1920 || height > 1080)
            return;
        // Keep the latest available frame; never queue up delayed FPV frames.
        if (!frameBusy.compareAndSet(false, true)) return;
        final byte[] copy = data.clone();
        final IPreviewDataCallBack.DataFormat kind = format;
        new Thread(() -> {
            try {
                Bitmap result;
                synchronized (bitmapLock) {
                    if (bitmap == null || bitmap.getWidth() != width || bitmap.getHeight() != height) {
                        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                        argb = new int[width * height];
                    }
                    if (kind == IPreviewDataCallBack.DataFormat.NV21) {
                        if (copy.length < width * height * 3 / 2) return;
                        nv21ToArgb(copy, width, height, argb);
                    } else if (kind == IPreviewDataCallBack.DataFormat.RGBA) {
                        if (copy.length < width * height * 4) return;
                        for (int i = 0, p = 0; i < width * height; i++, p += 4) {
                            argb[i] = Color.argb(copy[p + 3] & 255, copy[p] & 255,
                                    copy[p + 1] & 255, copy[p + 2] & 255);
                        }
                    } else return;
                    bitmap.setPixels(argb, 0, width, 0, 0, width, height);
                    // A new bitmap avoids a UI read/write race with the next frame.
                    result = bitmap.copy(Bitmap.Config.ARGB_8888, false);
                }
                main.post(() -> {
                    if (started && enabled && renderer == Renderer.SOFTWARE) {
                        softwareView.setImageBitmap(result);
                        drawnFrames++;
                    }
                    frameBusy.set(false);
                });
            } catch (RuntimeException ex) {
                frameBusy.set(false);
            }
        }, "uvc-cpu-frame").start();
    };

    public UvcPreviewController(Context context, FrameLayout host, Listener listener) {
        this.context = context;
        this.listener = listener;
        usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        surface = new SurfaceView(context);
        surface.setZOrderOnTop(false);
        surface.setZOrderMediaOverlay(false);
        host.addView(surface, new FrameLayout.LayoutParams(-1, -1));
        softwareView = new ImageView(context);
        softwareView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        softwareView.setBackgroundColor(Color.BLACK);
        host.addView(softwareView, new FrameLayout.LayoutParams(-1, -1));
        client = new MultiCameraClient(context, this);
        surface.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override public void surfaceCreated(SurfaceHolder holder) {
                if (started && enabled && camera != null && renderer == Renderer.SURFACE) openPreview();
            }
            @Override public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}
            @Override public void surfaceDestroyed(SurfaceHolder holder) {}
        });
        applyVisibility();
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
        if (enabled == value) return;
        enabled = value;
        applyVisibility();
        if (!enabled) {
            closeCamera();
            notifyStatus("VIDEO OFF");
        } else if (started) findCamera();
    }
    public boolean isEnabled() { return enabled; }
    public Renderer getRenderer() { return renderer; }
    public void setRenderer(Renderer next) {
        if (renderer == next) return;
        renderer = next;
        applyVisibility();
        reconnect();
    }
    public void setFormat(boolean mjpeg) {
        if (useMjpeg == mjpeg) return;
        useMjpeg = mjpeg;
        reconnect();
    }
    public void setSize(int width, int height) {
        if (desiredWidth == width && desiredHeight == height) return;
        desiredWidth = width;
        desiredHeight = height;
        reconnect();
    }
    public void onCameraPermissionGranted() { if (started && enabled) findCamera(); }
    private void applyVisibility() {
        // Keep native capture surface alive even when software output is selected.
        surface.setVisibility(enabled ? View.VISIBLE : View.GONE);
        softwareView.setVisibility(enabled && renderer == Renderer.SOFTWARE ? View.VISIBLE : View.GONE);
    }
    private void reconnect() {
        if (!started || !enabled) return;
        closeCamera();
        findCamera();
    }
    private void closeCamera() {
        requestInFlight = false;
        previewOpened = false;
        if (camera != null) {
            camera.removePreviewDataCallBack(callback);
            camera.setCameraStateCallBack(null);
            camera.closeCamera();
            camera = null;
        }
        selectedDevice = null;
    }
    private void findCamera() {
        if (destroyed || !started || !enabled || camera != null || requestInFlight) return;
        List<UsbDevice> devices = new ArrayList<>(usbManager.getDeviceList().values());
        for (UsbDevice device : devices) {
            if (!isUvc(device)) continue;
            selectedDevice = device;
            requestInFlight = true;
            notifyStatus("UVC: запрашиваю доступ");
            if (!client.requestPermission(device)) {
                requestInFlight = false;
                notifyStatus("UVC: USB permission failed");
            }
            return;
        }
        notifyStatus("UVC не обнаружена");
    }
    private static boolean isUvc(UsbDevice d) {
        if (d == null) return false;
        if (d.getDeviceClass() == UsbConstants.USB_CLASS_VIDEO) return true;
        for (int i = 0; i < d.getInterfaceCount(); i++) {
            UsbInterface inf = d.getInterface(i);
            if (inf.getInterfaceClass() == UsbConstants.USB_CLASS_VIDEO) return true;
        }
        return false;
    }
    @Override public void onAttachDev(UsbDevice d) { if (isUvc(d)) findCamera(); }
    @Override public void onDetachDec(UsbDevice d) {
        if (selectedDevice != null && d != null && d.getDeviceId() == selectedDevice.getDeviceId()) {
            closeCamera();
            notifyStatus("UVC disconnected");
        }
    }
    @Override public void onConnectDev(UsbDevice d, USBMonitor.UsbControlBlock block) {
        requestInFlight = false;
        if (!started || !enabled || block == null || !isUvc(d)) return;
        if (selectedDevice != null && d.getDeviceId() != selectedDevice.getDeviceId()) return;
        if (camera != null) closeCamera();
        selectedDevice = d;
        camera = new CameraUVC(context, d);
        camera.setUsbControlBlock(block);
        camera.setCameraStateCallBack(this);
        camera.addPreviewDataCallBack(callback);
        openPreview();
    }
    @Override public void onDisConnectDec(UsbDevice d, USBMonitor.UsbControlBlock block) {
        if (selectedDevice != null && d != null && d.getDeviceId() == selectedDevice.getDeviceId()) {
            closeCamera();
            notifyStatus("UVC lost");
        }
    }
    @Override public void onCancelDev(UsbDevice d) {
        requestInFlight = false;
        notifyStatus("UVC permission denied");
    }
    private void openPreview() {
        if (camera == null || previewOpened) return;
        if (renderer == Renderer.SURFACE && !surface.getHolder().getSurface().isValid()) return;
        previewOpened = true;
        CameraRequest request = new CameraRequest.Builder()
                .setPreviewWidth(desiredWidth)
                .setPreviewHeight(desiredHeight)
                .setPreviewFormat(useMjpeg ? CameraRequest.PreviewFormat.FORMAT_MJPEG :
                        CameraRequest.PreviewFormat.FORMAT_YUYV)
                .setRawPreviewData(true)
                .setRenderMode(CameraRequest.RenderMode.NORMAL)
                .setAudioSource(CameraRequest.AudioSource.NONE)
                .create();
        // The library supports SurfaceView natively. In software mode it still receives
        // raw frames; the SurfaceView is kept as the hidden capture surface.
        camera.openCamera(surface, request);
        notifyStatus("UVC starting " + renderer);
    }
    @Override public void onCameraState(MultiCameraClient.ICamera self,
                                        ICameraStateCallBack.State state, String msg) {
        if (self != camera || !started || !enabled) return;
        if (state == ICameraStateCallBack.State.OPENED) {
            try { modes = camera.getAllPreviewSizes(null).toString(); }
            catch (RuntimeException e) { modes = "modes: " + e.getMessage(); }
            notifyStatus("UVC LIVE " + renderer + " " + desiredWidth + "x" + desiredHeight +
                    (useMjpeg ? " MJPEG" : " YUYV"));
        } else if (state == ICameraStateCallBack.State.ERROR) {
            notifyStatus("UVC ERROR " + msg);
        }
    }
    private void notifyStatus(String s) { listener.onVideoStatus(s); }
    public String diagnostics() {
        long now = SystemClock.elapsedRealtime();
        long dt = Math.max(1, now - sampledAt);
        long raw = (rawFrames - sampledRaw) * 1000 / dt;
        long draw = (drawnFrames - sampledDrawn) * 1000 / dt;
        sampledRaw = rawFrames;
        sampledDrawn = drawnFrames;
        sampledAt = now;
        return renderer + " RAW " + raw + "fps DRAW " + draw + "fps age=" +
                (lastRawMs == 0 ? -1 : now - lastRawMs) + "ms " + frameFormat +
                "\nVID:PID " + (selectedDevice == null ? "none" :
                selectedDevice.getVendorId() + ":" + selectedDevice.getProductId()) +
                " modes: " + modes;
    }
    /** NV21 YUV420sp to ARGB; no JPEG re-encoding. */
    static void nv21ToArgb(byte[] yuv, int w, int h, int[] pixels) {
        int frameSize = w * h;
        for (int y = 0; y < h; y++) {
            int uvRow = frameSize + (y >> 1) * w;
            for (int x = 0; x < w; x++) {
                int yy = (yuv[y * w + x] & 255) - 16;
                int uv = uvRow + (x & ~1);
                int v = (yuv[uv] & 255) - 128;
                int u = (yuv[uv + 1] & 255) - 128;
                int c = Math.max(0, yy) * 298;
                int r = (c + 409 * v + 128) >> 8;
                int g = (c - 100 * u - 208 * v + 128) >> 8;
                int b = (c + 516 * u + 128) >> 8;
                pixels[y * w + x] = 0xff000000 | (Math.min(255, Math.max(0, r)) << 16)
                        | (Math.min(255, Math.max(0, g)) << 8)
                        | Math.min(255, Math.max(0, b));
            }
        }
    }
}

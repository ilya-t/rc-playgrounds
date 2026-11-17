package com.rc.playgrounds.server

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.herohan.uvcapp.CameraHelper
import com.herohan.uvcapp.ICameraHelper
import com.serenegiant.usb.Size
import com.serenegiant.usb.UVCCamera
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.abs

class AndroidServerCamera(
    private val activity: AppCompatActivity,
    private val preview: SurfaceView,
) : ServerCamera {
    private val states = MutableStateFlow(ServerCameraState.Stopped)
    override val state: Flow<ServerCameraState> = states

    private var cameraHelper: CameraHelper? = null
    private var started = false
    private var surfaceReady = false
    private var deviceChosen = false
    private var cameraOpened = false
    private var previewAttached = false
    private var viewWidth = 0
    private var viewHeight = 0
    private var receiverRegistered = false
    private var pendingDevice: UsbDevice? = null
    private var usbRequestInFlight = false
    private var askedForCameraPermission = false

    private val cameraPermissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (!started) {
            return@registerForActivityResult
        }
        if (!granted) {
            states.value = ServerCameraState.CameraDenied
            return@registerForActivityResult
        }
        pendingDevice?.let { openWhenReady(it) }
    }

    private val permissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != permissionAction()) {
                return
            }
            usbRequestInFlight = false
            val device = intent.usbDevice() ?: return
            val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
            if (!started) {
                return
            }
            if (granted) {
                cameraHelper?.selectDevice(device)
            } else {
                states.value = ServerCameraState.Denied
            }
        }
    }

    private val missingCheck = Runnable {
        if (started && pendingDevice == null) {
            states.value = ServerCameraState.Missing
        }
    }

    private val holderCallback = object : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
            publishSurface(preview.width, preview.height)
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            publishSurface(preview.width, preview.height)
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
            surfaceReady = false
            detachPreview()
        }
    }

    private val cameraCallback = object : ICameraHelper.StateCallback {
        override fun onAttach(device: UsbDevice) {
            if (!started || deviceChosen || !isVideoDevice(device)) {
                return
            }
            choose(device)
        }

        override fun onDeviceOpen(device: UsbDevice, isFirstOpen: Boolean) {
            if (!started) {
                return
            }
            cameraHelper?.openCamera()
        }

        override fun onCameraOpen(device: UsbDevice) {
            if (!started) {
                return
            }
            cameraOpened = true
            val size = previewSize(cameraHelper?.supportedSizeList)
            if (size != null) {
                Log.i(TAG, "preview ${size.width}x${size.height}@${size.fps} type=${size.type}")
                cameraHelper?.previewSize = size
            }
            attachPreview()
        }

        override fun onCameraClose(device: UsbDevice) {
            cameraOpened = false
            previewAttached = false
        }

        override fun onDeviceClose(device: UsbDevice) = Unit

        override fun onDetach(device: UsbDevice) {
            if (!started || pendingDevice?.deviceName != device.deviceName) {
                return
            }
            pendingDevice = null
            deviceChosen = false
            cameraOpened = false
            previewAttached = false
            states.value = ServerCameraState.Missing
        }

        override fun onCancel(device: UsbDevice) {
            if (!started) {
                return
            }
            states.value = ServerCameraState.Denied
        }

        override fun onError(device: UsbDevice, e: com.herohan.uvcapp.CameraException) {
            Log.e(TAG, "camera error ${e.code}", e)
            if (started) {
                states.value = ServerCameraState.Failed
            }
        }
    }

    init {
        preview.holder.addCallback(holderCallback)
        preview.viewTreeObserver.addOnWindowFocusChangeListener { hasFocus ->
            val device = pendingDevice
            if (hasFocus && started && device != null) {
                openWhenReady(device)
            }
        }
    }

    override fun start() {
        started = true
        states.value = ServerCameraState.Starting
        registerPermissionReceiver()
        val helper = CameraHelper()
        cameraHelper = helper
        helper.setStateCallback(cameraCallback)
        val connected = helper.deviceList.orEmpty().firstOrNull { isVideoDevice(it) }
        if (connected != null) {
            choose(connected)
        }
        preview.removeCallbacks(missingCheck)
        preview.postDelayed(missingCheck, MISSING_DELAY_MS)
    }

    override fun stop() {
        started = false
        preview.removeCallbacks(missingCheck)
        unregisterPermissionReceiver()
        detachPreview()
        cameraHelper?.release()
        cameraHelper = null
        deviceChosen = false
        cameraOpened = false
        pendingDevice = null
        states.value = ServerCameraState.Stopped
    }

    private fun choose(device: UsbDevice) {
        pendingDevice = device
        deviceChosen = true
        states.value = ServerCameraState.Starting
        Log.i(TAG, "usb ${device.deviceName} ${device.productName}")
        openWhenReady(device)
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun openWhenReady(device: UsbDevice) {
        if (!started) {
            return
        }
        if (!hasCameraPermission()) {
            if (!askedForCameraPermission) {
                askedForCameraPermission = true
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            return
        }
        val usbManager = activity.getSystemService(UsbManager::class.java)
        if (usbManager.hasPermission(device)) {
            cameraHelper?.selectDevice(device)
            return
        }
        if (!activity.hasWindowFocus() || usbRequestInFlight) {
            return
        }
        usbRequestInFlight = true
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        val pending = PendingIntent.getBroadcast(
            activity,
            0,
            Intent(permissionAction()).setPackage(activity.packageName),
            flags,
        )
        usbManager.requestPermission(device, pending)
    }

    private fun registerPermissionReceiver() {
        if (receiverRegistered) {
            return
        }
        val filter = IntentFilter(permissionAction())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.registerReceiver(permissionReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            activity.registerReceiver(permissionReceiver, filter)
        }
        receiverRegistered = true
    }

    private fun unregisterPermissionReceiver() {
        if (!receiverRegistered) {
            return
        }
        activity.unregisterReceiver(permissionReceiver)
        receiverRegistered = false
    }

    private fun permissionAction(): String = "${activity.packageName}.USB_PERMISSION"

    private fun Intent.usbDevice(): UsbDevice? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra(UsbManager.EXTRA_DEVICE)
        }
    }

    private fun publishSurface(width: Int, height: Int) {
        if (width > 0 && height > 0) {
            viewWidth = width
            viewHeight = height
        }
        surfaceReady = true
        if (started && cameraOpened) {
            attachPreview()
        }
    }

    private fun attachPreview() {
        val helper = cameraHelper ?: return
        if (!started || !surfaceReady || !cameraOpened || previewAttached) {
            return
        }
        if (!preview.holder.surface.isValid) {
            return
        }
        helper.addSurface(preview, false)
        helper.startPreview()
        previewAttached = true
        states.value = ServerCameraState.Previewing
    }

    private fun detachPreview() {
        if (!previewAttached) {
            return
        }
        cameraHelper?.removeSurface(preview)
        previewAttached = false
    }

    private fun isVideoDevice(device: UsbDevice): Boolean {
        if (device.deviceClass == UsbConstants.USB_CLASS_VIDEO) {
            return true
        }
        for (index in 0 until device.interfaceCount) {
            if (device.getInterface(index).interfaceClass == UsbConstants.USB_CLASS_VIDEO) {
                return true
            }
        }
        return false
    }

    private fun previewSize(sizes: List<Size>?): Size? {
        if (sizes.isNullOrEmpty()) {
            return null
        }
        val mjpeg = sizes.filter { it.type == UVCCamera.UVC_VS_FRAME_MJPEG }
        val preferred = if (mjpeg.isNotEmpty()) mjpeg else sizes
        val capped = preferred.filter { it.width <= MAX_PREVIEW_WIDTH && it.height <= MAX_PREVIEW_HEIGHT }
        val steady = capped.filter { it.fps == 0 || it.fps <= MAX_PREVIEW_FPS }
        val candidates = when {
            steady.isNotEmpty() -> steady
            capped.isNotEmpty() -> capped
            else -> preferred
        }
        val metrics = activity.resources.displayMetrics
        val width = viewWidth.takeIf { it > 0 } ?: metrics.widthPixels
        val height = viewHeight.takeIf { it > 0 } ?: metrics.heightPixels
        val viewRatio = width.toFloat() / height.coerceAtLeast(1)
        return candidates.minWith(
            compareBy<Size> { size -> abs(size.width.toFloat() / size.height.coerceAtLeast(1) - viewRatio) }
                .thenByDescending { size -> size.width.toLong() * size.height }
                .thenBy { size -> size.fps },
        )
    }

    private companion object {
        const val TAG = "ServerCamera"
        const val MISSING_DELAY_MS = 1500L
        const val MAX_PREVIEW_WIDTH = 1920
        const val MAX_PREVIEW_HEIGHT = 1080
        const val MAX_PREVIEW_FPS = 30
    }
}

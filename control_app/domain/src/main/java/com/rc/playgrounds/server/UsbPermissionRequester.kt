package com.rc.playgrounds.server

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

/**
 * Activity-side USB permission dialog. Connects to the app-scoped [UsbPermissionHandler].
 */
class UsbPermissionRequester(
    private val activity: AppCompatActivity,
) : UsbPermissionHandler.Requester {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var handler: UsbPermissionHandler? = null
    private var receiverRegistered = false
    private var requestInFlight = false
    private var awaiting: UsbDevice? = null
    private var onResult: ((Boolean) -> Unit)? = null

    private val permissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != permissionAction()) {
                return
            }
            requestInFlight = false
            val device = intent.usbDevice() ?: return
            if (awaiting?.deviceName != device.deviceName) {
                return
            }
            val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
            deliver(granted)
        }
    }

    private val retry = object : Runnable {
        override fun run() {
            askIfReady()
            if (awaiting != null && !requestInFlight) {
                mainHandler.postDelayed(this, RETRY_MS)
            }
        }
    }

    fun connect(handler: UsbPermissionHandler) {
        this.handler = handler
        register()
        handler.connect(this)
    }

    fun disconnect() {
        handler?.disconnect(this)
        handler = null
        mainHandler.removeCallbacks(retry)
        unregister()
        awaiting = null
        onResult = null
        requestInFlight = false
    }

    fun onResume() {
        if (awaiting == null || requestInFlight) {
            return
        }
        mainHandler.removeCallbacks(retry)
        mainHandler.post(retry)
    }

    override fun request(device: UsbDevice, onResult: (Boolean) -> Unit) {
        awaiting = device
        this.onResult = onResult
        requestInFlight = false
        mainHandler.removeCallbacks(retry)
        mainHandler.post(retry)
    }

    private fun askIfReady() {
        val device = awaiting ?: return
        if (requestInFlight || !activity.hasWindowFocus()) {
            return
        }
        val manager = activity.getSystemService(UsbManager::class.java) ?: return
        if (manager.hasPermission(device)) {
            deliver(true)
            return
        }
        requestInFlight = true
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        val pendingIntent = PendingIntent.getBroadcast(
            activity,
            REQUEST_CODE,
            Intent(permissionAction()).setPackage(activity.packageName),
            flags,
        )
        try {
            manager.requestPermission(device, pendingIntent)
        } catch (e: Exception) {
            requestInFlight = false
            Log.w(TAG, "permission", e)
        }
    }

    private fun deliver(granted: Boolean) {
        val callback = onResult ?: return
        onResult = null
        awaiting = null
        requestInFlight = false
        mainHandler.removeCallbacks(retry)
        callback(granted)
    }

    private fun register() {
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

    private fun unregister() {
        if (!receiverRegistered) {
            return
        }
        try {
            activity.unregisterReceiver(permissionReceiver)
        } catch (_: Exception) {
        }
        receiverRegistered = false
    }

    private fun permissionAction(): String = "${activity.packageName}.USB_DEVICE_PERMISSION"

    private fun Intent.usbDevice(): UsbDevice? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra(UsbManager.EXTRA_DEVICE)
        }
    }

    private companion object {
        const val TAG = "UsbPermission"
        const val REQUEST_CODE = 1
        const val RETRY_MS = 500L
    }
}

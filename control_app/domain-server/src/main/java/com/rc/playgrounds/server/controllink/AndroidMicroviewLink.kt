package com.rc.playgrounds.server.controllink

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.rc.playgrounds.server.ErrorLogger
import com.rc.playgrounds.server.ServerControlState
import com.rc.playgrounds.server.UsbPermissionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Sends the latest UDP control axes to a MicroView over USB UART.
 * Owned by the server process, not by an activity.
 */
class AndroidMicroviewLink(
    private val app: Application,
    controls: StateFlow<ServerControlState>,
    scope: CoroutineScope,
    private val usbPermissionHandler: UsbPermissionHandler,
    private val errors: ErrorLogger,
) {
    private val states = MutableStateFlow(MicroviewLinkState.NoAdapter)
    val state: StateFlow<MicroviewLinkState> = states
    private val statuses = MutableStateFlow<MicroviewProtocol.Status?>(null)
    val status: StateFlow<MicroviewProtocol.Status?> = statuses

    private val mainHandler = Handler(Looper.getMainLooper())
    private val running = AtomicBoolean(false)

    @Volatile private var latest = ServerControlState()
    @Volatile private var stopRequested = false
    @Volatile private var openDeviceName: String? = null

    private var receiversRegistered = false
    private var deniedDeviceName: String? = null
    private var permissionRequestDevice: String? = null

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    val device = intent.usbDevice()
                    if (device != null && device.deviceName == openDeviceName) {
                        stopRequested = true
                    }
                    if (device != null && device.deviceName == deniedDeviceName) {
                        deniedDeviceName = null
                    }
                }
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> scanAndOpen()
            }
        }
    }

    private val watch = object : Runnable {
        override fun run() {
            scanAndOpen()
            mainHandler.postDelayed(this, WATCH_MS)
        }
    }

    private val reopen = Runnable { scanAndOpen() }

    init {
        scope.launch {
            controls.collect { latest = it }
        }
        register()
        mainHandler.post(watch)
    }

    private fun scanAndOpen() {
        if (running.get()) {
            return
        }
        val manager = app.getSystemService(UsbManager::class.java) ?: return
        val driver = try {
            UsbSerialProber.getDefaultProber()
                .findAllDrivers(manager)
                .firstOrNull { !isVideoDevice(it.device) }
        } catch (e: Exception) {
            report("probe", e)
            null
        }
        if (driver == null) {
            states.value = MicroviewLinkState.NoAdapter
            return
        }
        val device = driver.device
        if (!manager.hasPermission(device)) {
            states.value = MicroviewLinkState.NoPermission
            if (deniedDeviceName != device.deviceName && permissionRequestDevice != device.deviceName) {
                permissionRequestDevice = device.deviceName
                usbPermissionHandler.request(device) { granted ->
                    if (permissionRequestDevice == device.deviceName) {
                        permissionRequestDevice = null
                    }
                    if (!granted) {
                        deniedDeviceName = device.deviceName
                        states.value = MicroviewLinkState.NoPermission
                        return@request
                    }
                    if (deniedDeviceName == device.deviceName) {
                        deniedDeviceName = null
                    }
                    scanAndOpen()
                }
            }
            return
        }
        open(manager, driver)
    }

    private fun open(manager: UsbManager, driver: UsbSerialDriver) {
        val device = driver.device
        if (!running.compareAndSet(false, true)) {
            return
        }
        val connection = try {
            manager.openDevice(device)
        } catch (e: SecurityException) {
            report("open", e)
            null
        }
        if (connection == null || driver.ports.isEmpty()) {
            running.set(false)
            connection?.close()
            states.value = if (connection == null) {
                MicroviewLinkState.NoPermission
            } else {
                MicroviewLinkState.NoAdapter
            }
            return
        }
        stopRequested = false
        openDeviceName = device.deviceName
        Thread({
            ioLoop(connection, driver.ports[0])
        }, THREAD_NAME).start()
    }

    private fun ioLoop(connection: UsbDeviceConnection, port: UsbSerialPort) {
        var opened = false
        var sequence = 0
        try {
            port.open(connection)
            port.setParameters(
                BAUD,
                UsbSerialPort.DATABITS_8,
                UsbSerialPort.STOPBITS_1,
                UsbSerialPort.PARITY_NONE,
            )
            opened = true
            states.value = MicroviewLinkState.Open
            val input = ByteArray(READ_BUFFER)
            val receiveLine = StringBuilder()
            var ignoreUntilNewline = false
            var nextTransmissionMs = 0L
            while (!stopRequested) {
                val now = SystemClock.elapsedRealtime()
                if (now >= nextTransmissionMs) {
                    port.write(MicroviewProtocol.command(sequence, latest), WRITE_TIMEOUT_MS)
                    sequence = (sequence + 1) and 0xffff
                    nextTransmissionMs = now + INTERVAL_MS
                }
                val count = port.read(input, READ_TIMEOUT_MS)
                for (index in 0 until count) {
                    ignoreUntilNewline = appendIncoming(
                        receiveLine,
                        input[index].toInt() and 0xff,
                        ignoreUntilNewline,
                    )
                }
            }
        } catch (e: Exception) {
            report("uart", e)
        } finally {
            if (opened) {
                try {
                    port.write(
                        MicroviewProtocol.command(sequence, ServerControlState()),
                        WRITE_TIMEOUT_MS,
                    )
                } catch (e: Exception) {
                    report("neutral", e)
                }
            }
            try {
                port.close()
            } catch (_: Exception) {
            }
            try {
                connection.close()
            } catch (_: Exception) {
            }
            openDeviceName = null
            running.set(false)
            statuses.value = null
            states.value = MicroviewLinkState.NoAdapter
            mainHandler.postDelayed(reopen, REOPEN_DELAY_MS)
        }
    }

    private fun appendIncoming(
        receiveLine: StringBuilder,
        value: Int,
        ignoreUntilNewline: Boolean,
    ): Boolean {
        val symbol = value.toChar()
        if (symbol == '\r') {
            return ignoreUntilNewline
        }
        if (symbol == '\n') {
            if (!ignoreUntilNewline) {
                val received = MicroviewProtocol.parseStatus(receiveLine.toString())
                if (received != null) {
                    statuses.value = received
                    val next = if (received.deviceState == DeviceState.FAILSAFE) {
                        MicroviewLinkState.Failsafe
                    } else {
                        MicroviewLinkState.Open
                    }
                    states.value = next
                }
            }
            receiveLine.setLength(0)
            return false
        }
        if (ignoreUntilNewline) {
            return true
        }
        if (symbol.code < 32 || symbol.code > 126 || receiveLine.length >= MAX_LINE) {
            receiveLine.setLength(0)
            return true
        }
        receiveLine.append(symbol)
        return false
    }

    private fun register() {
        if (receiversRegistered) {
            return
        }
        val usbFilter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            app.registerReceiver(usbReceiver, usbFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            app.registerReceiver(usbReceiver, usbFilter)
        }
        receiversRegistered = true
    }

    private fun Intent.usbDevice(): UsbDevice? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra(UsbManager.EXTRA_DEVICE)
        }
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

    private fun report(where: String, error: Exception) {
        errors.report("microview/$where", error)
    }

    private companion object {
        const val THREAD_NAME = "microview-uart"
        const val BAUD = 115200
        const val INTERVAL_MS = 33L
        const val WRITE_TIMEOUT_MS = 100
        const val READ_TIMEOUT_MS = 20
        const val READ_BUFFER = 128
        const val MAX_LINE = 95
        const val WATCH_MS = 1000L
        const val REOPEN_DELAY_MS = 1000L
    }
}

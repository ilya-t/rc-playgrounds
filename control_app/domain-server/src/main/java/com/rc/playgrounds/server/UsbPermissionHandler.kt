package com.rc.playgrounds.server

import android.hardware.usb.UsbDevice

/**
 * App-scoped USB permission requests.
 * An activity connects a [Requester] that can show the system dialog.
 */
class UsbPermissionHandler {
    fun interface Requester {
        fun request(device: UsbDevice, onResult: (Boolean) -> Unit)
    }

    private val gate = Any()
    private var requester: Requester? = null
    private val waiting = ArrayDeque<Pending>()
    private var inFlight: Pending? = null

    fun connect(requester: Requester) {
        val pending = synchronized(gate) {
            this.requester = requester
            if (inFlight == null) {
                inFlight = waiting.removeFirstOrNull()
            }
            inFlight
        }
        if (pending != null) {
            requester.request(pending.device) { granted -> finish(pending, granted) }
        }
    }

    fun disconnect(requester: Requester) {
        synchronized(gate) {
            if (this.requester === requester) {
                this.requester = null
            }
        }
    }

    fun request(device: UsbDevice, onResult: (Boolean) -> Unit) {
        val pending = Pending(device, onResult)
        val current = synchronized(gate) {
            val connected = requester
            if (connected == null || inFlight != null) {
                waiting.add(pending)
                null
            } else {
                inFlight = pending
                connected
            }
        } ?: return
        current.request(device) { granted -> finish(pending, granted) }
    }

    private fun finish(pending: Pending, granted: Boolean) {
        val next = synchronized(gate) {
            if (inFlight !== pending) {
                return
            }
            inFlight = null
            val queued = waiting.removeFirstOrNull() ?: return@synchronized null
            val connected = requester
            inFlight = queued
            if (connected == null) null else connected to queued
        }
        pending.onResult(granted)
        if (next != null) {
            next.first.request(next.second.device) { grantedNext ->
                finish(next.second, grantedNext)
            }
        }
    }

    private class Pending(
        val device: UsbDevice,
        val onResult: (Boolean) -> Unit,
    )
}

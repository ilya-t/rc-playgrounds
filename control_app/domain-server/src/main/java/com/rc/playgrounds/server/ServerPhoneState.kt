package com.rc.playgrounds.server

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class ServerPhoneState(
    private val activity: AppCompatActivity,
    private val cellular: ServerCellularMonitor,
) {
    private var asked = false
    private val launcher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        cellular.setPermissionGranted(granted)
    }

    fun onResume() {
        val granted = ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.READ_PHONE_STATE,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            cellular.setPermissionGranted(true)
            return
        }
        cellular.setPermissionGranted(false)
        if (!asked) {
            asked = true
            launcher.launch(Manifest.permission.READ_PHONE_STATE)
        }
    }
}

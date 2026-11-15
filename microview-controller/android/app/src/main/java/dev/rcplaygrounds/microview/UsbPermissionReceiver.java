package dev.rcplaygrounds.microview;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbManager;

/** Explicit receiver for a mutable USB permission PendingIntent (Android 14 safe). */
public final class UsbPermissionReceiver extends BroadcastReceiver {
    public static final String ACTION_PERMISSION =
            "dev.rcplaygrounds.microview.USB_PERMISSION";
    public static final String EXTRA_PERMISSION_RESULT = "permission_result";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!ACTION_PERMISSION.equals(intent.getAction())) return;
        boolean granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);
        Intent toActivity = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(EXTRA_PERMISSION_RESULT, granted);
        context.startActivity(toActivity);
    }
}
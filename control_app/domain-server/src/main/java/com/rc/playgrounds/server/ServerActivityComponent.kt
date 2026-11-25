package com.rc.playgrounds.server

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope

class ServerActivityComponent(
    private val appComponent: ServerAppComponent,
    private val activity: AppCompatActivity,
) {
    init {
        activity.setContentView(R.layout.server_activity)
        val receiver = appComponent.controlReceiver
        val camera = AndroidServerCamera(
            activity = activity,
            preview = activity.findViewById(R.id.server_preview),
        )
        val session = ServerSession(
            screenWake = AndroidScreenWake(activity),
            keepAlive = AndroidServerKeepAlive(activity),
            pictureInPicture = AndroidServerPictureInPicture(activity),
            camera = camera,
            videoSender = AndroidServerVideoSender(receiver.videoTarget, appComponent.scope),
        )
        val viewModelProvider = ServerViewModelProvider(
            camera,
            receiver.controls,
            receiver.videoTarget,
            appComponent.microviewLink.state,
            appComponent.errorLogger.errors,
        )
        ServerView(
            titleView = activity.findViewById(R.id.server_title),
            controlsView = activity.findViewById(R.id.server_controls),
            viewModel = viewModelProvider.viewModel,
            scope = activity.lifecycleScope,
        )
        val usbPermissionRequester = UsbPermissionRequester(activity)
        usbPermissionRequester.connect(appComponent.usbPermissionHandler)
        val phoneState = ServerPhoneState(activity, appComponent.cellularMonitor)
        session.start()
        activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                usbPermissionRequester.onResume()
                phoneState.onResume()
            }

            override fun onPause(owner: LifecycleOwner) {
                session.onUserLeave()
            }

            override fun onDestroy(owner: LifecycleOwner) {
                usbPermissionRequester.disconnect()
                if (!activity.isChangingConfigurations) {
                    session.onClosed()
                }
            }
        })
    }
}

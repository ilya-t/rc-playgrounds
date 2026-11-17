package com.rc.playgrounds.server

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.rc.playgrounds.domain.R

class ServerActivityComponent(
    private val appComponent: ServerAppComponent,
    private val activity: AppCompatActivity,
) {
    init {
        activity.setContentView(R.layout.server_activity)
        val configs = appComponent.activeConfigProvider
        val camera = AndroidServerCamera(
            activity = activity,
            preview = activity.findViewById(R.id.server_preview),
        )
        val session = ServerSession(
            screenWake = AndroidScreenWake(activity),
            keepAlive = AndroidServerKeepAlive(activity),
            pictureInPicture = AndroidServerPictureInPicture(activity),
            camera = camera,
            videoSender = AndroidServerVideoSender(configs.config, appComponent.scope),
        )
        val viewModelProvider = ServerViewModelProvider(camera, configs)
        ServerView(
            titleView = activity.findViewById(R.id.server_title),
            targetView = activity.findViewById(R.id.server_target),
            viewModel = viewModelProvider.viewModel,
            scope = activity.lifecycleScope,
        )
        session.start()
        activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onPause(owner: LifecycleOwner) {
                session.onUserLeave()
            }

            override fun onDestroy(owner: LifecycleOwner) {
                if (!activity.isChangingConfigurations) {
                    session.onClosed()
                }
            }
        })
    }
}

package com.rc.playgrounds

import android.view.SurfaceView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.rc.playgrounds.domain.R
import com.rc.playgrounds.server.AndroidScreenWake
import com.rc.playgrounds.server.AndroidServerCamera
import com.rc.playgrounds.server.AndroidServerKeepAlive
import com.rc.playgrounds.server.AndroidServerPictureInPicture
import com.rc.playgrounds.server.ServerSession
import com.rc.playgrounds.server.ServerView
import com.rc.playgrounds.server.ServerViewModelProvider

class ServerActivityComponent(
    private val appComponent: AppComponent,
    private val activity: AppCompatActivity,
) {
    init {
        activity.setContentView(R.layout.server_activity)
        val camera = AndroidServerCamera(
            activity = activity,
            preview = activity.findViewById(R.id.server_preview),
        )
        val session = ServerSession(
            screenWake = AndroidScreenWake(activity),
            keepAlive = AndroidServerKeepAlive(activity),
            pictureInPicture = AndroidServerPictureInPicture(activity),
            camera = camera,
        )
        val viewModelProvider = ServerViewModelProvider(camera)
        ServerView(
            titleView = activity.findViewById(R.id.server_title),
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

package com.rc.playgrounds

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.rc.playgrounds.domain.R
import com.rc.playgrounds.server.AndroidScreenWake
import com.rc.playgrounds.server.AndroidServerPictureInPicture
import com.rc.playgrounds.server.ServerSession

class ServerActivityComponent(
    private val appComponent: AppComponent,
    private val activity: AppCompatActivity,
) {
    init {
        activity.setContentView(R.layout.server_activity)
        val session = ServerSession(
            screenWake = AndroidScreenWake(activity),
            pictureInPicture = AndroidServerPictureInPicture(activity),
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

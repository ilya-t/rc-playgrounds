package com.rc.playgrounds

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.rc.playgrounds.domain.R
import com.rc.playgrounds.server.AndroidServerPictureInPicture
import com.rc.playgrounds.server.ServerSession

class ServerActivityComponent(
    private val appComponent: AppComponent,
    private val activity: AppCompatActivity,
) {
    init {
        activity.setContentView(R.layout.server_activity)
        val session = ServerSession(
            pictureInPicture = AndroidServerPictureInPicture(activity),
        )
        activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onPause(owner: LifecycleOwner) {
                session.onUserLeave()
            }
        })
    }
}

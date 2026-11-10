package com.rc.playgrounds

import androidx.appcompat.app.AppCompatActivity
import com.rc.playgrounds.domain.R

class ServerActivityComponent(
    private val appComponent: AppComponent,
    private val activity: AppCompatActivity,
) {
    init {
        activity.setContentView(R.layout.server_activity)
    }
}

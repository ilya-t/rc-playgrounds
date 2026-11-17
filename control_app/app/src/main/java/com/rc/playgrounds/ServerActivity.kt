package com.rc.playgrounds

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.rc.playgrounds.server.ServerActivityComponent
import com.rc.playgrounds.server.ServerAppComponent

class ServerActivity : AppCompatActivity() {
    private var activityComponent: ServerActivityComponent? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityComponent = ServerActivityComponent(ServerAppComponent.instance, this)
    }
}

package com.rc.playgrounds

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class ServerActivity : AppCompatActivity() {
    private lateinit var component: ServerActivityComponent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        component = ServerActivityComponent(AppComponent.instance, this)
    }
}

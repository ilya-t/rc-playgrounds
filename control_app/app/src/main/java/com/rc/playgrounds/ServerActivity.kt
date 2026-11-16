package com.rc.playgrounds

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class ServerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ServerActivityComponent(AppComponent.instance, this)
    }
}

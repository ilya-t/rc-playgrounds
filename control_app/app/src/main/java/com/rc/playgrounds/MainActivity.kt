package com.rc.playgrounds

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.util.UnstableApi
import com.rc.playgrounds.domain.R
import com.rc.playgrounds.stream.GStreamerReceiver
import com.rc.playgrounds.stream.StreamReceiver


class MainActivity : AppCompatActivity() {
    private lateinit var component: ActivityComponent

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        component = ActivityComponent(AppComponent.instance, this, ::createReceiver)
//        testSampleStream(playerView)
//        testRTSP(playerView)
//        testRtp(surfaceView)
//        RtpMediaDecoder.testRtpMediaDecoder(surfaceView)
//        testSocket()
//        testRTPReceiver(playerView)
//        testVLCReceiver()
//        testDefaultRenderersFactory(playerView)
//        testExo()
//        testExo_v2()
//        testVLCReceiver()
    //    testVLCReceiverV2()
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun createReceiver(): StreamReceiver {
//        return ExoReceiver(
//            activity,
//            playerView,
//        )
//        return VLCStreamReceiver(
//            activity,
//            surfaceView
//        )

        val surfaceContainer = findViewById<ViewGroup>(R.id.surface_container)
        val appComponent = AppComponent.instance
        return GStreamerReceiver(
            appComponent.streamerEvents,
            this,
            surfaceContainer,
            appComponent.configModel.configFlow.value.streamLocalCmd,
        )
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (component.onGenericMotionEvent(event)) {
            return true
        }
        return super.onGenericMotionEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (component.onKeyDown(keyCode, event)) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}

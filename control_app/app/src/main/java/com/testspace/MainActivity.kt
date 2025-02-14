package com.testspace

import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.testspace.core.ExperimentActivity
import rc.playgrounds.ActivityComponent
import rc.playgrounds.AppComponent


class MainActivity : ExperimentActivity() {
    override fun createExperiment() = CurrentExperiment(this)
    private lateinit var component: ActivityComponent

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        component = ActivityComponent(AppComponent.instance, this)
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

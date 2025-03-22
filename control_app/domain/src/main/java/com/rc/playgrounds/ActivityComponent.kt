package com.rc.playgrounds

import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.rc.playgrounds.config.Config
import com.rc.playgrounds.config.view.ConfigView
import com.rc.playgrounds.control.gamepad.GamepadEventEmitter
import com.rc.playgrounds.domain.R
import com.rc.playgrounds.navigation.NaiveNavigator
import com.rc.playgrounds.status.view.StatusView
import com.rc.playgrounds.stopwatch.StopwatchView
import com.rc.playgrounds.stream.StreamReceiver
import com.rc.playgrounds.stream.StreamingProcess
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ActivityComponent(
    private val appComponent: AppComponent,
    private val a: AppCompatActivity,
    private val streamReceiverFactory: (Config) -> StreamReceiver,
) {
    private val lifecycleScope = a.lifecycleScope

    init {
        a.setContentView(R.layout.experiment_activity)
    }
    private val resetButton = a.findViewById<View>(R.id.reset_button)
    private val saveButton = a.findViewById<Button>(R.id.save_button)
    private val backButton = a.findViewById<Button>(R.id.back_button)
    private val okButton = a.findViewById<Button>(R.id.ok_button)
    private val configureButton = a.findViewById<Button>(R.id.configure_button)
    private val configInput: AppCompatEditText = a.findViewById(R.id.config_input)
    private var streamingProcess: StreamingProcess? = null
    private val navigator = NaiveNavigator(a)
    private val configView = ConfigView(
        configInput = configInput,
        okButton = okButton,
        saveButton = saveButton,
        backButton = backButton,
        nextButton = a.findViewById<Button>(R.id.next_config_button),
        prevButton = a.findViewById<Button>(R.id.prev_config_button),
        configTitle = a.findViewById<AppCompatTextView>(R.id.config_name),
        scope = a.lifecycleScope,
        navigator = navigator,
        configModel = appComponent.configModel,
    )
    private val statusView = StatusView(
        textView = a.findViewById<TextView>(R.id.tv_output),
        text = appComponent.statusModel.text,
        scope = a.lifecycleScope,
    )
    private val stopwatchView = StopwatchView(
        model = appComponent.stopwatchModel,
        container = a.findViewById<ViewGroup>(R.id.stopwatch_container),
        stopwatchButton = a.findViewById<Button>(R.id.stopwatch_button),
        scope = lifecycleScope,
    )
    private var gamepadEventEmitter = GamepadEventEmitter(appComponent.gamepadEventStream)

    init {
        lifecycleScope.launch {
            appComponent.activeConfigProvider.configFlow.collect {
                doReset()
            }
        }

        resetButton.setOnClickListener {
            resetButton.isEnabled = false
            lifecycleScope.launch {
                doReset()
                resetButton.isEnabled = true
            }
        }

        a.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                release()
            }
        })

        configureButton.setOnClickListener {
            navigator.openConfig()
        }

        navigator.openMain()
    }

    private suspend fun doReset() {
        release()
        start()
        lifecycleScope.launch {
            // Give a small delay before stream restart on remote
            // so our stream receiver won't miss first frame.
            delay(500L)
            appComponent.streamCmdHash.invalidate()
        }.join()
    }

    private suspend fun start() {
        streamingProcess?.release()
        lifecycleScope.launch {
            val config = appComponent.activeConfigProvider.configFlow.first()
            streamingProcess = StreamingProcess(config, streamReceiverFactory)
            streamingProcess?.start()
            gamepadEventEmitter.restart()
        }.join()
    }

    private fun release() {
        streamingProcess?.release()
        streamingProcess = null
    }

    fun onGenericMotionEvent(event: MotionEvent): Boolean {
        return gamepadEventEmitter.onGenericMotionEvent(event)
    }

    fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return gamepadEventEmitter.onKeyDown(keyCode, event)
    }
}
package rc.playgrounds

import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.rc.playgrounds.AppComponent
import com.rc.playgrounds.config.ConfigView
import com.rc.playgrounds.control.gamepad.GamepadEventEmitter
import com.rc.playgrounds.domain.R
import com.rc.playgrounds.navigation.NaiveNavigator
import com.rc.playgrounds.status.view.StatusView
import com.rc.playgrounds.stopwatch.StopwatchView
import com.rc.playgrounds.stream.StreamReceiver
import com.testspace.core.Static
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import rc.playgrounds.stream.StreamingProcess

class ActivityComponent(
    private val appComponent: AppComponent,
    private val a: AppCompatActivity,
    private val streamReceiverFactory: () -> StreamReceiver,
) {
    private val resetButton = a.findViewById<View>(R.id.reset_button)
    private val saveButton = a.findViewById<Button>(R.id.save_button)
    private val backButton = a.findViewById<Button>(R.id.back_button)
    private val okButton = a.findViewById<Button>(R.id.ok_button)
    private val configureButton = a.findViewById<Button>(R.id.configure_button)
    private val configInput: AppCompatEditText = a.findViewById(R.id.config_input)
    private var streamingProcess: StreamingProcess? = null
    private val navigator = NaiveNavigator(a)
    private val configView = ConfigView(
        configInput,
        okButton,
        saveButton,
        backButton,
        appComponent.configModel,
        a.lifecycleScope,
        navigator,
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
        scope = a.lifecycleScope,
    )
    private var gamepadEventEmitter = GamepadEventEmitter(appComponent.gamepadEventStream)

    init {
        a.lifecycleScope.launch {
            appComponent.configModel.configFlow.collect {
                doReset()
            }
        }

        resetButton.setOnClickListener {
            doReset()
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

    private fun doReset() {
        release()
        start()
        a.lifecycleScope.launch {
            // Give a small delay before stream restart on remote
            // so our stream receiver won't miss first frame.
            delay(500L)
            appComponent.streamCmdHash.invalidate()
        }
    }

    private fun start() {
        streamingProcess?.release()
        streamingProcess = StreamingProcess(streamReceiverFactory)
        streamingProcess?.start()
        Static.output("Receiving stream!")

        gamepadEventEmitter.restart()
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
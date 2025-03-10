package rc.playgrounds

import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.testspace.R
import com.testspace.core.Static
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import rc.playgrounds.config.ConfigView
import rc.playgrounds.control.gamepad.GamepadEventEmitter
import rc.playgrounds.navigation.NaiveNavigator
import rc.playgrounds.stream.StreamingProcess

class ActivityComponent(
    private val appComponent: AppComponent,
    private val a: AppCompatActivity,
) {
    private val surfaceContainer = a.findViewById<ViewGroup>(R.id.surface_container)
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
        streamingProcess = StreamingProcess(
            appComponent.configModel,
            a,
            surfaceContainer,
            )
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
package rc.playgrounds

import android.net.Uri
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.media3.ui.PlayerView
import com.testspace.R
import com.testspace.core.Static
import kotlinx.coroutines.launch
import rc.playgrounds.config.ConfigView
import rc.playgrounds.telemetry.gamepad.GamepadEventEmitter
import rc.playgrounds.stream.StreamingProcess

class ActivityComponent(
    private val appComponent: AppComponent,
    private val a: AppCompatActivity,
) {
    private val playerView = a.findViewById<PlayerView>(R.id.player_view)
    private val surfaceView = a.findViewById<SurfaceView>(R.id.surface_view)
    private val textureView = a.findViewById<TextureView>(R.id.texture_view)
    private val resetButton = a.findViewById<View>(R.id.reset_button)
    private val saveButton = a.findViewById<Button>(R.id.save_button)
    private val configInput: AppCompatEditText = a.findViewById(R.id.config_input)
    private var streamingProcess: StreamingProcess? = null
    private val configView = ConfigView(
        configInput,
        saveButton,
        appComponent.configModel,
        a.lifecycleScope,
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
    }

    private fun doReset() {
        release()
        start()
    }

    private fun start() {
        val url: String = appComponent.configModel.configFlow.value.streamUrl ?: run {
            Static.output("No stream url passed!")
            android.util.Log.i("VLC", "No stream url passed!")
            return
        }
        streamingProcess?.release()
        streamingProcess = StreamingProcess(a, textureView, playerView, surfaceView)
        streamingProcess?.start(Uri.parse(url))
        Static.output("Receiving stream at: $url")

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
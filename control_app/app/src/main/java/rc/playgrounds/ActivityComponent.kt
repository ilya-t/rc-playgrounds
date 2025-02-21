package rc.playgrounds

import android.net.Uri
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.testspace.R
import com.testspace.core.Static
import kotlinx.coroutines.launch
import org.freedesktop.gstreamer.GStreamerSurfaceView
import rc.playgrounds.config.ConfigView
import rc.playgrounds.control.gamepad.GamepadEventEmitter
import rc.playgrounds.navigation.NaiveNavigator
import rc.playgrounds.stream.StreamingProcess

class ActivityComponent(
    private val appComponent: AppComponent,
    private val a: AppCompatActivity,
) {
//    private val playerView = a.findViewById<PlayerView>(R.id.player_view)
//    private val surfaceView = a.findViewById<SurfaceView>(R.id.surface_view)
    private val gSurfaceView = a.findViewById<GStreamerSurfaceView>(R.id.g_surface_view)
//    private val textureView = a.findViewById<TextureView>(R.id.texture_view)
    private val resetButton = a.findViewById<View>(R.id.reset_button)
    private val saveButton = a.findViewById<Button>(R.id.save_button)
    private val backButton = a.findViewById<Button>(R.id.back_button)
    private val configureButton = a.findViewById<Button>(R.id.configure_button)
    private val configInput: AppCompatEditText = a.findViewById(R.id.config_input)
    private var streamingProcess: StreamingProcess? = null
    private val navigator = NaiveNavigator(a)
    private val configView = ConfigView(
        configInput,
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
    }

    private fun start() {
        val url: String = appComponent.configModel.configFlow.value.streamUrl ?: run {
            Static.output("No stream url passed!")
            android.util.Log.i("VLC", "No stream url passed!")
            return
        }
        streamingProcess?.release()
        streamingProcess = StreamingProcess(
            appComponent.configModel,
            a,
//            textureView,
//            playerView,
//            surfaceView,
            gSurfaceView)
        streamingProcess?.start(Uri.parse(url))
        Static.output("Receiving stream at: $url")
            gSurfaceView,
            )

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
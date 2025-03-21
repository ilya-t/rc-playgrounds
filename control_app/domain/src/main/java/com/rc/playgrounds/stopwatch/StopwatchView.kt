package com.rc.playgrounds.stopwatch

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.ToggleButton
import com.rc.playgrounds.domain.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class StopwatchView(
    private val model: StopwatchModel,
    private val container: ViewGroup,
    private val stopwatchButton: Button,
    private val scope: CoroutineScope,
) {
    private var tvOutput: TextView? = null

    init {
        stopwatchButton.setOnClickListener {
            model.toggle()
        }
        scope.launch(Dispatchers.Main) {
            model.state.collect {
                if (it == null) {
                    hide()
                } else {
                    show(it)
                }
            }
        }

        scope.launch(Dispatchers.Main) {
            val resources = stopwatchButton.context.resources

            model.state.map { it != null }.distinctUntilChanged().collect { visible ->
                val drawable = if (visible) {
                    resources.getDrawable(R.drawable.outline_timer_off_24)
                } else {
                    resources.getDrawable(R.drawable.outline_timer_24)
                }
                stopwatchButton.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
            }
        }
    }

    private fun show(text: String) {
        if (container.childCount == 0) {
            doInflate()
        }
        container.visibility = View.VISIBLE
        tvOutput?.text = text
        (container.context as Activity).window.attributes.screenBrightness =
            WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
    }

    private fun doInflate() {
        LayoutInflater.from(container.context).inflate(R.layout.stopwatch, container)
        tvOutput = container.findViewById<TextView>(R.id.stopwatch_output)
    }

    private fun hide() {
        container.visibility = View.GONE
        (container.context as Activity).window.attributes.screenBrightness =
            WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
    }


}

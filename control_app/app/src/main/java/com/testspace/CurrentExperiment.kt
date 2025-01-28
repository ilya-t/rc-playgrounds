package com.testspace

import android.os.Handler
import androidx.annotation.LayoutRes
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.testspace.core.Experiment
import com.testspace.core.ExperimentActivity

/**
 * @see Experiment
 */
class CurrentExperiment(private val a: ExperimentActivity) : Experiment(a) {
    init {
        a.addTriggers(
                /* add triggers here */
        )


    }

    @LayoutRes
    override fun getExperimentLayout() = R.layout.basic_layout
}
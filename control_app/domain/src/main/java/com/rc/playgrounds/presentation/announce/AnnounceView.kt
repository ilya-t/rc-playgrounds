package com.rc.playgrounds.presentation.announce

import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.rc.playgrounds.domain.R
import com.rc.playgrounds.navigation.ActiveScreenProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class AnnounceView(
    private val activity: AppCompatActivity,
    private val model: AnnounceModel,
    private val scope: CoroutineScope,
    activeScreenProvider: ActiveScreenProvider,
) {
    private val announceText = activity.findViewById<TextView>(R.id.announce_text)
    init {
        scope.launch {
            model.viewModel.collect { viewModel ->
                when (viewModel) {
                    AnnounceViewModel.Hidden -> Unit // Do nothing. This is a job for OverlayView.
                    is AnnounceViewModel.Visible -> {
                        announceText.text = viewModel.title
                    }
                }
            }
        }
    }
}
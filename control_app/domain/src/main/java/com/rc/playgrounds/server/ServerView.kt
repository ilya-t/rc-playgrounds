package com.rc.playgrounds.server

import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ServerView(
    private val titleView: TextView,
    viewModel: Flow<ServerViewModel>,
    scope: CoroutineScope,
) {
    init {
        scope.launch(Dispatchers.Main) {
            viewModel.collect { model ->
                titleView.text = model.title
            }
        }
    }
}

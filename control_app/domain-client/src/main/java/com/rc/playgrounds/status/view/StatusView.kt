package com.rc.playgrounds.status.view

import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class StatusView(
    private val textView: TextView,
    private val text: Flow<String>,
    private val scope: CoroutineScope,
) {
    init {
        scope.launch(Dispatchers.Main) {
            text.collect {
                textView.text = it
            }
        }
    }
}

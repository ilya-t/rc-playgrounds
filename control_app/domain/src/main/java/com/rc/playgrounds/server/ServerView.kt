package com.rc.playgrounds.server

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ServerView(
    private val titleView: TextView,
    private val targetView: EditText,
    viewModel: Flow<ServerViewModel>,
    scope: CoroutineScope,
) {
    private var model: ServerViewModel? = null
    private var writing = false
    private var applyJob: Job? = null

    init {
        targetView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable?) {
                if (writing) {
                    return
                }
                applyJob?.cancel()
                applyJob = scope.launch(Dispatchers.Main) {
                    delay(APPLY_DELAY_MS)
                    model?.onTargetChanged(targetView.text?.toString().orEmpty())
                }
            }
        })
        targetView.setOnEditorActionListener { view, actionId, _ ->
            if (actionId != EditorInfo.IME_ACTION_DONE) {
                return@setOnEditorActionListener false
            }
            view.clearFocus()
            hideKeyboard(view)
            true
        }
        targetView.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                commitTarget()
            }
        }
        scope.launch(Dispatchers.Main) {
            viewModel.collect { next ->
                model = next
                titleView.text = next.title
                if (!targetView.isFocused) {
                    showTarget(next.target)
                }
            }
        }
    }

    private fun commitTarget() {
        applyJob?.cancel()
        val raw = targetView.text?.toString().orEmpty()
        model?.onTargetChanged(raw)
        val parsed = parseServerTarget(raw)
        if (parsed == null) {
            showTarget(model?.target)
            return
        }
        showTarget("${parsed.host}:${parsed.port}")
    }

    private fun showTarget(target: String?) {
        if (target == null || targetView.text?.toString() == target) {
            return
        }
        writing = true
        targetView.setText(target)
        writing = false
    }

    private fun hideKeyboard(view: TextView) {
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private companion object {
        const val APPLY_DELAY_MS = 400L
    }
}

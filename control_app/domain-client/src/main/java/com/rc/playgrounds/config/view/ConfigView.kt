package com.rc.playgrounds.config.view

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.view.isVisible
import com.rc.playgrounds.navigation.NaiveNavigator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ConfigView(
    private val configInput: AppCompatEditText,
    private val okButton: Button,
    private val saveButton: Button,
    private val backButton: Button,
    private val nextButton: Button,
    private val prevButton: Button,
    private val configTitle: TextView,
    private val saveErrorView: TextView,
    private val scope: CoroutineScope,
    private val navigator: NaiveNavigator,
    private val configModel: ConfigModel,
) {
    private var skipDraftUpdate = false

    init {
        scope.launch(Dispatchers.Default) {
            configModel.viewModel.collect { viewModel ->
                apply(viewModel)
            }
        }

        backButton.setOnClickListener {
            navigator.openMain()
            hideKeyboard(configInput)
        }

        configInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable?) {
                if (skipDraftUpdate) {
                    return
                }
                configModel.updateDraft(configInput.text.toString())
            }

        })
    }

    private suspend fun apply(viewModel: ConfigViewModel) {
        val rawText = withContext(Dispatchers.Main) { configInput.text.toString() }
        if (viewModel.rawJson != rawText) {
            withContext(Dispatchers.Main) {
                skipDraftUpdate = true
                configInput.updateTextKeepingCursor(viewModel.rawJson)
                skipDraftUpdate = false
            }
        }

        withContext(Dispatchers.Main) {
            configTitle.text = viewModel.title
            saveButton.isEnabled = viewModel.saveEnabled
            nextButton.isEnabled = viewModel.nextEnabled
            prevButton.isEnabled = viewModel.prevEnabled

            nextButton.setOnClickListener {
                viewModel.next()
            }

            prevButton.setOnClickListener {
                viewModel.prev()
            }

            saveButton.setOnClickListener {
                viewModel.saveBtn()
                saveButton.isEnabled = false
            }
            okButton.setOnClickListener {
                saveButton.isEnabled = false
                hideKeyboard(configInput)
                scope.launch {
                    if (viewModel.okBtn()) {
                        navigator.openMain()
                    }
                }
            }
        }

        withContext(Dispatchers.Main) {
            saveErrorView.isVisible = viewModel.saveError != null
            saveErrorView.text = viewModel.saveError
        }
    }

    private fun hideKeyboard(view: View) {
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(view.windowToken, 0)
    }
}

fun EditText.updateTextKeepingCursor(newText: String) {
    val oldText = this.text.toString()
    val selectionStart = selectionStart
    val selectionEnd = selectionEnd

    setText(newText)

    val newLength = newText.length

    // Try to keep the same relative cursor position
    val offset = newLength - oldText.length
    val adjustedStart = (selectionStart + offset).coerceIn(0, newLength)
    val adjustedEnd = (selectionEnd + offset).coerceIn(0, newLength)

    if (adjustedStart == adjustedEnd) {
        setSelection(adjustedStart)
    } else {
        setSelection(adjustedStart, adjustedEnd)
    }
}

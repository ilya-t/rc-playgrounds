package com.rc.playgrounds.config

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import androidx.appcompat.widget.AppCompatEditText
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
    private val configModel: ConfigModel,
    private val scope: CoroutineScope,
    private val navigator: NaiveNavigator,
    ) {
    init {
        scope.launch {
            configModel.configFlow.collect {
                if (it.rawJson != configInput.text.toString()) {
                    withContext(Dispatchers.Main) {
                        configInput.setText(it.rawJson)
                    }
                }
            }
        }

        saveButton.setOnClickListener {
            configModel.updateConfig(configInput.text.toString())
            saveButton.isEnabled = false
        }
        backButton.setOnClickListener {
            navigator.openMain()
            hideKeyboard(configInput)
        }
        okButton.setOnClickListener {
            configModel.updateConfig(configInput.text.toString())
            saveButton.isEnabled = false
            navigator.openMain()
            hideKeyboard(configInput)
        }

        configInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable?) {
                scope.launch {
//                    val unsaved = configModel.configFlow.value.rawJson != configInput.text.toString()
//                    withContext(Dispatchers.Main) {
//                        saveButton.isEnabled = unsaved
//                    }
                }
            }

        })
    }

    private fun hideKeyboard(view: View) {
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(view.windowToken, 0)
    }}
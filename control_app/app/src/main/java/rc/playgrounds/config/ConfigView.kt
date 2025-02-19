package rc.playgrounds.config

import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import androidx.appcompat.widget.AppCompatEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rc.playgrounds.navigation.NaiveNavigator

class ConfigView(
    private val configInput: AppCompatEditText,
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
        }

        configInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable?) {
                scope.launch {
                    val unsaved = configModel.configFlow.value.rawJson != configInput.text.toString()
                    withContext(Dispatchers.Main) {
                        saveButton.isEnabled = unsaved
                    }
                }
            }

        })
    }
}
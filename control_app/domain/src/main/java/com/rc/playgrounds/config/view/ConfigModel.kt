package com.rc.playgrounds.config.view

import com.rc.playgrounds.config.ConfigRepository
import com.rc.playgrounds.config.ConfigVersion
import com.rc.playgrounds.config.pickNextVersion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import org.json.JSONObject

class ConfigModel(
    private val configRepository: ConfigRepository,
    private val scope: CoroutineScope,
) {
    private val draftState = MutableStateFlow<DraftState?>(null)

    fun updateDraft(text: String) {
        draftState.value = DraftState.Unsaved(text)
    }

    private val _viewModel = MutableStateFlow<ConfigViewModel?>(null)
    val viewModel: Flow<ConfigViewModel> = _viewModel.filterNotNull()

    init {
        scope.launch {
            combine(
                draftState, configRepository.activeVersion, configRepository.allVersions,
                ::asViewModel
            ).collect {
                _viewModel.value = it
            }
        }
    }

    private fun asViewModel(
        draft: DraftState?,
        activeConfig: ConfigVersion,
        allVersions: List<String>
    ): ConfigViewModel {
        val unsaved = draft != null && draft.text != activeConfig.rawConfig
        val text: String = if (unsaved) {
            draft?.text.orEmpty()
        } else {
            activeConfig.rawConfig
        }
        val isLastConfig = allVersions.indexOf(activeConfig.version) == allVersions.lastIndex
        val isFirstConfig = allVersions.indexOf(activeConfig.version) == 0

        return ConfigViewModel(
            title = if (unsaved) {
                pickNextVersion(allVersions)+"?"
            } else {
                activeConfig.version
            },
            rawJson = runCatching { JSONObject(text).toString(2) }.getOrElse { text },
            saveEnabled = unsaved,
            nextEnabled = !isLastConfig,
            prevEnabled = !isFirstConfig,
            saveError = when (draft) {
                is DraftState.Unsaved -> null
                is DraftState.UnsavedWithErrors ->  draft.error.message
                null -> null
            },
            saveBtn = {
                scope.launch {
                    doSave(draft, unsaved, allVersions, text)
                }
            },
            okBtn = suspend {
                doSave(draft, unsaved, allVersions, text) == null
            },
            next = {
                if (unsaved) {
                    draftState.value = null
                    return@ConfigViewModel
                }
                val nextConfigIndex = allVersions.indexOf(activeConfig.version) + 1
                allVersions.getOrNull(nextConfigIndex)?.let {
                    configRepository.switchActive(it)
                }
            },
            prev = {
                if (unsaved) {
                    draftState.value = null
                    return@ConfigViewModel
                }
                val nextConfigIndex = allVersions.indexOf(activeConfig.version) - 1
                allVersions.getOrNull(nextConfigIndex)?.let {
                    configRepository.switchActive(it)
                }
            },
        )
    }

    private suspend fun doSave(draft: DraftState?, unsaved: Boolean, allVersions: List<String>, text: String): Throwable? {
        if (!unsaved) {
            return null
        }
        val newVersion = ConfigVersion(
            version = pickNextVersion(allVersions),
            rawConfig = text
        )

        return configRepository.storeConfig(newVersion).await()
            .onSuccess {
                draftState.value = null
                configRepository.switchActive(newVersion.version)
            }
            .onFailure { t ->
                draftState.value = DraftState.UnsavedWithErrors(
                    text = draft?.text.orEmpty(),
                    error = t,
                )
            }
            .exceptionOrNull()
    }
}
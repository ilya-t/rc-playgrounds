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

class ConfigModel(
    private val configRepository: ConfigRepository,
    private val scope: CoroutineScope,
) {
    private val draftState = MutableStateFlow<String?>(null)

    fun updateDraft(text: String) {
        draftState.value = text
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
        draft: String?,
        activeConfig: ConfigVersion,
        allVersions: List<String>
    ): ConfigViewModel {
        val unsaved = draft != null && draft != activeConfig.rawConfig
        val text: String = if (unsaved) {
            draft.orEmpty()
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
            rawJson = text,
            saveEnabled = unsaved,
            nextEnabled = !isLastConfig,
            prevEnabled = !isFirstConfig,
            save = {
                val newVersion = ConfigVersion(
                    version = pickNextVersion(allVersions),
                    rawConfig = text
                )
                configRepository.storeConfig(
                    newVersion
                )
                draftState.value = null
                configRepository.switchActive(newVersion.version)
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
}
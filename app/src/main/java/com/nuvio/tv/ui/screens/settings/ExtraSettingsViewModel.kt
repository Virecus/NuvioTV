package com.nuvio.tv.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nuvio.tv.data.local.ExtraSettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExtraSettingsViewModel @Inject constructor(
    private val dataStore: ExtraSettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExtraSettingsUiState())
    val uiState: StateFlow<ExtraSettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            dataStore.translateTraktCommentsToTurkish.collectLatest { enabled ->
                _uiState.update { it.copy(translateTraktCommentsToTurkish = enabled) }
            }
        }
        viewModelScope.launch {
            dataStore.liveTvEnabled.collectLatest { enabled ->
                _uiState.update { it.copy(liveTvEnabled = enabled) }
            }
        }
    }

    fun onEvent(event: ExtraSettingsEvent) {
        when (event) {
            is ExtraSettingsEvent.ToggleTranslateTraktCommentsToTurkish -> {
                viewModelScope.launch {
                    dataStore.setTranslateTraktCommentsToTurkish(event.enabled)
                }
            }
            is ExtraSettingsEvent.ToggleLiveTvEnabled -> {
                viewModelScope.launch {
                    dataStore.setLiveTvEnabled(event.enabled)
                }
            }
        }
    }
}

data class ExtraSettingsUiState(
    val translateTraktCommentsToTurkish: Boolean = false,
    val liveTvEnabled: Boolean = false
)

sealed class ExtraSettingsEvent {
    data class ToggleTranslateTraktCommentsToTurkish(val enabled: Boolean) : ExtraSettingsEvent()
    data class ToggleLiveTvEnabled(val enabled: Boolean) : ExtraSettingsEvent()
}

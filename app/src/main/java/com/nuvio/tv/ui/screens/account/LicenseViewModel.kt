package com.nuvio.tv.ui.screens.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nuvio.tv.core.license.LicenseManager
import com.nuvio.tv.core.license.LicenseStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LicenseUiState(
    val draftCode: String = "",
    val status: LicenseStatus = LicenseStatus.Loading,
    val isSubmitting: Boolean = false
)

@HiltViewModel
class LicenseViewModel @Inject constructor(
    private val licenseManager: LicenseManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LicenseUiState())
    val uiState: StateFlow<LicenseUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            licenseManager.savedLicenseCode.collect { savedCode ->
                _uiState.update { current ->
                    current.copy(draftCode = savedCode.orEmpty())
                }
            }
        }
        viewModelScope.launch {
            licenseManager.status.collect { status ->
                _uiState.update { current ->
                    current.copy(status = status, isSubmitting = false)
                }
            }
        }
    }

    fun updateDraftCode(value: String) {
        _uiState.update { it.copy(draftCode = value) }
    }

    fun submit() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            val status = licenseManager.submitLicenseCode(_uiState.value.draftCode)
            _uiState.update { it.copy(status = status, isSubmitting = false) }
        }
    }

    fun clearLicenseCode() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            licenseManager.clearLicenseCode()
            _uiState.update { it.copy(isSubmitting = false, draftCode = "") }
        }
    }
}

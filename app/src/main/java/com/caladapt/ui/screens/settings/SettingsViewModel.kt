package com.caladapt.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.caladapt.data.db.CalAdaptDatabase
import com.caladapt.data.repository.UserProfileRepository
import com.caladapt.domain.model.UnitSystem
import com.caladapt.domain.usecase.DataExportManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class SettingsState(
    val notificationsEnabled: Boolean = false,
    val isExporting: Boolean = false,
    val currentUnitSystem: UnitSystem = UnitSystem.METRIC,
    val dataCleared: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataExportManager: DataExportManager,
    private val userProfileRepository: UserProfileRepository,
    private val database: CalAdaptDatabase
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state = _state.asStateFlow()

    private val _exportEvent = MutableSharedFlow<List<File>>()
    val exportEvent = _exportEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            userProfileRepository.getProfile().collect { profile ->
                if (profile != null) {
                    _state.update { 
                        it.copy(currentUnitSystem = UnitSystem.valueOf(profile.unitSystem))
                    }
                }
            }
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        _state.update { it.copy(notificationsEnabled = enabled) }
    }

    fun toggleUnitSystem(isMetric: Boolean) {
        viewModelScope.launch {
            val profile = userProfileRepository.getProfileOnce() ?: return@launch
            val newUnit = if (isMetric) UnitSystem.METRIC else UnitSystem.IMPERIAL
            userProfileRepository.updateProfile(profile.copy(unitSystem = newUnit.name))
        }
    }

    fun exportData() {
        if (_state.value.isExporting) return
        _state.update { it.copy(isExporting = true) }
        viewModelScope.launch {
            try {
                val files = dataExportManager.exportDataToCsv()
                _exportEvent.emit(files)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _state.update { it.copy(isExporting = false) }
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                database.clearAllTables()
            }
            _state.update { it.copy(dataCleared = true) }
        }
    }
}

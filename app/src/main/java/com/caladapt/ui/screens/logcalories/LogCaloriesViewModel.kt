package com.caladapt.ui.screens.logcalories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.caladapt.data.db.entity.CalorieLogEntity
import com.caladapt.data.repository.CalorieRepository
import com.caladapt.data.repository.PhaseRepository
import com.caladapt.domain.usecase.LogCaloriesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class LogCaloriesState(
    val calories: String = "",
    val proteinG: String = "",
    val carbsG: String = "",
    val fatG: String = "",
    val selectedMealType: String = "Snack",
    val note: String = "",
    val todayLogs: List<CalorieLogEntity> = emptyList(),
    val todayTotal: Int = 0,
    val calorieTarget: Int = 0,
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false,
    val error: String? = null
) {
    val isValid: Boolean
        get() = calories.toIntOrNull()?.let { it > 0 } == true

    val caloriesRemaining: Int
        get() = calorieTarget - todayTotal

    val mealTypes: List<String> = listOf("Breakfast", "Lunch", "Dinner", "Snack")
}

@HiltViewModel
class LogCaloriesViewModel @Inject constructor(
    private val logCaloriesUseCase: LogCaloriesUseCase,
    private val calorieRepository: CalorieRepository,
    private val phaseRepository: PhaseRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LogCaloriesState())
    val state: StateFlow<LogCaloriesState> = _state.asStateFlow()

    init {
        observeTodayLogs()
        loadTarget()
    }

    private fun observeTodayLogs() {
        viewModelScope.launch {
            calorieRepository.getLogsForDateFlow(LocalDate.now()).collect { logs ->
                val total = logs.sumOf { it.calories }
                _state.update {
                    it.copy(todayLogs = logs, todayTotal = total)
                }
            }
        }
    }

    private fun loadTarget() {
        viewModelScope.launch {
            val phase = phaseRepository.getActivePhase()
            _state.update {
                it.copy(calorieTarget = phase?.dailyCalorieTarget ?: 0)
            }
        }
    }

    fun updateCalories(value: String) {
        if (value.isEmpty() || value.toIntOrNull() != null) {
            _state.update { it.copy(calories = value, savedSuccessfully = false) }
        }
    }

    fun updateProtein(value: String) {
        if (value.isEmpty() || value.toFloatOrNull() != null) {
            _state.update { it.copy(proteinG = value) }
        }
    }

    fun updateCarbs(value: String) {
        if (value.isEmpty() || value.toFloatOrNull() != null) {
            _state.update { it.copy(carbsG = value) }
        }
    }

    fun updateFat(value: String) {
        if (value.isEmpty() || value.toFloatOrNull() != null) {
            _state.update { it.copy(fatG = value) }
        }
    }

    fun updateMealType(mealType: String) {
        _state.update { it.copy(selectedMealType = mealType) }
    }

    fun updateNote(note: String) {
        _state.update { it.copy(note = note) }
    }

    fun logEntry() {
        val current = _state.value
        if (!current.isValid) return

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }

            try {
                logCaloriesUseCase(
                    calories = current.calories.toInt(),
                    proteinG = current.proteinG.toFloatOrNull() ?: 0f,
                    carbsG = current.carbsG.toFloatOrNull() ?: 0f,
                    fatG = current.fatG.toFloatOrNull() ?: 0f,
                    timeOfDay = current.selectedMealType,
                    note = current.note.trim()
                )

                // Reset form after successful save
                _state.update {
                    it.copy(
                        calories = "",
                        proteinG = "",
                        carbsG = "",
                        fatG = "",
                        note = "",
                        isSaving = false,
                        savedSuccessfully = true
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(isSaving = false, error = "Failed to save: ${e.message}")
                }
            }
        }
    }

    fun deleteEntry(entry: CalorieLogEntity) {
        viewModelScope.launch {
            try {
                calorieRepository.deleteEntry(entry)
            } catch (_: Exception) {
                // Silently handle
            }
        }
    }

    fun dismissSuccess() {
        _state.update { it.copy(savedSuccessfully = false) }
    }
}

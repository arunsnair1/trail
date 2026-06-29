package com.caladapt.ui.screens.logweight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.caladapt.data.db.entity.WeightLogEntity
import com.caladapt.data.repository.UserProfileRepository
import com.caladapt.data.repository.WeightRepository
import com.caladapt.domain.model.UnitSystem
import com.caladapt.domain.usecase.LogWeightUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class LogWeightState(
    val weightInput: String = "",
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val todayWeight: WeightLogEntity? = null,
    val weightHistory: List<WeightLogEntity> = emptyList(),
    val latestEMA: Float? = null,
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false,
    val isWaterRetention: Boolean = false,
    val error: String? = null
) {
    val isValid: Boolean
        get() = weightInput.toFloatOrNull()?.let { it > 0 } == true

    val weightLabel: String
        get() = unitSystem.weightLabel

    /** Display EMA in user's preferred unit */
    val latestEMADisplay: String
        get() {
            val ema = latestEMA ?: return "--"
            val displayVal = unitSystem.fromKg(ema)
            return "%.1f".format(displayVal)
        }
}

@HiltViewModel
class LogWeightViewModel @Inject constructor(
    private val logWeightUseCase: LogWeightUseCase,
    private val weightRepository: WeightRepository,
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LogWeightState())
    val state: StateFlow<LogWeightState> = _state.asStateFlow()

    init {
        loadProfile()
        observeWeightHistory()
        checkTodayWeight()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            val profile = userProfileRepository.getProfileOnce()
            val unitSystem = profile?.let {
                try { UnitSystem.valueOf(it.unitSystem) } catch (_: Exception) { UnitSystem.METRIC }
            } ?: UnitSystem.METRIC

            _state.update { it.copy(unitSystem = unitSystem) }
        }
    }

    private fun observeWeightHistory() {
        viewModelScope.launch {
            weightRepository.getAllWeightsFlow().collect { weights ->
                val lastEma = weights.lastOrNull()?.emaWeight
                _state.update {
                    it.copy(
                        weightHistory = weights.takeLast(30),
                        latestEMA = lastEma
                    )
                }
            }
        }
    }

    private fun checkTodayWeight() {
        viewModelScope.launch {
            val today = weightRepository.getWeightByDate(LocalDate.now())
            if (today != null) {
                val displayWeight = _state.value.unitSystem.fromKg(today.weightKg)
                _state.update {
                    it.copy(
                        todayWeight = today,
                        weightInput = "%.1f".format(displayWeight),
                        isWaterRetention = today.isCycleWaterRetention
                    )
                }
            }
        }
    }

    fun toggleWaterRetention() {
        val current = _state.value
        val newValue = !current.isWaterRetention
        _state.update { it.copy(isWaterRetention = newValue) }
        
        if (current.todayWeight != null) {
            viewModelScope.launch {
                weightRepository.toggleWaterRetention(LocalDate.now(), newValue)
                checkTodayWeight() // refresh
            }
        }
    }

    fun updateWeightInput(value: String) {
        if (value.isEmpty() || value.toFloatOrNull() != null || value == ".") {
            _state.update { it.copy(weightInput = value, savedSuccessfully = false) }
        }
    }

    fun nudgeWeight(delta: Float) {
        val current = _state.value.weightInput.toFloatOrNull() ?: return
        val newVal = (current + delta).coerceAtLeast(0.1f)
        _state.update { it.copy(weightInput = "%.1f".format(newVal)) }
    }

    fun toggleUnit() {
        val current = _state.value
        val newUnit = if (current.unitSystem == UnitSystem.METRIC) {
            UnitSystem.IMPERIAL
        } else {
            UnitSystem.METRIC
        }

        // Convert current input to new unit
        val currentVal = current.weightInput.toFloatOrNull()
        val newInput = if (currentVal != null) {
            // Convert: old unit → kg → new unit
            val kg = current.unitSystem.toKg(currentVal)
            "%.1f".format(newUnit.fromKg(kg))
        } else {
            ""
        }

        _state.update { it.copy(unitSystem = newUnit, weightInput = newInput) }
    }

    fun logWeight() {
        val current = _state.value
        if (!current.isValid) return

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }

            try {
                val displayWeight = current.weightInput.toFloat()
                val weightKg = current.unitSystem.toKg(displayWeight)

                val result = logWeightUseCase(
                    weightKg = weightKg,
                    isWaterRetention = current.isWaterRetention
                )

                _state.update {
                    it.copy(
                        todayWeight = result,
                        latestEMA = result.emaWeight,
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

    fun dismissSuccess() {
        _state.update { it.copy(savedSuccessfully = false) }
    }
}

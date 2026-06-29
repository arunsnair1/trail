package com.caladapt.ui.screens.onboarding

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.caladapt.data.db.entity.UserProfileEntity
import com.caladapt.data.repository.UserProfileRepository
import com.caladapt.domain.model.Sex
import com.caladapt.domain.model.UnitSystem
import com.caladapt.domain.usecase.TransitionPhaseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

data class OnboardingState(
    val currentStep: Int = 0,
    val name: String = "",
    val sex: Sex = Sex.MALE,
    val age: String = "",
    val heightValue: String = "",
    val weightValue: String = "",
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val isLoading: Boolean = false,
    val isComplete: Boolean = false,
    val error: String? = null
) {
    val totalSteps: Int = 4

    val isCurrentStepValid: Boolean
        get() = when (currentStep) {
            0 -> name.isNotBlank()
            1 -> true // sex always has a default
            2 -> {
                val ageVal = age.toIntOrNull()
                val heightVal = heightValue.toFloatOrNull()
                val weightVal = weightValue.toFloatOrNull()
                ageVal != null && ageVal in 13..120 &&
                        heightVal != null && heightVal > 0 &&
                        weightVal != null && weightVal > 0
            }
            3 -> true // summary step, always valid
            else -> false
        }
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userProfileRepository: UserProfileRepository,
    private val transitionPhaseUseCase: TransitionPhaseUseCase,
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    companion object {
        val ONBOARDING_COMPLETE_KEY = booleanPreferencesKey("onboarding_complete")
    }

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    fun updateName(name: String) {
        _state.update { it.copy(name = name) }
    }

    fun updateSex(sex: Sex) {
        _state.update { it.copy(sex = sex) }
    }

    fun updateAge(age: String) {
        // Only allow digits
        if (age.all { it.isDigit() } && age.length <= 3) {
            _state.update { it.copy(age = age) }
        }
    }

    fun updateHeight(height: String) {
        if (height.isEmpty() || height.toFloatOrNull() != null) {
            _state.update { it.copy(heightValue = height) }
        }
    }

    fun updateWeight(weight: String) {
        if (weight.isEmpty() || weight.toFloatOrNull() != null) {
            _state.update { it.copy(weightValue = weight) }
        }
    }

    fun updateUnitSystem(unitSystem: UnitSystem) {
        _state.update { it.copy(unitSystem = unitSystem) }
    }

    fun nextStep() {
        val current = _state.value
        if (current.currentStep < current.totalSteps - 1 && current.isCurrentStepValid) {
            _state.update { it.copy(currentStep = it.currentStep + 1) }
        }
    }

    fun previousStep() {
        if (_state.value.currentStep > 0) {
            _state.update { it.copy(currentStep = it.currentStep - 1) }
        }
    }

    /**
     * Complete onboarding: save profile, start Discovery phase, set flag.
     */
    fun completeOnboarding() {
        val current = _state.value
        if (!current.isCurrentStepValid) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                val unitSystem = current.unitSystem

                // Convert to internal units (always stored in kg/cm)
                val weightKg = unitSystem.toKg(current.weightValue.toFloat())
                val heightCm = unitSystem.toCm(current.heightValue.toFloat())
                val age = current.age.toInt()
                val isMale = current.sex == Sex.MALE

                // Save user profile
                val profile = UserProfileEntity(
                    name = current.name.trim(),
                    heightCm = heightCm,
                    age = age,
                    sex = current.sex.name,
                    unitSystem = current.unitSystem.name,
                    createdAt = Instant.now().toString()
                )
                userProfileRepository.saveProfile(profile)

                // Start Discovery phase
                transitionPhaseUseCase.startDiscovery(
                    weightKg = weightKg,
                    heightCm = heightCm,
                    age = age,
                    isMale = isMale
                )

                // Mark onboarding as complete
                dataStore.edit { prefs ->
                    prefs[ONBOARDING_COMPLETE_KEY] = true
                }

                _state.update { it.copy(isLoading = false, isComplete = true) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Something went wrong: ${e.message}"
                    )
                }
            }
        }
    }
}

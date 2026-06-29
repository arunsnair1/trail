package com.caladapt.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.caladapt.data.db.entity.PhaseConfigEntity
import com.caladapt.data.db.entity.WeightLogEntity
import com.caladapt.data.repository.CalorieRepository
import com.caladapt.data.repository.PhaseRepository
import com.caladapt.data.repository.UserProfileRepository
import com.caladapt.data.repository.WeightRepository
import com.caladapt.domain.model.Goal
import com.caladapt.domain.model.Phase
import com.caladapt.domain.model.UnitSystem
import com.caladapt.domain.usecase.DailyTargets
import com.caladapt.domain.usecase.GetDailyTargetsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class DashboardState(
    val dailyTargets: DailyTargets? = null,
    val recentWeights: List<WeightLogEntity> = emptyList(),
    val currentPhase: PhaseConfigEntity? = null,
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val userName: String = "",
    val tdeeResult: com.caladapt.domain.algorithm.TDEEResult? = null,
    val isLoading: Boolean = true,
    val error: String? = null
) {
    val phaseName: String
        get() = when {
            currentPhase == null -> "Not Started"
            currentPhase.phase == Phase.DISCOVERY.name -> "Discovery"
            currentPhase.phase == Phase.GOAL.name -> {
                when (currentPhase.goal) {
                    Goal.CUT.name -> "Cutting"
                    Goal.BULK.name -> "Bulking"
                    Goal.RECOMP.name -> "Recomposition"
                    else -> "Goal"
                }
            }
            currentPhase.phase == Phase.MAINTENANCE.name -> "Maintenance"
            else -> currentPhase.phase
        }

    val phaseDescription: String
        get() = when {
            currentPhase == null -> ""
            currentPhase.phase == Phase.DISCOVERY.name ->
                "Finding your true maintenance calories"
            currentPhase.phase == Phase.GOAL.name && currentPhase.goal == Goal.CUT.name ->
                "Fat loss phase — caloric deficit"
            currentPhase.phase == Phase.GOAL.name && currentPhase.goal == Goal.BULK.name ->
                "Muscle gain phase — caloric surplus"
            currentPhase.phase == Phase.GOAL.name && currentPhase.goal == Goal.RECOMP.name ->
                "Body recomposition — maintenance calories"
            currentPhase.phase == Phase.MAINTENANCE.name ->
                "Sustaining your results"
            else -> ""
        }
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getDailyTargetsUseCase: GetDailyTargetsUseCase,
    private val weightRepository: WeightRepository,
    private val phaseRepository: PhaseRepository,
    private val userProfileRepository: UserProfileRepository,
    private val calorieRepository: CalorieRepository,
    private val tdeeEngine: com.caladapt.domain.algorithm.TDEEEngine
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        loadDashboard()
        observeData()
    }

    private fun loadDashboard() {
        viewModelScope.launch {
            try {
                val profile = userProfileRepository.getProfileOnce()
                val unitSystem = profile?.let {
                    try { UnitSystem.valueOf(it.unitSystem) } catch (_: Exception) { UnitSystem.METRIC }
                } ?: UnitSystem.METRIC

                _state.update {
                    it.copy(
                        userName = profile?.name ?: "",
                        unitSystem = unitSystem,
                        isLoading = false
                    )
                }

                refreshTargets()
            } catch (e: Exception) {
                _state.update {
                    it.copy(isLoading = false, error = "Failed to load: ${e.message}")
                }
            }
        }
    }

    private fun observeData() {
        // Observe active phase changes
        viewModelScope.launch {
            phaseRepository.getActivePhaseFlow().collect { phase ->
                _state.update { it.copy(currentPhase = phase) }
            }
        }

        // Observe recent weight entries for the mini-chart
        viewModelScope.launch {
            weightRepository.getAllWeightsFlow().collect { weights ->
                _state.update { it.copy(recentWeights = weights.takeLast(30)) }
            }
        }

        // Observe today's calorie summary changes (reactive to new food logs)
        viewModelScope.launch {
            calorieRepository.getDailySummaryFlow(LocalDate.now()).collect {
                refreshTargets()
            }
        }
    }

    fun refreshTargets() {
        viewModelScope.launch {
            try {
                val targets = getDailyTargetsUseCase()
                
                // Compute TDEE Data Quality dynamically
                var result: com.caladapt.domain.algorithm.TDEEResult? = null
                val profile = userProfileRepository.getProfileOnce()
                if (profile != null) {
                    val allWeights = weightRepository.getAllWeights()
                    val today = LocalDate.now()
                    val summaries = calorieRepository.getSummariesBetween(today.minusDays(14), today)
                    val isMale = profile.sex == com.caladapt.domain.model.Sex.MALE.name
                    
                    if (allWeights.isNotEmpty()) {
                        val currentWeight = allWeights.last().emaWeight
                        result = tdeeEngine.calculateTDEEFromData(
                            weights = allWeights,
                            summaries = summaries,
                            userWeightKg = currentWeight,
                            userHeightCm = profile.heightCm,
                            userAge = profile.age,
                            isMale = isMale
                        )
                    }
                }
                
                _state.update { it.copy(dailyTargets = targets, tdeeResult = result) }
            } catch (_: Exception) {
                // Silently handle — targets stay null if no phase active
            }
        }
    }
}

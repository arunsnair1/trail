package com.caladapt.domain.usecase

import com.caladapt.data.repository.CalorieRepository
import com.caladapt.data.repository.PhaseRepository
import com.caladapt.domain.model.Goal
import com.caladapt.domain.algorithm.MacroCalculator
import com.caladapt.domain.algorithm.MacroTargets
import java.time.LocalDate
import javax.inject.Inject

/**
 * Use case to get today's calorie and macro targets
 * from the active phase configuration.
 */
class GetDailyTargetsUseCase @Inject constructor(
    private val phaseRepository: PhaseRepository,
    private val calorieRepository: CalorieRepository
) {
    /**
     * @return DailyTargets for the given date, or null if no active phase
     */
    suspend operator fun invoke(date: LocalDate = LocalDate.now()): DailyTargets? {
        val activePhase = phaseRepository.getActivePhase() ?: return null

        val summary = calorieRepository.getDailySummary(date)

        return DailyTargets(
            calorieTarget = activePhase.dailyCalorieTarget,
            proteinTargetG = activePhase.proteinTarget,
            carbsTargetG = activePhase.carbsTarget,
            fatTargetG = activePhase.fatTarget,
            caloriesConsumed = summary?.totalCalories ?: 0,
            proteinConsumedG = summary?.totalProtein ?: 0f,
            carbsConsumedG = summary?.totalCarbs ?: 0f,
            fatConsumedG = summary?.totalFat ?: 0f,
            tdeeEstimate = activePhase.tdeeEstimate,
            phase = activePhase.phase,
            goal = activePhase.goal
        )
    }
}

data class DailyTargets(
    val calorieTarget: Int,
    val proteinTargetG: Float,
    val carbsTargetG: Float,
    val fatTargetG: Float,
    val caloriesConsumed: Int,
    val proteinConsumedG: Float,
    val carbsConsumedG: Float,
    val fatConsumedG: Float,
    val tdeeEstimate: Float,
    val phase: String,
    val goal: String
) {
    val caloriesRemaining: Int get() = calorieTarget - caloriesConsumed
    val proteinRemainingG: Float get() = proteinTargetG - proteinConsumedG
    val carbsRemainingG: Float get() = carbsTargetG - carbsConsumedG
    val fatRemainingG: Float get() = fatTargetG - fatConsumedG

    val calorieProgress: Float get() =
        if (calorieTarget > 0) caloriesConsumed.toFloat() / calorieTarget else 0f
    val proteinProgress: Float get() =
        if (proteinTargetG > 0) proteinConsumedG / proteinTargetG else 0f
    val carbsProgress: Float get() =
        if (carbsTargetG > 0) carbsConsumedG / carbsTargetG else 0f
    val fatProgress: Float get() =
        if (fatTargetG > 0) fatConsumedG / fatTargetG else 0f
}

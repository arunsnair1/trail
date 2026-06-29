package com.caladapt.domain.algorithm

import com.caladapt.data.db.entity.PhaseConfigEntity
import com.caladapt.domain.model.Goal
import com.caladapt.domain.model.Phase
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * State machine managing transitions between the three phases:
 *   DISCOVERY → GOAL → MAINTENANCE
 *
 * Transition rules:
 *   DISCOVERY → GOAL:        Maintenance calories confirmed by MaintenanceDetector
 *   GOAL → MAINTENANCE:      User reaches target weight or chooses to stop
 *   MAINTENANCE → GOAL:      User sets a new goal
 *   Any → DISCOVERY:         User wants to re-discover maintenance (e.g., after long break)
 */
@Singleton
class PhaseManager @Inject constructor(
    private val tdeeEngine: TDEEEngine,
    private val calorieTargetEngine: CalorieTargetEngine,
    private val macroCalculator: MacroCalculator,
    private val safetyGuardrails: SafetyGuardrails,
    private val estimatedMaintenanceCalculator: EstimatedMaintenanceCalculator
) {

    /**
     * Create the initial Discovery phase configuration.
     * Called during onboarding.
     */
    fun createDiscoveryPhase(
        weightKg: Float,
        heightCm: Float,
        age: Int,
        isMale: Boolean
    ): PhaseConfigEntity {
        // Use Mifflin-St Jeor as starting estimate only
        val estimatedTDEE = estimatedMaintenanceCalculator.calculateEstimatedTDEE(weightKg, heightCm, age, isMale)
        val dailyCalories = estimatedTDEE.toInt()

        // Calculate macros for discovery (moderate protein)
        val macroResult = macroCalculator.calculateMacros(dailyCalories, weightKg, navyBodyFat = null, goal = null)
        val macros = (macroResult as? MacroResult.Success)?.targets ?: MacroTargets(0f, 0f, 0f, 0f, 0f, 0f)

        return PhaseConfigEntity(
            phase = Phase.DISCOVERY.name,
            goal = "",
            startingWeight = weightKg,
            targetWeight = null,
            dailyCalorieTarget = dailyCalories,
            proteinTarget = macros.proteinG,
            carbsTarget = macros.carbsG,
            fatTarget = macros.fatG,
            tdeeEstimate = estimatedTDEE,
            startedAt = Instant.now().toString()
        )
    }

    /**
     * Transition from Discovery to Goal phase.
     * Called when MaintenanceDetector confirms stability.
     *
     * @param confirmedTDEE The empirically confirmed TDEE
     * @param goal User's chosen goal
     * @param currentWeightKg User's current weight
     * @param targetWeightKg Target weight (optional)
     * @param heightCm User's height (for BMR safety floor)
     * @param age User's age
     * @param isMale User's sex
     */
    fun createGoalPhase(
        confirmedTDEE: Float,
        goal: Goal,
        currentWeightKg: Float,
        targetWeightKg: Float?,
        heightCm: Float,
        age: Int,
        isMale: Boolean
    ): PhaseConfigEntity {
        val rawCalories = calorieTargetEngine.calculateGoalCalories(confirmedTDEE, goal)

        // Apply safety guardrails
        val bmr = tdeeEngine.calculateBMR(currentWeightKg, heightCm, age, isMale)
        val safetyCheck = safetyGuardrails.validateCalorieTarget(
            rawCalories, confirmedTDEE, bmr, isMale
        )

        val dailyCalories = safetyCheck.adjustedCalories
        val macroResult = macroCalculator.calculateMacros(dailyCalories, currentWeightKg, navyBodyFat = null, goal = goal)
        val macros = (macroResult as? MacroResult.Success)?.targets ?: MacroTargets(0f, 0f, 0f, 0f, 0f, 0f)

        return PhaseConfigEntity(
            phase = Phase.GOAL.name,
            goal = goal.name,
            startingWeight = currentWeightKg,
            targetWeight = targetWeightKg,
            dailyCalorieTarget = dailyCalories,
            proteinTarget = macros.proteinG,
            carbsTarget = macros.carbsG,
            fatTarget = macros.fatG,
            tdeeEstimate = confirmedTDEE,
            startedAt = Instant.now().toString()
        )
    }

    /**
     * Transition to Maintenance phase.
     * Called when user reaches goal or wants to maintain.
     */
    fun createMaintenancePhase(
        currentTDEE: Float,
        currentWeightKg: Float
    ): PhaseConfigEntity {
        val dailyCalories = currentTDEE.toInt()
        val macroResult = macroCalculator.calculateMacros(dailyCalories, currentWeightKg, navyBodyFat = null, goal = null)
        val macros = (macroResult as? MacroResult.Success)?.targets ?: MacroTargets(0f, 0f, 0f, 0f, 0f, 0f)

        return PhaseConfigEntity(
            phase = Phase.MAINTENANCE.name,
            goal = "",
            startingWeight = currentWeightKg,
            targetWeight = null,
            dailyCalorieTarget = dailyCalories,
            proteinTarget = macros.proteinG,
            carbsTarget = macros.carbsG,
            fatTarget = macros.fatG,
            tdeeEstimate = currentTDEE,
            startedAt = Instant.now().toString()
        )
    }

    /**
     * Update targets after a weekly review.
     * Returns the updated values to be persisted.
     */
    fun computeUpdatedTargets(
        currentPhase: PhaseConfigEntity,
        updatedTDEE: Float,
        currentWeightKg: Float,
        newCalorieTarget: Int,
        heightCm: Float,
        age: Int,
        isMale: Boolean
    ): UpdatedTargets {
        val goal = if (currentPhase.goal.isNotEmpty()) {
            Goal.valueOf(currentPhase.goal)
        } else null

        // Apply safety guardrails
        val bmr = tdeeEngine.calculateBMR(currentWeightKg, heightCm, age, isMale)
        val safetyCheck = safetyGuardrails.validateCalorieTarget(
            newCalorieTarget, updatedTDEE, bmr, isMale
        )

        val safeCalories = safetyCheck.adjustedCalories
        val macroResult = macroCalculator.calculateMacros(safeCalories, currentWeightKg, navyBodyFat = null, goal = goal)
        val macros = (macroResult as? MacroResult.Success)?.targets ?: MacroTargets(0f, 0f, 0f, 0f, 0f, 0f)

        return UpdatedTargets(
            calories = safeCalories,
            protein = macros.proteinG,
            carbs = macros.carbsG,
            fat = macros.fatG,
            tdee = updatedTDEE,
            safetyWarnings = safetyCheck.warnings
        )
    }
}

data class UpdatedTargets(
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val tdee: Float,
    val safetyWarnings: List<String>
)

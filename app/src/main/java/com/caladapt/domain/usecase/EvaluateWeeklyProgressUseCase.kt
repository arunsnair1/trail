package com.caladapt.domain.usecase

import com.caladapt.data.db.entity.PhaseConfigEntity
import com.caladapt.data.repository.CalorieRepository
import com.caladapt.data.repository.PhaseRepository
import com.caladapt.data.repository.TDEERepository
import com.caladapt.data.repository.UserProfileRepository
import com.caladapt.data.repository.WeightRepository
import com.caladapt.domain.algorithm.*
import com.caladapt.domain.model.Goal
import com.caladapt.domain.model.Phase
import com.caladapt.domain.model.Sex
import java.time.LocalDate
import javax.inject.Inject

/**
 * Weekly evaluation use case — the "brain" that runs every 7 days.
 *
 * Performs:
 *   1. Recalculate TDEE from the last 7 days of data
 *   2. Compare actual vs target weight change
 *   3. Adjust calorie/macro targets
 *   4. Record TDEE history
 *   5. Check safety guardrails
 *   6. In Discovery: check if maintenance is found
 *   7. Return a comprehensive WeeklyReview for the UI
 */
class EvaluateWeeklyProgressUseCase @Inject constructor(
    private val weightRepository: WeightRepository,
    private val calorieRepository: CalorieRepository,
    private val phaseRepository: PhaseRepository,
    private val tdeeRepository: TDEERepository,
    private val userProfileRepository: UserProfileRepository,
    private val emaCalculator: EMACalculator,
    private val tdeeEngine: TDEEEngine,
    private val maintenanceDetector: MaintenanceDetector,
    private val calorieTargetEngine: CalorieTargetEngine,
    private val phaseManager: PhaseManager,
    private val safetyGuardrails: SafetyGuardrails,
    private val complianceCalculator: ComplianceCalculator,
    private val goalWeightDetector: GoalWeightDetector,
    private val dietBreakAdvisor: DietBreakAdvisor,
    private val reverseDietEngine: ReverseDietEngine
) {
    suspend operator fun invoke(): WeeklyReview? {
        val profile = userProfileRepository.getProfileOnce() ?: return null
        val activePhase = phaseRepository.getActivePhase() ?: return null

        val today = LocalDate.now()
        val weekAgo = today.minusDays(7)

        // Gather data
        val allWeights = weightRepository.getAllWeights()
        val recentWeights = weightRepository.getWeightsBetween(weekAgo, today)
        val avgCalories = calorieRepository.getAverageDailyCalories(weekAgo, today)
        
        val recentSummaries = calorieRepository.getSummariesBetween(weekAgo, today)
        val complianceResult = complianceCalculator.calculateCompliance(recentSummaries)
        val compliancePct = complianceResult.scorePercentage / 100f

        if (recentWeights.size < 3) {
            return WeeklyReview(
                status = ReviewStatus.INSUFFICIENT_DATA,
                message = "Need at least 3 days of weight data this week for analysis.",
                phase = activePhase.phase,
                currentCalorieTarget = activePhase.dailyCalorieTarget,
                newCalorieTarget = activePhase.dailyCalorieTarget,
                tdeeEstimate = activePhase.tdeeEstimate,
                avgCalories = avgCalories,
                weightChangeKgPerWeek = null,
                compliance = compliancePct,
                warnings = emptyList()
            )
        }
        
        if (!complianceResult.isCompliant) {
            return WeeklyReview(
                status = ReviewStatus.POOR_COMPLIANCE,
                message = "Your compliance was ${complianceResult.scorePercentage}%. Targets will not be adjusted until you log consistently for a week.",
                phase = activePhase.phase,
                currentCalorieTarget = activePhase.dailyCalorieTarget,
                newCalorieTarget = activePhase.dailyCalorieTarget,
                tdeeEstimate = activePhase.tdeeEstimate,
                avgCalories = avgCalories,
                weightChangeKgPerWeek = null,
                compliance = compliancePct,
                warnings = listOf("Need >80% compliance to update targets.")
            )
        }

        // Calculate weight trend
        val weightTrend = emaCalculator.calculateWeightTrendRate(allWeights) ?: 0f
        val currentWeight = recentWeights.last().emaWeight

        // Recalculate TDEE (Requires last 14 days minimum for the window)
        val allSummaries = calorieRepository.getSummariesBetween(today.minusDays(14), today)
        val isMale = profile.sex == Sex.MALE.name
        
        val tdeeResult = tdeeEngine.calculateTDEEFromData(
            weights = allWeights,
            summaries = allSummaries,
            userWeightKg = currentWeight,
            userHeightCm = profile.heightCm,
            userAge = profile.age,
            isMale = isMale
        )
        
        val updatedTDEE = if (tdeeResult is TDEEResult.Success) {
            tdeeResult.tdee
        } else if (tdeeResult is TDEEResult.BelowBMRWarning) {
            tdeeResult.computedTdee
        } else {
            activePhase.tdeeEstimate
        }

        val warnings = mutableListOf<String>()
        if (tdeeResult is TDEEResult.BelowBMRWarning) {
            warnings.add("Warning: Computed TDEE (${tdeeResult.computedTdee.toInt()}) is below your BMR (${tdeeResult.bmr.toInt()}).")
        } else if (tdeeResult is TDEEResult.InsufficientCompliance) {
            warnings.add("Need more consistent calorie logging to update TDEE accurately.")
        }

        // Phase-specific logic
        val newCalorieTarget: Int
        val reviewStatus: ReviewStatus

        when (Phase.valueOf(activePhase.phase)) {
            Phase.DISCOVERY -> {
                // Check if maintenance is found
                val stability = maintenanceDetector.checkStability(allWeights, avgCalories)

                if (stability.isStable) {
                    reviewStatus = ReviewStatus.MAINTENANCE_FOUND
                    newCalorieTarget = avgCalories.toInt()
                } else {
                    // Adjust calories toward maintenance
                    val adjustment = maintenanceDetector.getDiscoveryAdjustment(weightTrend)
                    newCalorieTarget = activePhase.dailyCalorieTarget + adjustment.calorieChange
                    reviewStatus = ReviewStatus.DISCOVERY_ADJUSTING
                    if (adjustment.calorieChange != 0) {
                        warnings.add(adjustment.reason)
                    }
                }
            }

            Phase.GOAL -> {
                if (activePhase.targetWeight != null && goalWeightDetector.hasReachedGoal(currentWeight, activePhase.targetWeight)) {
                    // Transition to Maintenance
                    val newPhase = phaseManager.createMaintenancePhase(
                        currentTDEE = updatedTDEE,
                        currentWeightKg = currentWeight
                    )
                    phaseRepository.startNewPhase(newPhase)
                    
                    reviewStatus = ReviewStatus.GOAL_REACHED
                    newCalorieTarget = newPhase.dailyCalorieTarget
                    warnings.add("Congratulations! You've reached your target weight. Moving to Maintenance.")
                } else {
                    val goal = Goal.valueOf(activePhase.goal)
                    val targetWeeklyChange = calorieTargetEngine.getTargetWeeklyChange(currentWeight, goal)
    
                    val adjustment = calorieTargetEngine.weeklyAdjustment(
                        currentTarget = activePhase.dailyCalorieTarget,
                        actualWeeklyChange = weightTrend,
                        targetWeeklyChange = targetWeeklyChange,
                        goal = goal
                    )
                    newCalorieTarget = adjustment.newTarget
                    reviewStatus = if (adjustment.change == 0) {
                        ReviewStatus.ON_TRACK
                    } else {
                        ReviewStatus.GOAL_ADJUSTING
                    }
                    warnings.add(adjustment.reason)
    
                    // Check weight change rate safety
                    val rateCheck = safetyGuardrails.checkWeightChangeRate(weightTrend, currentWeight)
                    warnings.addAll(rateCheck.warnings)
    
                    // Check for diet break suggestion
                    if (goal == Goal.CUT) {
                        val phaseStart = LocalDate.parse(activePhase.startedAt.substring(0, 10))
                        val daysInDeficit = java.time.temporal.ChronoUnit.DAYS.between(phaseStart, today).toInt()
                        
                        val dietBreak = dietBreakAdvisor.checkDietBreak(
                            daysInContinuousDeficit = daysInDeficit,
                            weightTrendKgPerWeek = weightTrend,
                            compliancePct = compliancePct
                        )
                        if (dietBreak.recommended) {
                            warnings.add("DIET BREAK ADVISORY: ${dietBreak.reason}")
                        }
                    }
                }
            }

            Phase.MAINTENANCE -> {
                // Check if we need to reverse diet up to TDEE
                if (activePhase.dailyCalorieTarget < updatedTDEE.toInt()) {
                    val reverseDiet = reverseDietEngine.calculateAdjustment(
                        currentTarget = activePhase.dailyCalorieTarget,
                        empiricalTdee = updatedTDEE.toInt(),
                        weightTrendKgPerWeek = weightTrend
                    )
                    newCalorieTarget = activePhase.dailyCalorieTarget + reverseDiet.change
                    reviewStatus = if (reverseDiet.change > 0) ReviewStatus.REVERSE_DIETING else ReviewStatus.ON_TRACK
                    if (reverseDiet.reason.isNotEmpty()) {
                        warnings.add(reverseDiet.reason)
                    }
                } else {
                    // Monitor maintenance stability
                    val stability = maintenanceDetector.checkStability(allWeights, avgCalories)
                    if (!stability.isStable && stability.weeklyRate != null) {
                        val adjustment = maintenanceDetector.getDiscoveryAdjustment(stability.weeklyRate)
                        newCalorieTarget = activePhase.dailyCalorieTarget + adjustment.calorieChange
                        reviewStatus = ReviewStatus.MAINTENANCE_ADJUSTING
                        if (adjustment.calorieChange != 0) warnings.add(adjustment.reason)
                    } else {
                        newCalorieTarget = activePhase.dailyCalorieTarget
                        reviewStatus = ReviewStatus.ON_TRACK
                    }
                }
            }
        }

        // Apply safety guardrails to new target
        val bmr = tdeeEngine.calculateBMR(currentWeight, profile.heightCm, profile.age, isMale)
        val safetyCheck = safetyGuardrails.validateCalorieTarget(newCalorieTarget, updatedTDEE, bmr, isMale)
        warnings.addAll(safetyCheck.warnings)

        val finalTarget = safetyCheck.adjustedCalories

        // Record TDEE history
        tdeeRepository.recordTDEE(
            date = today,
            tdee = updatedTDEE,
            emaWeight = currentWeight,
            avgCalories7d = avgCalories,
            weightChangeRate = weightTrend,
            phase = activePhase.phase
        )

        // Update active phase targets if changed
        if (finalTarget != activePhase.dailyCalorieTarget || updatedTDEE != activePhase.tdeeEstimate) {
            val goal = if (activePhase.goal.isNotEmpty()) Goal.valueOf(activePhase.goal) else null
            val updatedTargets = phaseManager.computeUpdatedTargets(
                currentPhase = activePhase,
                updatedTDEE = updatedTDEE,
                currentWeightKg = currentWeight,
                newCalorieTarget = finalTarget,
                heightCm = profile.heightCm,
                age = profile.age,
                isMale = isMale
            )

            phaseRepository.updateActiveTargets(
                calories = updatedTargets.calories,
                protein = updatedTargets.protein,
                carbs = updatedTargets.carbs,
                fat = updatedTargets.fat,
                tdee = updatedTargets.tdee
            )
        }

        return WeeklyReview(
            status = reviewStatus,
            message = buildReviewMessage(reviewStatus, weightTrend, updatedTDEE, compliancePct),
            phase = activePhase.phase,
            currentCalorieTarget = activePhase.dailyCalorieTarget,
            newCalorieTarget = finalTarget,
            tdeeEstimate = updatedTDEE,
            avgCalories = avgCalories,
            weightChangeKgPerWeek = weightTrend,
            compliance = compliancePct,
            warnings = warnings,
            currentWeightKg = currentWeight
        )
    }

    private fun buildReviewMessage(
        status: ReviewStatus,
        weightTrend: Float,
        tdee: Float,
        compliance: Float
    ): String {
        val trendDir = when {
            weightTrend < -0.05f -> "losing"
            weightTrend > 0.05f -> "gaining"
            else -> "maintaining"
        }
        val compliancePct = (compliance * 100).toInt()

        return when (status) {
            ReviewStatus.MAINTENANCE_FOUND ->
                "🎉 Maintenance calories found! Your TDEE is ~${tdee.toInt()} kcal/day. " +
                "You can now set a weight goal."
            ReviewStatus.DISCOVERY_ADJUSTING ->
                "Still discovering your maintenance. Currently $trendDir weight. " +
                "Targets adjusted. Keep logging consistently."
            ReviewStatus.ON_TRACK ->
                "Great job! You're on track. Compliance: $compliancePct%. Keep it up!"
            ReviewStatus.GOAL_ADJUSTING ->
                "Targets adjusted based on your progress. Compliance: $compliancePct%."
            ReviewStatus.MAINTENANCE_ADJUSTING ->
                "Small adjustment to maintain stability. Currently $trendDir slightly."
            ReviewStatus.INSUFFICIENT_DATA ->
                "Need more data. Try to log weight at least 4-5 times per week."
            ReviewStatus.POOR_COMPLIANCE ->
                "Compliance is too low ($compliancePct%). Logging consistently is required to adjust targets."
            ReviewStatus.GOAL_REACHED ->
                "Congratulations! You've reached your target weight. Your targets have been updated to Maintenance."
            ReviewStatus.REVERSE_DIETING ->
                "Safely reversing out of deficit. Stepping calories up towards maintenance."
        }
    }
}

enum class ReviewStatus {
    MAINTENANCE_FOUND,
    DISCOVERY_ADJUSTING,
    ON_TRACK,
    GOAL_ADJUSTING,
    MAINTENANCE_ADJUSTING,
    INSUFFICIENT_DATA,
    POOR_COMPLIANCE,
    GOAL_REACHED,
    REVERSE_DIETING
}

data class WeeklyReview(
    val status: ReviewStatus,
    val message: String,
    val phase: String,
    val currentCalorieTarget: Int,
    val newCalorieTarget: Int,
    val tdeeEstimate: Float,
    val avgCalories: Float,
    val weightChangeKgPerWeek: Float?,
    val compliance: Float,
    val warnings: List<String>,
    val currentWeightKg: Float? = null
)

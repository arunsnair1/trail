package com.caladapt.domain.algorithm

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Safety guardrails to prevent unhealthy calorie targets.
 *
 * Rules:
 *   1. Never go below estimated BMR
 *   2. Never exceed 25% deficit below TDEE
 *   3. Never exceed 20% surplus above TDEE
 *   4. Alert if losing >1.5% BW/week
 *   5. Alert if gaining >0.75% BW/week
 *   6. Absolute minimum: 1200 kcal (female) / 1500 kcal (male)
 */
@Singleton
class SafetyGuardrails @Inject constructor() {

    companion object {
        const val MAX_DEFICIT_PERCENTAGE = 0.25f  // 25% below TDEE
        const val MAX_SURPLUS_PERCENTAGE = 0.20f  // 20% above TDEE

        const val MAX_LOSS_RATE_PCT = 0.015f      // 1.5% body weight per week
        const val MAX_GAIN_RATE_PCT = 0.0075f     // 0.75% body weight per week

        const val ABSOLUTE_MIN_FEMALE = 1200
        const val ABSOLUTE_MIN_MALE = 1500
    }

    /**
     * Validate and clamp a calorie target within safe bounds.
     *
     * @param proposedCalories The calorie target to validate
     * @param tdee Current TDEE estimate
     * @param bmr Estimated BMR (safety floor)
     * @param isMale User's sex for absolute minimums
     * @return SafetyCheckResult with clamped value and any warnings
     */
    fun validateCalorieTarget(
        proposedCalories: Int,
        tdee: Float,
        bmr: Float,
        isMale: Boolean
    ): SafetyCheckResult {
        val warnings = mutableListOf<String>()
        var adjustedCalories = proposedCalories

        val absoluteMin = if (isMale) ABSOLUTE_MIN_MALE else ABSOLUTE_MIN_FEMALE
        val maxDeficit = (tdee * (1 - MAX_DEFICIT_PERCENTAGE)).toInt()
        val maxSurplus = (tdee * (1 + MAX_SURPLUS_PERCENTAGE)).toInt()
        val bmrFloor = bmr.toInt()

        // Check absolute minimum
        if (adjustedCalories < absoluteMin) {
            warnings.add("⚠️ Target was below absolute minimum ($absoluteMin kcal). Adjusted up.")
            adjustedCalories = absoluteMin
        }

        // Check BMR floor
        if (adjustedCalories < bmrFloor) {
            warnings.add("⚠️ Target was below estimated BMR ($bmrFloor kcal). Adjusted to BMR.")
            adjustedCalories = maxOf(adjustedCalories, bmrFloor)
        }

        // Check maximum deficit
        if (adjustedCalories < maxDeficit) {
            warnings.add("⚠️ Deficit exceeds 25% of TDEE. Capped at $maxDeficit kcal.")
            adjustedCalories = maxOf(adjustedCalories, maxDeficit)
        }

        // Check maximum surplus
        if (adjustedCalories > maxSurplus) {
            warnings.add("⚠️ Surplus exceeds 20% of TDEE. Capped at $maxSurplus kcal.")
            adjustedCalories = minOf(adjustedCalories, maxSurplus)
        }

        return SafetyCheckResult(
            originalCalories = proposedCalories,
            adjustedCalories = adjustedCalories,
            wasAdjusted = adjustedCalories != proposedCalories,
            warnings = warnings
        )
    }

    /**
     * Check if the current weight change rate is within safe bounds.
     *
     * @param weeklyChangeKg Weight change in kg/week (negative = losing)
     * @param currentWeightKg Current body weight in kg
     * @return RateCheckResult with safety status and warnings
     */
    fun checkWeightChangeRate(
        weeklyChangeKg: Float,
        currentWeightKg: Float
    ): RateCheckResult {
        val ratePct = kotlin.math.abs(weeklyChangeKg) / currentWeightKg
        val warnings = mutableListOf<String>()
        var severity = Severity.SAFE

        if (weeklyChangeKg < 0) {
            // Losing weight
            if (ratePct > MAX_LOSS_RATE_PCT) {
                warnings.add(
                    "🔴 Losing weight too fast (${"%.2f".format(weeklyChangeKg)} kg/wk = " +
                    "${"%.1f".format(ratePct * 100)}% BW/wk). Risk of muscle loss and metabolic adaptation."
                )
                severity = Severity.DANGER
            } else if (ratePct > MAX_LOSS_RATE_PCT * 0.8f) {
                warnings.add(
                    "🟡 Weight loss rate is high (${"%.2f".format(weeklyChangeKg)} kg/wk). " +
                    "Consider slowing down."
                )
                severity = Severity.WARNING
            }
        } else if (weeklyChangeKg > 0) {
            // Gaining weight
            if (ratePct > MAX_GAIN_RATE_PCT) {
                warnings.add(
                    "🔴 Gaining weight too fast (${"%.2f".format(weeklyChangeKg)} kg/wk = " +
                    "${"%.1f".format(ratePct * 100)}% BW/wk). Likely gaining excess fat."
                )
                severity = Severity.DANGER
            } else if (ratePct > MAX_GAIN_RATE_PCT * 0.8f) {
                warnings.add(
                    "🟡 Weight gain rate is high (${"%.2f".format(weeklyChangeKg)} kg/wk). " +
                    "Consider a leaner surplus."
                )
                severity = Severity.WARNING
            }
        }

        return RateCheckResult(
            severity = severity,
            weeklyChangeKg = weeklyChangeKg,
            percentageOfBW = ratePct * 100,
            warnings = warnings
        )
    }

    /**
     * Suggest a diet break based on continuous deficit duration.
     */
    fun shouldSuggestDietBreak(
        daysInContinuousDeficit: Int
    ): DietBreakSuggestion? {
        return when {
            daysInContinuousDeficit >= 84 -> DietBreakSuggestion(  // 12+ weeks
                urgency = Severity.DANGER,
                suggestedDays = 14,
                reason = "You've been in a deficit for ${daysInContinuousDeficit / 7} weeks. " +
                         "A 2-week diet break at maintenance is strongly recommended to " +
                         "mitigate metabolic adaptation and improve adherence."
            )
            daysInContinuousDeficit >= 42 -> DietBreakSuggestion(  // 6+ weeks
                urgency = Severity.WARNING,
                suggestedDays = 7,
                reason = "You've been in a deficit for ${daysInContinuousDeficit / 7} weeks. " +
                         "Consider a 1-week diet break at maintenance to support recovery."
            )
            else -> null
        }
    }
}

data class SafetyCheckResult(
    val originalCalories: Int,
    val adjustedCalories: Int,
    val wasAdjusted: Boolean,
    val warnings: List<String>
)

data class RateCheckResult(
    val severity: Severity,
    val weeklyChangeKg: Float,
    val percentageOfBW: Float,
    val warnings: List<String>
)

data class DietBreakSuggestion(
    val urgency: Severity,
    val suggestedDays: Int,
    val reason: String
)

enum class Severity {
    SAFE,
    WARNING,
    DANGER
}

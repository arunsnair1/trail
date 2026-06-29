package com.caladapt.domain.algorithm

import com.caladapt.domain.model.Goal
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Calculates daily calorie targets based on the user's goal and current TDEE.
 *
 * Target strategies:
 *   CUT:    TDEE − 300 to 500 kcal (0.5–1.0% BW loss/week)
 *   BULK:   TDEE + 200 to 400 kcal (0.25–0.5% BW gain/week)
 *   RECOMP: TDEE ± 0 (maintain weight, body recomposition via high protein)
 *
 * Weekly adaptive adjustment compares actual vs target weight change
 * and nudges calories by 50–100 kcal to stay on track.
 */
@Singleton
class CalorieTargetEngine @Inject constructor() {

    companion object {
        // Deficit/surplus ranges (kcal/day)
        const val CUT_DEFICIT_MIN = 300
        const val CUT_DEFICIT_MAX = 500
        const val CUT_DEFICIT_DEFAULT = 400

        const val BULK_SURPLUS_MIN = 200
        const val BULK_SURPLUS_MAX = 400
        const val BULK_SURPLUS_DEFAULT = 300

        // Weekly adjustment step size (kcal)
        const val ADJUSTMENT_STEP_SMALL = 50
        const val ADJUSTMENT_STEP_LARGE = 100

        // Target weight change rates (% of body weight per week)
        const val CUT_TARGET_RATE_MIN = 0.005f  // 0.5%
        const val CUT_TARGET_RATE_MAX = 0.010f  // 1.0%
        const val CUT_TARGET_RATE_DEFAULT = 0.007f // 0.7%

        const val BULK_TARGET_RATE_MIN = 0.0025f // 0.25%
        const val BULK_TARGET_RATE_MAX = 0.005f  // 0.5%
        const val BULK_TARGET_RATE_DEFAULT = 0.003f // 0.3%
    }

    /**
     * Calculate the initial calorie target for a goal phase.
     *
     * @param tdee Current TDEE estimate (kcal/day)
     * @param goal User's goal (CUT, BULK, RECOMP)
     * @param aggressiveness 0.0 (conservative) to 1.0 (aggressive), default 0.5
     * @return Daily calorie target
     */
    fun calculateGoalCalories(
        tdee: Float,
        goal: Goal,
        aggressiveness: Float = 0.5f
    ): Int {
        val clampedAggro = aggressiveness.coerceIn(0f, 1f)

        return when (goal) {
            Goal.CUT -> {
                val deficit = CUT_DEFICIT_MIN + ((CUT_DEFICIT_MAX - CUT_DEFICIT_MIN) * clampedAggro)
                (tdee - deficit).toInt()
            }
            Goal.BULK -> {
                val surplus = BULK_SURPLUS_MIN + ((BULK_SURPLUS_MAX - BULK_SURPLUS_MIN) * clampedAggro)
                (tdee + surplus).toInt()
            }
            Goal.RECOMP -> tdee.toInt()
        }
    }

    /**
     * Calculate the target weekly weight change for the current goal.
     *
     * @param currentWeightKg User's current weight
     * @param goal User's goal
     * @return Target change in kg/week (negative for cut)
     */
    fun getTargetWeeklyChange(currentWeightKg: Float, goal: Goal): Float {
        return when (goal) {
            Goal.CUT -> -(currentWeightKg * CUT_TARGET_RATE_DEFAULT) // e.g., 80kg → -0.56 kg/week
            Goal.BULK -> currentWeightKg * BULK_TARGET_RATE_DEFAULT    // e.g., 80kg → +0.24 kg/week
            Goal.RECOMP -> 0f
        }
    }

    /**
     * Weekly adaptive adjustment based on actual vs target progress.
     *
     * @param currentTarget Current daily calorie target
     * @param actualWeeklyChange Actual weight change this week (kg, from EMA)
     * @param targetWeeklyChange Expected weight change this week (kg)
     * @param goal User's goal
     * @return AdjustmentResult with new calorie target and explanation
     */
    fun weeklyAdjustment(
        currentTarget: Int,
        actualWeeklyChange: Float,
        targetWeeklyChange: Float,
        goal: Goal
    ): AdjustmentResult {
        if (goal == Goal.RECOMP) {
            return AdjustmentResult(
                newTarget = currentTarget,
                change = 0,
                reason = "Recomposition: maintaining current calories"
            )
        }

        // Calculate how the actual change compares to target
        val ratio = if (targetWeeklyChange != 0f) {
            actualWeeklyChange / targetWeeklyChange
        } else {
            1f
        }

        return when (goal) {
            Goal.CUT -> adjustForCut(currentTarget, actualWeeklyChange, ratio)
            Goal.BULK -> adjustForBulk(currentTarget, actualWeeklyChange, ratio)
            else -> AdjustmentResult(currentTarget, 0, "No adjustment needed")
        }
    }

    private fun adjustForCut(
        currentTarget: Int,
        actualWeeklyChange: Float,
        ratio: Float
    ): AdjustmentResult {
        // During a cut, actualWeeklyChange should be negative
        return when {
            // Losing way too fast (>120% of target rate) → increase cals
            ratio > 1.2f -> {
                val change = ADJUSTMENT_STEP_LARGE
                AdjustmentResult(
                    newTarget = currentTarget + change,
                    change = change,
                    reason = "Losing too fast (${"%.2f".format(actualWeeklyChange)} kg/wk). " +
                            "Increasing calories to preserve muscle."
                )
            }
            // Losing slightly too fast → small increase
            ratio > 1.05f -> {
                val change = ADJUSTMENT_STEP_SMALL
                AdjustmentResult(
                    newTarget = currentTarget + change,
                    change = change,
                    reason = "Slightly faster loss than target. Small calorie increase."
                )
            }
            // Not losing enough (<50% of target) → decrease cals
            ratio < 0.5f -> {
                val change = -ADJUSTMENT_STEP_LARGE
                AdjustmentResult(
                    newTarget = currentTarget + change,
                    change = change,
                    reason = "Weight loss stalled (${"%.2f".format(actualWeeklyChange)} kg/wk). " +
                            "Reducing calories."
                )
            }
            // Losing slightly too slow → small decrease
            ratio < 0.8f -> {
                val change = -ADJUSTMENT_STEP_SMALL
                AdjustmentResult(
                    newTarget = currentTarget + change,
                    change = change,
                    reason = "Slightly slower loss than target. Small calorie decrease."
                )
            }
            // On track → no change
            else -> {
                AdjustmentResult(
                    newTarget = currentTarget,
                    change = 0,
                    reason = "Progress on track (${"%.2f".format(actualWeeklyChange)} kg/wk). " +
                            "No adjustment needed."
                )
            }
        }
    }

    private fun adjustForBulk(
        currentTarget: Int,
        actualWeeklyChange: Float,
        ratio: Float
    ): AdjustmentResult {
        // During a bulk, actualWeeklyChange should be positive
        return when {
            // Gaining too fast (>120%) → decrease cals to avoid excess fat
            ratio > 1.2f -> {
                val change = -ADJUSTMENT_STEP_LARGE
                AdjustmentResult(
                    newTarget = currentTarget + change,
                    change = change,
                    reason = "Gaining too fast (${"%.2f".format(actualWeeklyChange)} kg/wk). " +
                            "Reducing calories to minimize fat gain."
                )
            }
            ratio > 1.05f -> {
                val change = -ADJUSTMENT_STEP_SMALL
                AdjustmentResult(
                    newTarget = currentTarget + change,
                    change = change,
                    reason = "Slightly faster gain than target. Small calorie decrease."
                )
            }
            // Not gaining enough → increase cals
            ratio < 0.5f -> {
                val change = ADJUSTMENT_STEP_LARGE
                AdjustmentResult(
                    newTarget = currentTarget + change,
                    change = change,
                    reason = "Not gaining enough (${"%.2f".format(actualWeeklyChange)} kg/wk). " +
                            "Increasing calories."
                )
            }
            ratio < 0.8f -> {
                val change = ADJUSTMENT_STEP_SMALL
                AdjustmentResult(
                    newTarget = currentTarget + change,
                    change = change,
                    reason = "Slightly slower gain than target. Small calorie increase."
                )
            }
            else -> {
                AdjustmentResult(
                    newTarget = currentTarget,
                    change = 0,
                    reason = "Lean bulk on track (${"%.2f".format(actualWeeklyChange)} kg/wk). " +
                            "No adjustment needed."
                )
            }
        }
    }
}

data class AdjustmentResult(
    val newTarget: Int,
    val change: Int,
    val reason: String
)

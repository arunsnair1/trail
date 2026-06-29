package com.caladapt.domain.algorithm

import com.caladapt.data.db.entity.WeightLogEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Detects when the user's weight has stabilized, indicating that
 * maintenance calories have been found.
 *
 * Stabilization criteria:
 *   - Weight trend rate (from EMA) is within ±0.1 kg/week
 *   - This stability has been maintained for at least 14 consecutive days
 *   - At least 7 data points exist in the evaluation window
 *
 * During the Discovery phase, this detector determines when to
 * transition to the Goal phase.
 */
@Singleton
class MaintenanceDetector @Inject constructor(
    private val emaCalculator: EMACalculator
) {

    companion object {
        /** Maximum acceptable weight fluctuation to consider "stable" (kg/week) */
        const val STABILITY_THRESHOLD = 0.1f

        /** Minimum consecutive days of stability required */
        const val MIN_STABLE_DAYS = 14

        /** Minimum data points needed in the stability window */
        const val MIN_DATA_POINTS = 7

        /** Window sizes to check stability across multiple timeframes */
        val STABILITY_WINDOWS = listOf(7, 14)
    }

    /**
     * Check if weight is currently stable (maintenance found).
     *
     * @param weights All weight entries sorted by date ascending
     * @return StabilityResult with status and calculated maintenance calories
     */
    fun checkStability(
        weights: List<WeightLogEntity>,
        avgDailyCalories: Float
    ): StabilityResult {
        if (weights.size < MIN_DATA_POINTS) {
            return StabilityResult(
                isStable = false,
                reason = "Need at least $MIN_DATA_POINTS days of data (have ${weights.size})",
                weeklyRate = null,
                daysStable = 0
            )
        }

        // Check stability across multiple windows
        val rates = STABILITY_WINDOWS.mapNotNull { window ->
            emaCalculator.calculateWeightTrendRate(weights, window)
        }

        if (rates.isEmpty()) {
            return StabilityResult(
                isStable = false,
                reason = "Insufficient data for trend calculation",
                weeklyRate = null,
                daysStable = 0
            )
        }

        // All windows must show stability
        val allStable = rates.all { kotlin.math.abs(it) <= STABILITY_THRESHOLD }
        val avgRate = rates.average().toFloat()

        // Estimate how many days the weight has been stable
        val daysStable = estimateDaysStable(weights)

        val isStable = allStable && daysStable >= MIN_STABLE_DAYS

        return StabilityResult(
            isStable = isStable,
            reason = when {
                isStable -> "Weight stable at ≈${avgDailyCalories.toInt()} kcal/day for $daysStable days"
                !allStable -> "Weight still trending (${formatRate(avgRate)}/week)"
                daysStable < MIN_STABLE_DAYS -> "Stable but need ${MIN_STABLE_DAYS - daysStable} more days to confirm"
                else -> "Still evaluating..."
            },
            weeklyRate = avgRate,
            daysStable = daysStable,
            maintenanceCalories = if (isStable) avgDailyCalories else null
        )
    }

    /**
     * Determine what calorie adjustment is needed during discovery.
     *
     * @param weightTrendRate Current weight trend in kg/week
     * @return Recommended calorie adjustment (positive = add, negative = subtract)
     */
    fun getDiscoveryAdjustment(weightTrendRate: Float): DiscoveryAdjustment {
        return when {
            weightTrendRate < -0.3f -> DiscoveryAdjustment(
                calorieChange = 150,
                reason = "Losing weight too fast (${formatRate(weightTrendRate)}/week). Increasing calories."
            )
            weightTrendRate < -STABILITY_THRESHOLD -> DiscoveryAdjustment(
                calorieChange = 100,
                reason = "Slowly losing weight (${formatRate(weightTrendRate)}/week). Slightly increasing calories."
            )
            weightTrendRate > 0.3f -> DiscoveryAdjustment(
                calorieChange = -150,
                reason = "Gaining weight (${formatRate(weightTrendRate)}/week). Decreasing calories."
            )
            weightTrendRate > STABILITY_THRESHOLD -> DiscoveryAdjustment(
                calorieChange = -100,
                reason = "Slowly gaining weight (${formatRate(weightTrendRate)}/week). Slightly decreasing calories."
            )
            else -> DiscoveryAdjustment(
                calorieChange = 0,
                reason = "Weight is stable (${formatRate(weightTrendRate)}/week). Maintain current intake."
            )
        }
    }

    /**
     * Estimate how many recent days the weight has been within the stability threshold.
     */
    private fun estimateDaysStable(weights: List<WeightLogEntity>): Int {
        if (weights.size < 3) return 0

        val sorted = weights.sortedBy { it.date }

        // Walk backwards from the most recent entry
        // Check progressively larger windows until we find instability
        for (windowDays in MIN_STABLE_DAYS downTo 3) {
            if (sorted.size < windowDays) continue

            val windowWeights = sorted.takeLast(windowDays)
            val rate = emaCalculator.calculateWeightTrendRate(windowWeights, windowDays)

            if (rate != null && kotlin.math.abs(rate) > STABILITY_THRESHOLD) {
                return windowDays - 1
            }
        }

        return sorted.size // All data is stable
    }

    private fun formatRate(rate: Float): String {
        val sign = if (rate >= 0) "+" else ""
        return "$sign${"%.2f".format(rate)} kg"
    }
}

data class StabilityResult(
    val isStable: Boolean,
    val reason: String,
    val weeklyRate: Float?,
    val daysStable: Int,
    val maintenanceCalories: Float? = null
)

data class DiscoveryAdjustment(
    val calorieChange: Int,
    val reason: String
)

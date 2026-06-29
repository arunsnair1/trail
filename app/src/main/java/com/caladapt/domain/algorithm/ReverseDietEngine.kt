package com.caladapt.domain.algorithm

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

data class ReverseDietAdjustment(
    val change: Int,
    val reason: String
)

/**
 * Stage 7B: Reverse Dieting Engine.
 * Responsibly increases calories back up to empirical TDEE after a diet phase,
 * preventing sudden fat regain by adding calories slowly (+50 to +100 kcal/week)
 * while monitoring weight trends.
 */
@Singleton
class ReverseDietEngine @Inject constructor() {

    companion object {
        const val MAX_WEEKLY_ADDITION = 100
        const val CONSERVATIVE_ADDITION = 50
        
        /** If gaining faster than this, pause reverse diet */
        const val PAUSE_GAIN_THRESHOLD_KG = 0.2f 
    }

    /**
     * Calculates the weekly calorie bump during a reverse diet.
     *
     * @param currentTarget Currently prescribed daily calories
     * @param empiricalTdee Calculated actual TDEE
     * @param weightTrendKgPerWeek Current smoothed weight trend
     * @return The calorie adjustment and reason
     */
    fun calculateAdjustment(
        currentTarget: Int,
        empiricalTdee: Int,
        weightTrendKgPerWeek: Float
    ): ReverseDietAdjustment {
        // If we are already at or above TDEE, no reverse diet needed
        if (currentTarget >= empiricalTdee) {
            return ReverseDietAdjustment(0, "Target is already at or above maintenance. Reverse diet complete.")
        }
        
        val gap = empiricalTdee - currentTarget

        // If weight is increasing too fast, pause the reverse diet
        if (weightTrendKgPerWeek > PAUSE_GAIN_THRESHOLD_KG) {
            return ReverseDietAdjustment(0, "Pausing reverse diet. Weight trend is increasing (${"%.2f".format(weightTrendKgPerWeek)} kg/wk). Letting metabolism catch up.")
        }

        // If losing weight or very stable, add up to 100 kcal
        val addition = if (weightTrendKgPerWeek <= 0.05f) {
            // Stable or losing -> aggressive addition
            minOf(MAX_WEEKLY_ADDITION, gap)
        } else {
            // Slightly gaining but within threshold -> conservative addition
            minOf(CONSERVATIVE_ADDITION, gap)
        }

        return ReverseDietAdjustment(
            change = addition,
            reason = "Reverse dieting: Adding $addition kcal to safely step back up to your maintenance."
        )
    }
}

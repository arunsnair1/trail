package com.caladapt.domain.algorithm

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * Calculates the initial estimated maintenance calories (TDEE) for a user
 * entering the Discovery Phase.
 *
 * Uses the Mifflin-St Jeor equation for BMR, multiplied by a fixed
 * Physical Activity Level (PAL) of 1.4 (Sedentary/Light Activity).
 * The final result is rounded to the nearest 50 kcal to reflect that
 * it is an estimation, not an exact science.
 */
@Singleton
class EstimatedMaintenanceCalculator @Inject constructor() {

    companion object {
        const val PAL_DEFAULT = 1.4f
        const val ROUNDING_FACTOR = 50f
    }

    /**
     * Estimates initial TDEE using Mifflin-St Jeor * 1.4, rounded to nearest 50.
     *
     * @param weightKg User's weight in kg
     * @param heightCm User's height in cm
     * @param age User's age in years
     * @param isMale True if male, false if female
     * @return Estimated TDEE in kcal/day
     */
    fun calculateEstimatedTDEE(
        weightKg: Float,
        heightCm: Float,
        age: Int,
        isMale: Boolean
    ): Float {
        val bmr = if (isMale) {
            (10f * weightKg) + (6.25f * heightCm) - (5f * age) + 5f
        } else {
            (10f * weightKg) + (6.25f * heightCm) - (5f * age) - 161f
        }

        val rawTdee = bmr * PAL_DEFAULT
        
        // Round to nearest 50
        val roundedTdee = (rawTdee / ROUNDING_FACTOR).roundToInt() * ROUNDING_FACTOR
        return roundedTdee
    }
}

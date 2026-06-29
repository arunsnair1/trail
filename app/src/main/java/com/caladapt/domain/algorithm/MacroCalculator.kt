package com.caladapt.domain.algorithm

import com.caladapt.domain.model.Goal
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Calculates macronutrient targets (protein, fat, carbs) from a
 * daily calorie target and the user's body weight/body fat.
 *
 * Priority order (science-backed):
 *   1. PROTEIN: Based on Lean Body Mass (LBM)
 *   2. FAT: Strictly 0.8g per kg of LBM
 *   3. CARBS: Fill the remainder
 */
@Singleton
class MacroCalculator @Inject constructor() {

    companion object {
        // Caloric values per gram
        const val PROTEIN_KCAL_PER_G = 4f
        const val CARB_KCAL_PER_G = 4f
        const val FAT_KCAL_PER_G = 9f

        // Protein multipliers (g per kg LBM)
        const val PROTEIN_CUT = 2.2f
        const val PROTEIN_DEFAULT = 2.0f

        // Fat multiplier (g per kg LBM)
        const val FAT_MULTIPLIER = 0.8f
        
        // Fallback LBM estimation (assumes 18% body fat on average if no data exists)
        const val FALLBACK_LBM_RATIO = 0.82f
    }

    /**
     * Calculate macro targets for a given calorie target and goal.
     *
     * @param dailyCalories Total daily calorie target
     * @param weightKg User's current weight in kg
     * @param navyBodyFat User's body fat percentage (e.g. 15.5 for 15.5%), or null if unknown
     * @param goal User's goal (or null for discovery phase)
     * @return MacroResult (Success or CaloriesTooLowForMacros)
     */
    fun calculateMacros(
        dailyCalories: Int,
        weightKg: Float,
        navyBodyFat: Float? = null,
        goal: Goal? = null
    ): MacroResult {
        // Step 1: Determine LBM (Lean Body Mass)
        val lbmKg = if (navyBodyFat != null && navyBodyFat > 0f && navyBodyFat < 100f) {
            weightKg * (1f - (navyBodyFat / 100f))
        } else {
            weightKg * FALLBACK_LBM_RATIO
        }

        // Step 2: Protein
        val proteinMultiplier = if (goal == Goal.CUT) PROTEIN_CUT else PROTEIN_DEFAULT
        val proteinG = lbmKg * proteinMultiplier
        val proteinCals = proteinG * PROTEIN_KCAL_PER_G

        // Step 3: Fat (strictly 0.8g x LBM)
        val fatG = lbmKg * FAT_MULTIPLIER
        val fatCals = fatG * FAT_KCAL_PER_G

        // Step 4: Carbs fill the remainder
        val remainingCals = dailyCalories - proteinCals - fatCals
        
        if (remainingCals < 0) {
            return MacroResult.CaloriesTooLowForMacros
        }

        val carbsG = remainingCals / CARB_KCAL_PER_G

        return MacroResult.Success(
            MacroTargets(
                proteinG = proteinG,
                carbsG = carbsG,
                fatG = fatG,
                proteinCals = proteinCals,
                carbsCals = remainingCals,
                fatCals = fatCals
            )
        )
    }

    /**
     * Calculate percentage breakdown of macros.
     */
    fun calculatePercentages(macros: MacroTargets, totalCalories: Int): MacroPercentages {
        val total = totalCalories.toFloat().coerceAtLeast(1f)
        return MacroPercentages(
            proteinPct = (macros.proteinCals / total * 100).toInt(),
            carbsPct = (macros.carbsCals / total * 100).toInt(),
            fatPct = (macros.fatCals / total * 100).toInt()
        )
    }
}

sealed class MacroResult {
    data class Success(val targets: MacroTargets) : MacroResult()
    object CaloriesTooLowForMacros : MacroResult()
}

data class MacroTargets(
    val proteinG: Float,
    val carbsG: Float,
    val fatG: Float,
    val proteinCals: Float,
    val carbsCals: Float,
    val fatCals: Float
) {
    val totalCals: Float get() = proteinCals + carbsCals + fatCals
}

data class MacroPercentages(
    val proteinPct: Int,
    val carbsPct: Int,
    val fatPct: Int
)

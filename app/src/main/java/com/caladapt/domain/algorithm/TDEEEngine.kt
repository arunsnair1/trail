package com.caladapt.domain.algorithm

import com.caladapt.data.db.entity.DailySummaryEntity
import com.caladapt.data.db.entity.WeightLogEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

enum class DataQuality {
    HIGH, MEDIUM, LOW
}

sealed class TDEEResult {
    data class Success(
        val tdee: Float,
        val weightChangeRatePerWeek: Float,
        val avgDailyCalories: Float,
        val dataPointCount: Int,
        val dataQuality: DataQuality
    ) : TDEEResult()
    
    data class InsufficientCompliance(val missingDays: Int) : TDEEResult()
    
    data class BelowBMRWarning(val computedTdee: Float, val bmr: Float) : TDEEResult()
    
    object InsufficientData : TDEEResult()
}

/**
 * Adaptive TDEE (Total Daily Energy Expenditure) Engine.
 *
 * Calculates actual TDEE from the relationship between calorie intake
 * and weight change over exactly 14 days, using energy balance.
 */
@Singleton
class TDEEEngine @Inject constructor(
    private val emaCalculator: EMACalculator
) {

    companion object {
        const val KCAL_PER_KG = 7700f
        
        /** Lock calculation window to exactly 14 days */
        const val TDEE_WINDOW_DAYS = 14

        const val MIN_TDEE = 1000f
        const val MAX_TDEE = 6000f
        
        private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    }

    /**
     * Calculate adaptive TDEE from calorie intake and weight data.
     */
    fun calculateTDEE(
        avgDailyCalories: Float,
        weightChangeRatePerWeek: Float
    ): Float? {
        if (avgDailyCalories <= 0) return null

        val tdee = avgDailyCalories - (weightChangeRatePerWeek * KCAL_PER_KG / 7f)
        return tdee.coerceIn(MIN_TDEE, MAX_TDEE)
    }

    /**
     * Calculate TDEE using exact 14-day window logic.
     * Evaluates missing days, BMR guardrails, and data quality.
     */
    fun calculateTDEEFromData(
        weights: List<WeightLogEntity>,
        summaries: List<DailySummaryEntity>,
        userWeightKg: Float,
        userHeightCm: Float,
        userAge: Int,
        isMale: Boolean
    ): TDEEResult {
        if (weights.isEmpty() || summaries.isEmpty()) return TDEEResult.InsufficientData

        val sortedWeights = weights.sortedBy { it.date }
        val latestWeightDate = LocalDate.parse(sortedWeights.last().date, dateFormatter)
        val windowStart = latestWeightDate.minusDays(TDEE_WINDOW_DAYS.toLong())
        val windowStartStr = windowStart.format(dateFormatter)
        
        val weightsInWindow = sortedWeights.filter { it.date >= windowStartStr }
        val summariesInWindow = summaries.filter { it.date >= windowStartStr && it.date <= sortedWeights.last().date }
        
        // Count missing or zero-calorie days inside the 14-day window
        var missingDays = 0
        var totalCalories = 0f
        
        for (i in 0 until TDEE_WINDOW_DAYS) {
            val dateStr = windowStart.plusDays(i.toLong()).format(dateFormatter)
            val summary = summariesInWindow.find { it.date == dateStr }
            if (summary == null || summary.totalCalories <= 0) {
                missingDays++
            } else {
                totalCalories += summary.totalCalories
            }
        }

        if (missingDays > 3) {
            return TDEEResult.InsufficientCompliance(missingDays)
        }
        
        val dataQuality = when (missingDays) {
            0 -> DataQuality.HIGH
            1, 2 -> DataQuality.MEDIUM
            else -> DataQuality.LOW
        }
        
        val validDays = TDEE_WINDOW_DAYS - missingDays
        val avgDailyCalories = totalCalories / validDays

        val weightTrendRate = emaCalculator.calculateWeightTrendRate(weightsInWindow, TDEE_WINDOW_DAYS)
            ?: return TDEEResult.InsufficientData

        val tdee = calculateTDEE(avgDailyCalories, weightTrendRate) ?: return TDEEResult.InsufficientData

        val bmr = calculateBMR(userWeightKg, userHeightCm, userAge, isMale)
        if (tdee < bmr) {
            return TDEEResult.BelowBMRWarning(tdee, bmr)
        }

        return TDEEResult.Success(
            tdee = tdee,
            weightChangeRatePerWeek = weightTrendRate,
            avgDailyCalories = avgDailyCalories,
            dataPointCount = weightsInWindow.size,
            dataQuality = dataQuality
        )
    }

    /**
     * Calculate BMR using Mifflin-St Jeor (used as safety floor).
     */
    fun calculateBMR(
        weightKg: Float,
        heightCm: Float,
        age: Int,
        isMale: Boolean
    ): Float {
        return if (isMale) {
            (10f * weightKg) + (6.25f * heightCm) - (5f * age) + 5f
        } else {
            (10f * weightKg) + (6.25f * heightCm) - (5f * age) - 161f
        }
    }
}

package com.caladapt.domain.algorithm

import com.caladapt.data.db.entity.DailySummaryEntity
import javax.inject.Inject
import javax.inject.Singleton

data class ComplianceResult(
    val scorePercentage: Int,
    val isCompliant: Boolean
)

/**
 * Calculates user compliance over a rolling window (usually 7 or 14 days).
 * 
 * Compliance is composed of two factors:
 * 1. Logging Consistency (50%): How many days were actually logged.
 * 2. Adherence (50%): Of the days logged, how many were within +/- 100 kcal of target.
 */
@Singleton
class ComplianceCalculator @Inject constructor() {

    companion object {
        const val ADHERENCE_TOLERANCE_KCAL = 100
        const val COMPLIANT_THRESHOLD_PCT = 80
    }

    /**
     * Calculate compliance score for a list of daily summaries over a specific window length.
     *
     * @param summaries The list of daily summaries falling within the window.
     * @param windowDays The length of the window (e.g. 7 or 14).
     * @return ComplianceResult containing score (0-100) and boolean flag.
     */
    fun calculateCompliance(
        summaries: List<DailySummaryEntity>,
        windowDays: Int = 7
    ): ComplianceResult {
        if (windowDays <= 0) return ComplianceResult(0, false)
        
        // Count how many days have non-zero calories logged
        val daysLogged = summaries.count { it.totalCalories > 0 }
        
        val loggingScore = (daysLogged.toFloat() / windowDays) * 0.5f

        val adherenceScore = if (daysLogged > 0) {
            val adheredDays = summaries.count {
                it.totalCalories > 0 && Math.abs(it.totalCalories - it.calorieTarget) <= ADHERENCE_TOLERANCE_KCAL
            }
            (adheredDays.toFloat() / daysLogged) * 0.5f
        } else {
            0f
        }

        val totalScore = ((loggingScore + adherenceScore) * 100).toInt()
        val isCompliant = totalScore > COMPLIANT_THRESHOLD_PCT

        return ComplianceResult(
            scorePercentage = totalScore,
            isCompliant = isCompliant
        )
    }
}

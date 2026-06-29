package com.caladapt.domain.algorithm

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

data class DietBreakAdvice(
    val recommended: Boolean,
    val reason: String?
)

/**
 * Evaluates whether a user should take a diet break based on:
 * 1. Large continuous deficit duration (time-based)
 * 2. 14-day weight stall despite high logging compliance (stall-based)
 */
@Singleton
class DietBreakAdvisor @Inject constructor() {

    companion object {
        const val WEEKS_FOR_STRONG_SUGGESTION = 12
        const val WEEKS_FOR_MILD_SUGGESTION = 6
        const val STALL_THRESHOLD_KG_PER_WEEK = 0.05f
        const val MIN_COMPLIANCE_FOR_STALL_CHECK = 0.75f
    }

    /**
     * Checks if a diet break is recommended.
     *
     * @param daysInContinuousDeficit Number of consecutive days in CUT phase
     * @param weightTrendKgPerWeek Current weekly weight change trend
     * @param compliancePct Recent logging compliance (0.0 to 1.0)
     * @return DietBreakAdvice indicating if a break is recommended and why
     */
    fun checkDietBreak(
        daysInContinuousDeficit: Int,
        weightTrendKgPerWeek: Float,
        compliancePct: Float
    ): DietBreakAdvice {
        // 1. Time-based checks
        if (daysInContinuousDeficit >= WEEKS_FOR_STRONG_SUGGESTION * 7) {
            return DietBreakAdvice(
                recommended = true,
                reason = "You've been in a deficit for ${daysInContinuousDeficit / 7} weeks. A 1-2 week diet break at maintenance is strongly recommended to mitigate metabolic adaptation."
            )
        }

        if (daysInContinuousDeficit >= WEEKS_FOR_MILD_SUGGESTION * 7) {
            return DietBreakAdvice(
                recommended = true,
                reason = "You've been in a deficit for ${daysInContinuousDeficit / 7} weeks. Consider a 1-week diet break at maintenance to support recovery and adherence."
            )
        }

        // 2. Stall-based check
        // If they have been cutting for at least 3 weeks, and their weight is stalled,
        // and they are highly compliant, suggest a break to reset metabolism.
        val isStalled = abs(weightTrendKgPerWeek) <= STALL_THRESHOLD_KG_PER_WEEK
        val isHighlyCompliant = compliancePct >= MIN_COMPLIANCE_FOR_STALL_CHECK

        if (daysInContinuousDeficit >= 21 && isStalled && isHighlyCompliant) {
            return DietBreakAdvice(
                recommended = true,
                reason = "Your weight has stalled despite high logging compliance. A 1-week diet break at maintenance can help reset your metabolism and break the plateau."
            )
        }

        return DietBreakAdvice(recommended = false, reason = null)
    }
}

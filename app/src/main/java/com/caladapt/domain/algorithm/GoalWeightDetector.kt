package com.caladapt.domain.algorithm

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Detects if the user has reached their goal weight within an acceptable margin of error.
 */
@Singleton
class GoalWeightDetector @Inject constructor() {

    companion object {
        /** Acceptable tolerance in kg (approx 0.44 lbs) to consider goal reached */
        const val GOAL_TOLERANCE_KG = 0.2f
    }

    /**
     * Evaluates if the current EMA weight has reached the target weight.
     *
     * @param currentEmaWeightKg The user's current trend weight
     * @param targetWeightKg The target goal weight
     * @return true if reached, false otherwise
     */
    fun hasReachedGoal(
        currentEmaWeightKg: Float,
        targetWeightKg: Float
    ): Boolean {
        return abs(currentEmaWeightKg - targetWeightKg) <= GOAL_TOLERANCE_KG
    }
}

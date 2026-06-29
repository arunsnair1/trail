package com.caladapt.domain.model

/**
 * Represents the three main phases of the CalAdapt journey.
 *
 * DISCOVERY: Finding the user's true maintenance calories empirically
 * GOAL: Actively pursuing weight loss, gain, or recomposition
 * MAINTENANCE: Sustaining the achieved weight / reverse dieting
 */
enum class Phase {
    DISCOVERY,
    GOAL,
    MAINTENANCE
}

package com.caladapt.domain.model

/**
 * The user's weight-change goal during the Goal phase.
 *
 * CUT: Fat loss via caloric deficit (300–500 kcal below TDEE)
 * BULK: Muscle gain via caloric surplus (200–400 kcal above TDEE)
 * RECOMP: Body recomposition at maintenance (high protein, no caloric change)
 */
enum class Goal {
    CUT,
    BULK,
    RECOMP
}

package com.caladapt.ui.navigation

/**
 * Type-safe route definitions for the CalAdapt navigation graph.
 * Each route corresponds to a top-level destination in the app.
 */
sealed class Route(val route: String) {
    /** First-time setup flow — collects profile data and starts Discovery phase */
    data object Onboarding : Route("onboarding")

    /** Main daily hub — calorie ring, macro bars, phase info, quick stats */
    data object Dashboard : Route("dashboard")

    /** Log food/calorie entries with optional macros */
    data object LogCalories : Route("log_calories")

    /** Log daily weight and view weight trend chart */
    data object LogWeight : Route("log_weight")

    /** Body measurements: chest, waist, hips, limbs, body fat */
    data object Measurements : Route("measurements")

    /** Analytics: weight trend, calorie history, TDEE chart, macro breakdown, weekly averages */
    data object Analytics : Route("analytics")

    /** Weekly Review Flow */
    data object WeeklyReview : Route("weekly_review")

    /** App settings, notifications, data export */
    data object Settings : Route("settings")
}

/** Routes that appear in the bottom navigation bar */
val bottomNavRoutes = listOf(
    Route.Dashboard,
    Route.LogCalories,
    Route.LogWeight,
    Route.Measurements,
    Route.Analytics
)

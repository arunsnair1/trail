package com.caladapt.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.navigation.compose.composable
import com.caladapt.ui.screens.dashboard.DashboardScreen
import com.caladapt.ui.screens.logcalories.LogCaloriesScreen
import com.caladapt.ui.screens.logweight.LogWeightScreen
import com.caladapt.ui.screens.analytics.AnalyticsScreen
import com.caladapt.ui.screens.measurements.MeasurementsScreen
import com.caladapt.ui.screens.onboarding.OnboardingScreen

/**
 * Main navigation graph for CalAdapt.
 *
 * Flow:
 *   - First launch → Onboarding → Dashboard
 *   - Subsequent launches → Dashboard (with bottom nav)
 *   - Bottom nav tabs: Dashboard, Log Food, Log Weight
 */
@Composable
fun CalAdaptNavGraph(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = {
            fadeIn(animationSpec = tween(300)) + scaleIn(
                initialScale = 0.95f,
                animationSpec = tween(300)
            )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(300)) + scaleOut(
                targetScale = 1.05f,
                animationSpec = tween(300)
            )
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(300)) + scaleIn(
                initialScale = 1.05f,
                animationSpec = tween(300)
            )
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(300)) + scaleOut(
                targetScale = 0.95f,
                animationSpec = tween(300)
            )
        }
    ) {
        composable(Route.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Route.Dashboard.route) {
                        popUpTo(Route.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Route.Dashboard.route) {
            DashboardScreen(
                onNavigateToSettings = { navController.navigate(Route.Settings.route) }
            )
        }

        composable(Route.LogCalories.route) {
            LogCaloriesScreen()
        }

        composable(Route.LogWeight.route) {
            LogWeightScreen()
        }

        composable(Route.Measurements.route) {
            MeasurementsScreen()
        }

        composable(Route.Analytics.route) {
            AnalyticsScreen()
        }

        composable(Route.Settings.route) {
            com.caladapt.ui.screens.settings.SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onDataCleared = {
                    navController.navigate(Route.Onboarding.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            )
        }

        composable(Route.WeeklyReview.route) {
            com.caladapt.ui.screens.weeklyreview.WeeklyReviewScreen(
                onReviewComplete = { navController.popBackStack() }
            )
        }
    }
}

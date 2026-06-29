package com.caladapt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.caladapt.ui.navigation.BottomNavBar
import com.caladapt.ui.navigation.CalAdaptNavGraph
import com.caladapt.ui.navigation.Route
import com.caladapt.ui.screens.onboarding.OnboardingViewModel
import com.caladapt.ui.theme.AppBackgroundAlt
import com.caladapt.ui.theme.CalAdaptTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var dataStore: DataStore<Preferences>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CalAdaptTheme {
                val navController = rememberNavController()

                // Check if onboarding is complete
                var startDestination by remember { mutableStateOf<String?>(null) }
                LaunchedEffect(Unit) {
                    val isOnboarded = dataStore.data.map { prefs ->
                        prefs[OnboardingViewModel.ONBOARDING_COMPLETE_KEY] ?: false
                    }.first()
                    startDestination = if (isOnboarded) {
                        Route.Dashboard.route
                    } else {
                        Route.Onboarding.route
                    }
                }

                // Wait for start destination to be determined
                val destination = startDestination
                if (destination == null) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = AppBackgroundAlt
                    ) {
                        // Loading — determining start screen
                    }
                    return@CalAdaptTheme
                }

                // Track current route for bottom nav visibility
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                // Show bottom nav only on main screens (not onboarding)
                val showBottomNav = currentRoute in listOf(
                    Route.Dashboard.route,
                    Route.LogCalories.route,
                    Route.LogWeight.route,
                    Route.Measurements.route,
                    Route.Analytics.route
                )

                // Box layout — floating nav overlays content
                Box(modifier = Modifier.fillMaxSize()) {
                    CalAdaptNavGraph(
                        navController = navController,
                        startDestination = destination,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Floating glass bottom nav — overlaid at bottom
                    if (showBottomNav) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .zIndex(100f)
                        ) {
                            BottomNavBar(
                                currentRoute = currentRoute,
                                onNavigate = { route ->
                                    navController.navigate(route.route) {
                                        // Pop up to dashboard to avoid building up back stack
                                        popUpTo(Route.Dashboard.route) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

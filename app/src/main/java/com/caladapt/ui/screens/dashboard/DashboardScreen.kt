package com.caladapt.ui.screens.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.foundation.background
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.caladapt.ui.components.*
import com.caladapt.ui.theme.*

@Composable
fun DashboardScreen(
    onNavigateToSettings: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.isLoading) {
        DashboardSkeleton()
        return
    }

    MeshBackground(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Greeting header
            Box(modifier = Modifier.staggeredEntrance(0)) {
                GreetingHeader(
                    name = state.userName,
                    phaseName = state.phaseName,
                    phaseDescription = state.phaseDescription,
                    onSettingsClick = onNavigateToSettings
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Calorie ring card — hero style
            Box(modifier = Modifier.staggeredEntrance(1)) {
                CalorieRingCard(state)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Macro progress bars
            Box(modifier = Modifier.staggeredEntrance(2)) {
                MacroSection(state)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Quick stats
            Box(modifier = Modifier.staggeredEntrance(3)) {
                QuickStatsRow(state)
            }

            // Bottom padding for floating nav
            Spacer(modifier = Modifier.height(130.dp))
        }
    }
}

@Composable
private fun GreetingHeader(
    name: String,
    phaseName: String,
    phaseDescription: String,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (name.isNotEmpty()) "Hey $name 👋" else "Hey there 👋",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextMain
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = phaseDescription.ifEmpty { "Track your calories and weight daily" },
                style = MaterialTheme.typography.bodyMedium,
                color = TextSub
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            PhaseBadge(phase = phaseName)
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = TextSub
                )
            }
        }
    }
}

@Composable
private fun CalorieRingCard(state: DashboardState) {
    GlassHeroCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "TODAY'S CALORIES",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            )

            if (state.currentPhase?.phase == com.caladapt.domain.model.Phase.DISCOVERY.name) {
                val startedAt = state.currentPhase.startedAt
                val daysSinceStart = try {
                    val startDate = java.time.LocalDate.parse(startedAt.substring(0, 10))
                    java.time.temporal.ChronoUnit.DAYS.between(startDate, java.time.LocalDate.now()).toInt()
                } catch (e: Exception) { 0 }
                val daysRemaining = (14 - daysSinceStart).coerceAtLeast(0)
                
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Estimated • Updates in $daysRemaining days",
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentYellow
                )
                Spacer(modifier = Modifier.height(20.dp))
            } else {
                Spacer(modifier = Modifier.height(24.dp))
            }

            CalorieRing(
                consumed = state.dailyTargets?.caloriesConsumed ?: 0,
                target = state.dailyTargets?.calorieTarget ?: 2000,
                size = 200.dp,
                strokeWidth = 16.dp
            )
        }
    }
}

@Composable
private fun MacroSection(state: DashboardState) {
    val targets = state.dailyTargets

    GlassCard {
        SectionHeader(title = "Macros")

        Spacer(modifier = Modifier.height(16.dp))

        MacroProgressBar(
            label = "Protein",
            consumed = targets?.proteinConsumedG ?: 0f,
            target = targets?.proteinTargetG ?: 1f,
            gradientColors = listOf(ChartProtein, ChartProtein.copy(alpha = 0.6f))
        )

        Spacer(modifier = Modifier.height(16.dp))

        MacroProgressBar(
            label = "Carbs",
            consumed = targets?.carbsConsumedG ?: 0f,
            target = targets?.carbsTargetG ?: 1f,
            gradientColors = listOf(ChartCarbs, ChartCarbs.copy(alpha = 0.6f))
        )

        Spacer(modifier = Modifier.height(16.dp))

        MacroProgressBar(
            label = "Fat",
            consumed = targets?.fatConsumedG ?: 0f,
            target = targets?.fatTargetG ?: 1f,
            gradientColors = listOf(ChartFat, ChartFat.copy(alpha = 0.6f))
        )
    }
}

@Composable
private fun QuickStatsRow(state: DashboardState) {
    val targets = state.dailyTargets
    val latestWeight = state.recentWeights.lastOrNull()
    val unitSystem = state.unitSystem

    // Determine weight trend icon
    val weightHistory = state.recentWeights
    val trendIcon = if (weightHistory.size >= 5) {
        val recent = weightHistory.last().emaWeight
        val previous = weightHistory[weightHistory.size - 2].emaWeight
        val diff = recent - previous
        when {
            diff < -0.05f -> Icons.Default.TrendingDown
            diff > 0.05f -> Icons.Default.TrendingUp
            else -> Icons.Default.TrendingFlat
        }
    } else {
        Icons.Default.TrendingFlat
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // TDEE StatCard with Data Quality Dot if available
        Box(modifier = Modifier.weight(1f)) {
            StatCard(
                icon = Icons.Default.Speed,
                value = if (state.tdeeResult is com.caladapt.domain.algorithm.TDEEResult.Success) {
                    "${state.tdeeResult.tdee.toInt()}"
                } else if (state.tdeeResult is com.caladapt.domain.algorithm.TDEEResult.BelowBMRWarning) {
                    "${state.tdeeResult.computedTdee.toInt()}"
                } else {
                    "${targets?.tdeeEstimate?.toInt() ?: "—"}"
                },
                label = "TDEE",
                iconTint = AccentRed,
                modifier = Modifier.fillMaxWidth()
            )

            // Data Quality Dot overlay
            if (state.tdeeResult is com.caladapt.domain.algorithm.TDEEResult.Success) {
                val dotColor = when (state.tdeeResult.dataQuality) {
                    com.caladapt.domain.algorithm.DataQuality.HIGH -> Success
                    com.caladapt.domain.algorithm.DataQuality.MEDIUM -> AccentYellow
                    com.caladapt.domain.algorithm.DataQuality.LOW -> AccentTeal // Or Violet as requested
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(8.dp)
                        .background(dotColor, androidx.compose.foundation.shape.CircleShape)
                )
            }
        }
        StatCard(
            icon = Icons.Default.MonitorWeight,
            value = latestWeight?.let {
                "%.1f".format(unitSystem.fromKg(it.emaWeight))
            } ?: "—",
            label = "Weight (${unitSystem.weightLabel})",
            iconTint = AccentTeal,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            icon = trendIcon,
            value = latestWeight?.let {
                val raw = unitSystem.fromKg(it.weightKg)
                "%.1f".format(raw)
            } ?: "—",
            label = "Today",
            iconTint = AccentOrange,
            modifier = Modifier.weight(1f)
        )
    }

    // TDEE Warnings
    if (state.tdeeResult is com.caladapt.domain.algorithm.TDEEResult.InsufficientCompliance) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Log at least 11 of 14 days for your metabolism estimate to update.",
            style = MaterialTheme.typography.bodySmall,
            color = AccentYellow,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    } else if (state.tdeeResult is com.caladapt.domain.algorithm.TDEEResult.BelowBMRWarning) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Warning: Computed TDEE is below your BMR (${state.tdeeResult.bmr.toInt()} kcal).",
            style = MaterialTheme.typography.bodySmall,
            color = Error,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
private fun DashboardSkeleton() {
    MeshBackground(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Header Skeleton
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Box(modifier = Modifier.size(width = 150.dp, height = 32.dp).clip(RoundedCornerShape(8.dp)).shimmer())
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.size(width = 200.dp, height = 20.dp).clip(RoundedCornerShape(8.dp)).shimmer())
                }
                Box(modifier = Modifier.size(width = 80.dp, height = 32.dp).clip(RoundedCornerShape(16.dp)).shimmer())
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Hero Card Skeleton
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .shimmer()
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Macros Skeleton
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .shimmer()
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Stats Row Skeleton
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(120.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .shimmer()
                    )
                }
            }
        }
    }
}

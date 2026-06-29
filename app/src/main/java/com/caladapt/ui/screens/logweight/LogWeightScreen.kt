package com.caladapt.ui.screens.logweight

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.caladapt.data.db.entity.WeightLogEntity
import com.caladapt.ui.components.*
import com.caladapt.ui.theme.*

@Composable
fun LogWeightScreen(
    viewModel: LogWeightViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.savedSuccessfully) {
        if (state.savedSuccessfully) {
            snackbarHostState.showSnackbar("Weight logged! ✅")
            viewModel.dismissSuccess()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { padding ->
        MeshBackground(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Header
                Text(
                    text = "Log Weight",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextMain
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Weight input card
                Box(modifier = Modifier.staggeredEntrance(0)) {
                    WeightInputCard(
                        state = state,
                        onWeightChange = viewModel::updateWeightInput,
                        onNudgeUp = { viewModel.nudgeWeight(0.1f) },
                        onNudgeDown = { viewModel.nudgeWeight(-0.1f) },
                        onToggleUnit = viewModel::toggleUnit,
                        onToggleWaterRetention = viewModel::toggleWaterRetention,
                        onSubmit = viewModel::logWeight
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // EMA trend card
                Box(modifier = Modifier.staggeredEntrance(1)) {
                    EMATrendCard(state)
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Weight history chart
                Box(modifier = Modifier.staggeredEntrance(2)) {
                    if (state.weightHistory.isNotEmpty()) {
                        WeightChartCard(
                            weights = state.weightHistory,
                            unitSystem = state.unitSystem
                        )
                    } else {
                        PremiumEmptyState(
                            icon = Icons.AutoMirrored.Filled.TrendingFlat,
                            title = "No Weight Data",
                            subtitle = "Log your weight to see your trend and chart here.",
                            modifier = Modifier.padding(vertical = 32.dp)
                        )
                    }
                }

                // Bottom padding for floating nav
                Spacer(modifier = Modifier.height(120.dp))
            }
        }
    }
}

@Composable
private fun WeightInputCard(
    state: LogWeightState,
    onWeightChange: (String) -> Unit,
    onNudgeUp: () -> Unit,
    onNudgeDown: () -> Unit,
    onToggleUnit: () -> Unit,
    onToggleWaterRetention: () -> Unit,
    onSubmit: () -> Unit
) {
    GlassCard(cornerRadius = 32.dp) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Unit toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onToggleUnit,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(
                        Icons.Default.SwapHoriz,
                        contentDescription = "Toggle unit",
                        tint = AccentRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = state.weightLabel.uppercase(),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = AccentRed
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Weight input with nudge buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Minus button
                GlassIconButton(
                    onClick = onNudgeDown,
                    icon = Icons.Default.Remove,
                    contentDescription = "Decrease"
                )

                // Weight input
                GlassTextField(
                    value = state.weightInput,
                    onValueChange = onWeightChange,
                    textStyle = MaterialTheme.typography.displayLarge.copy(
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = TextMain
                    ),
                    suffix = {
                        Text(
                            state.weightLabel,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextSub
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.width(220.dp)
                )

                // Plus button
                GlassIconButton(
                    onClick = onNudgeUp,
                    icon = Icons.Default.Add,
                    contentDescription = "Increase"
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Water Retention Toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "High Water Retention / Cycle",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSub
                )
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = state.isWaterRetention,
                    onCheckedChange = { onToggleWaterRetention() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = AccentTeal,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = GlassBorder
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Log button
            GlassAccentButton(
                onClick = onSubmit,
                enabled = state.isValid && !state.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isSaving) {
                    Box(modifier = Modifier.size(24.dp)) {
                        CircularProgressIndicator(
                            modifier = Modifier.fillMaxSize(),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    }
                } else if (state.savedSuccessfully) {
                    SuccessCheckmark(modifier = Modifier.size(24.dp))
                } else {
                    Icon(
                        if (state.todayWeight != null) Icons.Default.Check else Icons.Default.Add,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (state.todayWeight != null) "Update Weight" else "Log Weight",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Error
            state.error?.let { error ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error,
                    color = Error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun EMATrendCard(state: LogWeightState) {
    val weights = state.weightHistory
    
    if (weights.size < 5) {
        GlassCard(cornerRadius = 28.dp) {
            Column {
                Text(
                    text = "SMOOTHED TREND (EMA)",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = TextSub
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Log a few more days to activate your trend line.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSub.copy(alpha = 0.7f)
                )
            }
        }
        return
    }

    val diff = weights.last().emaWeight - weights[weights.size - 2].emaWeight

    val trendIcon = when {
        diff < -0.05f -> Icons.AutoMirrored.Filled.TrendingDown
        diff > 0.05f -> Icons.AutoMirrored.Filled.TrendingUp
        else -> Icons.AutoMirrored.Filled.TrendingFlat
    }

    val trendColor = when {
        diff < -0.05f -> AccentTeal
        diff > 0.05f -> AccentYellow
        else -> Success
    }

    GlassCard(cornerRadius = 28.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SMOOTHED TREND (EMA)",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = TextSub
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${state.latestEMADisplay} ${state.weightLabel}",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = TextMain
                )
                Text(
                    text = "Filters daily noise (water, sodium)",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSub.copy(alpha = 0.7f)
                )
            }

            Icon(
                imageVector = trendIcon,
                contentDescription = "Trend",
                tint = trendColor,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

@Composable
private fun WeightChartCard(
    weights: List<WeightLogEntity>,
    unitSystem: com.caladapt.domain.model.UnitSystem
) {
    GlassCard(cornerRadius = 28.dp) {
        SectionHeader(title = "Weight History")
        Text(
            text = "Last ${weights.size} entries",
            style = MaterialTheme.typography.labelSmall,
            color = TextSub
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Simple weight chart
        SimpleWeightChart(
            weights = weights,
            unitSystem = unitSystem,
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Legend
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            LegendItem(color = ChartWeightRaw, label = "Raw weight")
            LegendItem(color = ChartWeightEMA, label = "EMA trend")
        }
    }
}

@Composable
private fun SimpleWeightChart(
    weights: List<WeightLogEntity>,
    unitSystem: com.caladapt.domain.model.UnitSystem,
    modifier: Modifier = Modifier
) {
    if (weights.isEmpty()) return

    val rawColor = ChartWeightRaw
    val emaColor = ChartWeightEMA

    // Animated reveal
    val revealAnim = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(weights) {
        revealAnim.animateTo(
            targetValue = 1f,
            animationSpec = androidx.compose.animation.core.tween(
                durationMillis = 1000,
                easing = androidx.compose.animation.core.FastOutSlowInEasing
            )
        )
    }

    Canvas(modifier = modifier) {
        val displayWeights = weights.map { unitSystem.fromKg(it.weightKg) }
        val displayEMA = weights.map { unitSystem.fromKg(it.emaWeight) }

        val allValues = displayWeights + displayEMA
        val minVal = allValues.min() - 0.5f
        val maxVal = allValues.max() + 0.5f
        val range = (maxVal - minVal).coerceAtLeast(1f)

        val stepX = if (weights.size > 1) size.width / (weights.size - 1) else size.width
        val paddingY = 8f

        fun yPos(value: Float): Float {
            return size.height - paddingY -
                    ((value - minVal) / range * (size.height - 2 * paddingY))
        }

        // Apply clipping for the reveal animation
        clipRect(
            left = 0f,
            top = 0f,
            right = size.width * revealAnim.value,
            bottom = size.height
        ) {
            // Draw raw weight dots
            displayWeights.forEachIndexed { index, value ->
                val x = index * stepX
                val y = yPos(value)
                drawCircle(
                    color = rawColor,
                    radius = 4f,
                    center = Offset(x, y)
                )
            }

            // Draw EMA trend line
            if (displayEMA.size >= 2) {
                val path = Path()
                displayEMA.forEachIndexed { index, value ->
                    val x = index * stepX
                    val y = yPos(value)
                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }
                drawPath(
                    path = path,
                    color = emaColor,
                    style = Stroke(width = 3f, cap = StrokeCap.Round)
                )
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSub
        )
    }
}

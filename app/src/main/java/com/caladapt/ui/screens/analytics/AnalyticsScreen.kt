package com.caladapt.ui.screens.analytics

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.caladapt.data.db.entity.DailySummaryEntity
import com.caladapt.data.db.entity.TDEEHistoryEntity
import com.caladapt.data.db.entity.WeightLogEntity
import com.caladapt.domain.model.UnitSystem
import com.caladapt.ui.components.*
import com.caladapt.ui.theme.*

// ═══════════════════════════════════════════════════════════════════════════
// ANALYTICS SCREEN
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.isLoading) {
        AnalyticsSkeleton()
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

            // Header
            Text(
                text = "Analytics",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextMain
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Date range selector
            Box(modifier = Modifier.staggeredEntrance(0)) {
                DateRangeSelector(
                    selectedRange = state.dateRange,
                    onRangeSelect = viewModel::selectDateRange
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (!state.hasAnyData) {
                PremiumEmptyState(
                    icon = Icons.Default.Insights,
                    title = "No Data Yet",
                    subtitle = "Start logging your weight and calories to see trends, charts, and insights here.",
                    modifier = Modifier.padding(vertical = 48.dp)
                )
            } else {
                // Weight trend chart
                if (state.weightData.size >= 2) {
                    Box(modifier = Modifier.staggeredEntrance(1)) {
                        WeightTrendCard(
                            weights = state.weightData,
                            unitSystem = state.unitSystem
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Calorie vs target chart
                if (state.calorieData.isNotEmpty()) {
                    Box(modifier = Modifier.staggeredEntrance(2)) {
                        CalorieBarChartCard(summaries = state.calorieData)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // TDEE history chart
                if (state.tdeeData.size >= 2) {
                    Box(modifier = Modifier.staggeredEntrance(3)) {
                        TDEEHistoryCard(tdeeEntries = state.tdeeData)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Macro donut
                if (state.macroTotals.totalDays > 0) {
                    Box(modifier = Modifier.staggeredEntrance(4)) {
                        MacroDonutCard(macros = state.macroTotals)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Weekly averages table
                if (state.weeklyAverages.isNotEmpty()) {
                    Box(modifier = Modifier.staggeredEntrance(5)) {
                        WeeklyAveragesCard(
                            weeklyAverages = state.weeklyAverages,
                            unitSystem = state.unitSystem
                        )
                    }
                }
            }

            // Bottom padding for floating nav
            Spacer(modifier = Modifier.height(130.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// DATE RANGE SELECTOR
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun DateRangeSelector(
    selectedRange: DateRange,
    onRangeSelect: (DateRange) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GlassBg)
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        DateRange.entries.forEach { range ->
            val isSelected = range == selectedRange
            val bgAlpha by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0f,
                animationSpec = tween(200),
                label = "range_bg_${range.name}"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AccentRed.copy(alpha = bgAlpha * 0.15f))
                    .clickable { onRangeSelect(range) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = range.label,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        letterSpacing = if (isSelected) 1.sp else 0.5.sp,
                        color = if (isSelected) AccentRed else TextSub
                    )
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// WEIGHT TREND CHART — dual line (raw + EMA)
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun WeightTrendCard(
    weights: List<WeightLogEntity>,
    unitSystem: UnitSystem
) {
    GlassCard(cornerRadius = 28.dp) {
        SectionHeader(title = "Weight Trend")

        val latest = weights.lastOrNull()
        val first = weights.firstOrNull()
        if (latest != null && first != null) {
            val deltaKg = latest.emaWeight - first.emaWeight
            val deltaDisplay = unitSystem.fromKg(deltaKg)
            val sign = if (deltaDisplay >= 0) "+" else ""
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Current: ${"%.1f".format(unitSystem.fromKg(latest.emaWeight))} ${unitSystem.weightLabel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSub
                )
                Text(
                    text = "$sign${"%.1f".format(deltaDisplay)} ${unitSystem.weightLabel}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (deltaKg < 0) AccentTeal else AccentOrange
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        WeightTrendChart(
            weights = weights,
            unitSystem = unitSystem,
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        )
    }
}

@Composable
private fun WeightTrendChart(
    weights: List<WeightLogEntity>,
    unitSystem: UnitSystem,
    modifier: Modifier = Modifier
) {
    if (weights.size < 2) return

    val revealAnim = remember { Animatable(0f) }
    LaunchedEffect(weights) {
        revealAnim.snapTo(0f)
        revealAnim.animateTo(1f, animationSpec = tween(1200, easing = FastOutSlowInEasing))
    }

    val rawData = weights.map { unitSystem.fromKg(it.weightKg) }
    val emaData = weights.map { unitSystem.fromKg(it.emaWeight) }
    val allValues = rawData + emaData

    Canvas(modifier = modifier) {
        val minVal = allValues.min() - 0.5f
        val maxVal = allValues.max() + 0.5f
        val range = (maxVal - minVal).coerceAtLeast(0.1f)
        val stepX = size.width / (weights.size - 1)
        val paddingY = 8f

        fun yPos(value: Float): Float =
            size.height - paddingY - ((value - minVal) / range * (size.height - 2 * paddingY))

        clipRect(right = size.width * revealAnim.value) {
            // Raw weight dots (muted)
            rawData.forEachIndexed { index, value ->
                drawCircle(
                    color = ChartWeightRaw,
                    radius = 3f,
                    center = Offset(index * stepX, yPos(value))
                )
            }

            // EMA trend line (bold)
            val emaPath = Path()
            emaData.forEachIndexed { index, value ->
                val x = index * stepX
                val y = yPos(value)
                if (index == 0) emaPath.moveTo(x, y) else emaPath.lineTo(x, y)
            }
            drawPath(
                path = emaPath,
                color = ChartWeightEMA,
                style = Stroke(width = 3f, cap = StrokeCap.Round)
            )

            // EMA dots
            emaData.forEachIndexed { index, value ->
                drawCircle(
                    color = ChartWeightEMA,
                    radius = 4f,
                    center = Offset(index * stepX, yPos(value))
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// CALORIE VS TARGET BAR CHART
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun CalorieBarChartCard(summaries: List<DailySummaryEntity>) {
    GlassCard(cornerRadius = 28.dp) {
        SectionHeader(title = "Calories vs Target")

        val avgActual = summaries.map { it.totalCalories }.average().toInt()
        val avgTarget = summaries.filter { it.calorieTarget > 0 }
            .map { it.calorieTarget }.average().toInt().let { if (it == 0) null else it }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Avg: $avgActual kcal/day",
                style = MaterialTheme.typography.labelSmall,
                color = TextSub
            )
            if (avgTarget != null) {
                Text(
                    text = "Target: $avgTarget kcal",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSub
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        CalorieBarChart(
            summaries = summaries,
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Legend
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LegendDot(color = AccentTeal, label = "On target")
            LegendDot(color = Error, label = "Over")
            LegendDot(color = AccentOrange.copy(alpha = 0.4f), label = "Target")
        }
    }
}

@Composable
private fun CalorieBarChart(
    summaries: List<DailySummaryEntity>,
    modifier: Modifier = Modifier
) {
    val revealAnim = remember { Animatable(0f) }
    LaunchedEffect(summaries) {
        revealAnim.snapTo(0f)
        revealAnim.animateTo(1f, animationSpec = tween(1000, easing = FastOutSlowInEasing))
    }

    // Limit to last 30 days for readability
    val data = summaries.takeLast(30)

    Canvas(modifier = modifier) {
        if (data.isEmpty()) return@Canvas

        val maxCal = data.maxOf {
            maxOf(it.totalCalories, it.calorieTarget)
        }.coerceAtLeast(1).toFloat()

        val barWidth = (size.width / data.size) * 0.6f
        val gap = (size.width / data.size) * 0.4f

        data.forEachIndexed { index, summary ->
            val x = index * (barWidth + gap)
            val actualReveal = revealAnim.value

            // Target bar (outline)
            if (summary.calorieTarget > 0) {
                val targetHeight = (summary.calorieTarget / maxCal) * size.height * actualReveal
                drawRect(
                    color = AccentOrange.copy(alpha = 0.25f),
                    topLeft = Offset(x, size.height - targetHeight),
                    size = Size(barWidth, targetHeight)
                )
            }

            // Actual bar (filled)
            if (summary.totalCalories > 0) {
                val actualHeight = (summary.totalCalories / maxCal) * size.height * actualReveal
                val isOver = summary.calorieTarget > 0 && summary.totalCalories > summary.calorieTarget
                drawRoundedBar(
                    color = if (isOver) Error else AccentTeal,
                    x = x,
                    barWidth = barWidth,
                    barHeight = actualHeight,
                    canvasHeight = size.height
                )
            }
        }
    }
}

private fun DrawScope.drawRoundedBar(
    color: Color,
    x: Float,
    barWidth: Float,
    barHeight: Float,
    canvasHeight: Float
) {
    val cornerRadius = (barWidth / 3).coerceAtMost(6f)
    val top = canvasHeight - barHeight

    // Simple rounded rect via path
    val path = Path().apply {
        moveTo(x, canvasHeight)
        lineTo(x, top + cornerRadius)
        quadraticBezierTo(x, top, x + cornerRadius, top)
        lineTo(x + barWidth - cornerRadius, top)
        quadraticBezierTo(x + barWidth, top, x + barWidth, top + cornerRadius)
        lineTo(x + barWidth, canvasHeight)
        close()
    }
    drawPath(path, color)
}

// ═══════════════════════════════════════════════════════════════════════════
// TDEE HISTORY CHART
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun TDEEHistoryCard(tdeeEntries: List<TDEEHistoryEntity>) {
    GlassCard(cornerRadius = 28.dp) {
        SectionHeader(title = "TDEE History")

        val latest = tdeeEntries.lastOrNull()
        if (latest != null) {
            Text(
                text = "Latest: ${latest.calculatedTDEE.toInt()} kcal/day",
                style = MaterialTheme.typography.labelSmall,
                color = TextSub
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        TDEELineChart(
            entries = tdeeEntries,
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        )
    }
}

@Composable
private fun TDEELineChart(
    entries: List<TDEEHistoryEntity>,
    modifier: Modifier = Modifier
) {
    if (entries.size < 2) return

    val revealAnim = remember { Animatable(0f) }
    LaunchedEffect(entries) {
        revealAnim.snapTo(0f)
        revealAnim.animateTo(1f, animationSpec = tween(1200, easing = FastOutSlowInEasing))
    }

    val data = entries.map { it.calculatedTDEE }

    Canvas(modifier = modifier) {
        val minVal = data.min() - 50f
        val maxVal = data.max() + 50f
        val range = (maxVal - minVal).coerceAtLeast(1f)
        val stepX = size.width / (data.size - 1)
        val paddingY = 8f

        fun yPos(value: Float): Float =
            size.height - paddingY - ((value - minVal) / range * (size.height - 2 * paddingY))

        clipRect(right = size.width * revealAnim.value) {
            // Area fill under line
            val areaPath = Path().apply {
                moveTo(0f, size.height)
                data.forEachIndexed { index, value ->
                    lineTo(index * stepX, yPos(value))
                }
                lineTo((data.size - 1) * stepX, size.height)
                close()
            }
            drawPath(areaPath, AccentBlue.copy(alpha = 0.12f))

            // Line
            val linePath = Path()
            data.forEachIndexed { index, value ->
                val x = index * stepX
                val y = yPos(value)
                if (index == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
            }
            drawPath(linePath, AccentBlue, style = Stroke(width = 3f, cap = StrokeCap.Round))

            // Dots
            data.forEachIndexed { index, value ->
                drawCircle(AccentBlue, radius = 4f, center = Offset(index * stepX, yPos(value)))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// MACRO BREAKDOWN DONUT
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun MacroDonutCard(macros: MacroAggregate) {
    GlassCard(cornerRadius = 28.dp) {
        SectionHeader(title = "Macro Breakdown")

        Text(
            text = "Avg per day · ${macros.totalDays} days",
            style = MaterialTheme.typography.labelSmall,
            color = TextSub
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Donut chart
            MacroDonutChart(
                macros = macros,
                modifier = Modifier.size(120.dp)
            )

            // Legend
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MacroLegendRow(
                    color = ChartProtein,
                    label = "Protein",
                    grams = macros.avgProtein,
                    pct = macros.proteinPct
                )
                MacroLegendRow(
                    color = ChartCarbs,
                    label = "Carbs",
                    grams = macros.avgCarbs,
                    pct = macros.carbsPct
                )
                MacroLegendRow(
                    color = ChartFat,
                    label = "Fat",
                    grams = macros.avgFat,
                    pct = macros.fatPct
                )
            }
        }
    }
}

@Composable
private fun MacroDonutChart(
    macros: MacroAggregate,
    modifier: Modifier = Modifier
) {
    val sweepAnim = remember { Animatable(0f) }
    LaunchedEffect(macros) {
        sweepAnim.snapTo(0f)
        sweepAnim.animateTo(1f, animationSpec = tween(1000, easing = FastOutSlowInEasing))
    }

    Canvas(modifier = modifier) {
        val strokeWidth = 20f
        val padding = strokeWidth / 2 + 4f
        val arcSize = Size(size.width - 2 * padding, size.height - 2 * padding)
        val topLeft = Offset(padding, padding)

        val total = macros.totalMacroGrams.coerceAtLeast(1f)
        val proteinSweep = (macros.avgProtein / total) * 360f * sweepAnim.value
        val carbsSweep = (macros.avgCarbs / total) * 360f * sweepAnim.value
        val fatSweep = (macros.avgFat / total) * 360f * sweepAnim.value

        var startAngle = -90f

        // Protein arc
        drawArc(
            color = ChartProtein,
            startAngle = startAngle,
            sweepAngle = proteinSweep,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        startAngle += proteinSweep

        // Carbs arc
        drawArc(
            color = ChartCarbs,
            startAngle = startAngle,
            sweepAngle = carbsSweep,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        startAngle += carbsSweep

        // Fat arc
        drawArc(
            color = ChartFat,
            startAngle = startAngle,
            sweepAngle = fatSweep,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun MacroLegendRow(
    color: Color,
    label: String,
    grams: Float,
    pct: Float
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextMain
                )
            )
            Text(
                text = "${"%.0f".format(grams)}g · ${"%.0f".format(pct * 100)}%",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TextSub,
                    fontSize = 10.sp
                )
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// WEEKLY AVERAGES TABLE
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun WeeklyAveragesCard(
    weeklyAverages: List<WeeklyAverage>,
    unitSystem: UnitSystem
) {
    GlassCard(cornerRadius = 28.dp) {
        SectionHeader(title = "Weekly Averages")

        Spacer(modifier = Modifier.height(12.dp))

        // Header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TableHeader("Week", Modifier.weight(1.2f))
            TableHeader("Avg Cal", Modifier.weight(1f))
            TableHeader("Avg Wt", Modifier.weight(1f))
            TableHeader("Δ Wt", Modifier.weight(0.8f))
        }

        // Divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.06f))
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Data rows (show most recent first)
        weeklyAverages.reversed().take(8).forEach { week ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = week.weekLabel,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium,
                        color = TextMain
                    ),
                    modifier = Modifier.weight(1.2f)
                )
                Text(
                    text = "${week.avgCalories}",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSub),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = week.avgWeight?.let {
                        "${"%.1f".format(unitSystem.fromKg(it))}"
                    } ?: "—",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSub),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                // Weight change delta
                val deltaText = week.weightChange?.let {
                    val display = unitSystem.fromKg(it)
                    val sign = if (display >= 0) "+" else ""
                    "$sign${"%.1f".format(display)}"
                } ?: "—"
                val deltaColor = when {
                    week.weightChange == null -> TextSub
                    week.weightChange < -0.01f -> AccentTeal
                    week.weightChange > 0.01f -> AccentOrange
                    else -> TextSub
                }
                Text(
                    text = deltaText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = deltaColor
                    ),
                    modifier = Modifier.weight(0.8f),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Composable
private fun TableHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            color = TextSub
        ),
        modifier = modifier,
        textAlign = if (text == "Week") TextAlign.Start else TextAlign.Center
    )
}

// ═══════════════════════════════════════════════════════════════════════════
// LEGEND DOT HELPER
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = TextSub,
                fontSize = 10.sp
            )
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SKELETON LOADING
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun AnalyticsSkeleton() {
    MeshBackground(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header skeleton
            Box(
                modifier = Modifier
                    .size(width = 120.dp, height = 32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .shimmer()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Date range selector skeleton
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .shimmer()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Chart skeletons
            repeat(3) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .shimmer()
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

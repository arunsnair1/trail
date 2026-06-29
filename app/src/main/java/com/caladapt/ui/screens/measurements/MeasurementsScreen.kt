package com.caladapt.ui.screens.measurements

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.caladapt.data.db.entity.BodyMeasurementEntity
import com.caladapt.domain.model.UnitSystem
import com.caladapt.ui.components.*
import com.caladapt.ui.theme.*

@Composable
fun MeasurementsScreen(
    viewModel: MeasurementsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.savedSuccessfully) {
        if (state.savedSuccessfully) {
            snackbarHostState.showSnackbar("Measurement logged! 📏")
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
            Column(modifier = Modifier.fillMaxSize()) {
                // Header + tabs
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Body Measurements",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextMain
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Tab row
                    MeasurementTabs(
                        selectedTab = state.currentTab,
                        onTabSelect = viewModel::selectTab
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Tab content
                AnimatedContent(
                    targetState = state.currentTab,
                    transitionSpec = {
                        fadeIn(tween(300)) togetherWith fadeOut(tween(200))
                    },
                    label = "tab_content"
                ) { tab ->
                    when (tab) {
                        MeasurementTab.LOG -> LogTabContent(
                            state = state,
                            viewModel = viewModel
                        )
                        MeasurementTab.HISTORY -> HistoryTabContent(
                            state = state,
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// TAB SELECTOR
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun MeasurementTabs(
    selectedTab: MeasurementTab,
    onTabSelect: (MeasurementTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GlassBg)
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        MeasurementTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab
            val bgAlpha by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0f,
                animationSpec = tween(200),
                label = "tab_bg_${tab.name}"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = bgAlpha * 0.10f))
                    .clickableNoRipple { onTabSelect(tab) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (tab == MeasurementTab.LOG) Icons.Outlined.Edit
                        else Icons.Outlined.Timeline,
                        contentDescription = null,
                        tint = if (isSelected) AccentRed else TextSub,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (tab == MeasurementTab.LOG) "Log" else "History",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) AccentRed else TextSub
                        )
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// LOG TAB
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun LogTabContent(
    state: MeasurementsState,
    viewModel: MeasurementsViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        // Unit toggle
        Box(modifier = Modifier.staggeredEntrance(0)) {
            UnitToggleChip(
                unitSystem = state.unitSystem,
                onToggle = viewModel::toggleUnit
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Torso group
        Box(modifier = Modifier.staggeredEntrance(1)) {
            MeasurementGroupCard(
                title = "TORSO",
                icon = Icons.Outlined.Accessibility,
                fields = listOf(
                    MeasurementField("Chest", state.chestInput, viewModel::updateChest),
                    MeasurementField("Waist", state.waistInput, viewModel::updateWaist),
                    MeasurementField("Hips", state.hipsInput, viewModel::updateHips)
                ),
                unitLabel = state.lengthLabel,
                deltas = state.deltas().filter { it.label in listOf("Chest", "Waist", "Hips") }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Arms group
        Box(modifier = Modifier.staggeredEntrance(2)) {
            MeasurementGroupCard(
                title = "ARMS",
                icon = Icons.Outlined.FitnessCenter,
                fields = listOf(
                    MeasurementField("L. Bicep", state.leftBicepInput, viewModel::updateLeftBicep),
                    MeasurementField("R. Bicep", state.rightBicepInput, viewModel::updateRightBicep),
                    MeasurementField("Forearm", state.forearmInput, viewModel::updateForearm)
                ),
                unitLabel = state.lengthLabel,
                deltas = state.deltas().filter { it.label in listOf("L. Bicep", "R. Bicep", "Forearm") }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Legs group
        Box(modifier = Modifier.staggeredEntrance(3)) {
            MeasurementGroupCard(
                title = "LEGS",
                icon = Icons.Outlined.DirectionsWalk,
                fields = listOf(
                    MeasurementField("L. Thigh", state.leftThighInput, viewModel::updateLeftThigh),
                    MeasurementField("R. Thigh", state.rightThighInput, viewModel::updateRightThigh),
                    MeasurementField("Calf", state.calfInput, viewModel::updateCalf)
                ),
                unitLabel = state.lengthLabel,
                deltas = state.deltas().filter { it.label in listOf("L. Thigh", "R. Thigh", "Calf") }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Neck
        Box(modifier = Modifier.staggeredEntrance(4)) {
            MeasurementGroupCard(
                title = "NECK",
                icon = Icons.Outlined.Person,
                fields = listOf(
                    MeasurementField("Neck", state.neckInput, viewModel::updateNeck)
                ),
                unitLabel = state.lengthLabel,
                deltas = state.deltas().filter { it.label == "Neck" }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Body fat section
        Box(modifier = Modifier.staggeredEntrance(5)) {
            BodyFatCard(
                autoBodyFat = state.autoBodyFat,
                manualInput = state.bodyFatInput,
                onManualChange = viewModel::updateBodyFat,
                hasRequiredInputs = state.waistInput.isNotBlank() && state.neckInput.isNotBlank()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Notes
        Box(modifier = Modifier.staggeredEntrance(6)) {
            GlassCard(cornerRadius = 28.dp) {
                SectionHeader(title = "Notes")
                Spacer(modifier = Modifier.height(8.dp))
                GlassTextField(
                    value = state.notesInput,
                    onValueChange = viewModel::updateNotes,
                    singleLine = false,
                    label = { Text("Optional notes") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Save button
        Box(modifier = Modifier.staggeredEntrance(7)) {
            GlassAccentButton(
                onClick = viewModel::saveMeasurement,
                enabled = state.hasAnyInput && !state.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Save Measurement",
                        fontWeight = FontWeight.Bold
                    )
                }
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

        // Bottom padding for floating nav
        Spacer(modifier = Modifier.height(120.dp))
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// HISTORY TAB
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun HistoryTabContent(
    state: MeasurementsState,
    viewModel: MeasurementsViewModel
) {
    if (state.history.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            PremiumEmptyState(
                icon = Icons.Default.Straighten,
                title = "No Measurements Yet",
                subtitle = "Log your first body measurement to see trends and progress here.",
                modifier = Modifier.padding(vertical = 48.dp)
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 0.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Summary card
        item {
            Box(modifier = Modifier.staggeredEntrance(0)) {
                MeasurementSummaryCard(
                    totalCount = state.history.size,
                    latestDate = state.history.firstOrNull()?.date ?: "--",
                    latestBodyFat = state.history.firstOrNull()?.bodyFatPct
                )
            }
        }

        // Waist trend chart (most tracked measurement)
        val waistData = state.history
            .filter { it.waistCm != null }
            .sortedBy { it.date }
            .takeLast(20)

        if (waistData.size >= 2) {
            item {
                Box(modifier = Modifier.staggeredEntrance(1)) {
                    TrendChartCard(
                        title = "Waist Trend",
                        data = waistData.map { it.waistCm!! },
                        unitSystem = state.unitSystem,
                        color = AccentRed
                    )
                }
            }
        }

        // Body fat trend chart
        val bfData = state.history
            .filter { it.bodyFatPct != null }
            .sortedBy { it.date }
            .takeLast(20)

        if (bfData.size >= 2) {
            item {
                Box(modifier = Modifier.staggeredEntrance(2)) {
                    TrendChartCard(
                        title = "Body Fat % Trend",
                        data = bfData.map { it.bodyFatPct!! },
                        unitSystem = null, // Already in %
                        color = AccentTeal,
                        suffix = "%"
                    )
                }
            }
        }

        // Measurement log list
        item {
            Box(modifier = Modifier.staggeredEntrance(3)) {
                SectionHeader(title = "All Measurements")
            }
        }

        itemsIndexed(
            items = state.history,
            key = { _, item -> item.id }
        ) { index, measurement ->
            Box(modifier = Modifier.staggeredEntrance(4 + index.coerceAtMost(6))) {
                MeasurementHistoryItem(
                    measurement = measurement,
                    unitSystem = state.unitSystem,
                    onDelete = { viewModel.deleteMeasurement(measurement) }
                )
            }
        }

        // Bottom spacing for nav bar
        item { Spacer(modifier = Modifier.height(100.dp)) }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SUBCOMPONENTS
// ═══════════════════════════════════════════════════════════════════════════

private data class MeasurementField(
    val label: String,
    val value: String,
    val onValueChange: (String) -> Unit
)

@Composable
private fun MeasurementGroupCard(
    title: String,
    icon: ImageVector,
    fields: List<MeasurementField>,
    unitLabel: String,
    deltas: List<MeasurementDelta>
) {
    GlassCard(cornerRadius = 28.dp) {
        // Group header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AccentRed.copy(alpha = 0.8f),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = TextMain
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Input fields
        fields.forEach { field ->
            val delta = deltas.find { it.label == field.label }
            MeasurementInputRow(
                label = field.label,
                value = field.value,
                onValueChange = field.onValueChange,
                unitLabel = unitLabel,
                delta = delta
            )
            if (field != fields.last()) {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun MeasurementInputRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    unitLabel: String,
    delta: MeasurementDelta?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Label
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                color = TextMain
            ),
            modifier = Modifier.width(72.dp)
        )

        // Input
        GlassTextField(
            value = value,
            onValueChange = onValueChange,
            suffix = {
                Text(
                    unitLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSub
                )
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier.weight(1f)
        )

        // Delta badge
        val deltaDisplay = delta?.deltaDisplay
        if (deltaDisplay != null) {
            DeltaBadge(
                text = deltaDisplay,
                isPositive = delta.isPositiveDelta
            )
        } else {
            Spacer(modifier = Modifier.width(56.dp))
        }
    }
}

@Composable
private fun DeltaBadge(
    text: String,
    isPositive: Boolean
) {
    // Animate badge entrance
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "delta_scale"
    )

    val bgColor = if (isPositive) AccentYellow.copy(alpha = 0.15f) else AccentTeal.copy(alpha = 0.15f)
    val textColor = if (isPositive) AccentYellow else AccentTeal

    Box(
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 6.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = textColor
            )
        )
    }
}

@Composable
private fun UnitToggleChip(
    unitSystem: UnitSystem,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        TextButton(
            onClick = onToggle,
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
                text = unitSystem.lengthLabel.uppercase(),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = AccentRed
                )
            )
        }
    }
}

@Composable
private fun BodyFatCard(
    autoBodyFat: Float?,
    manualInput: String,
    onManualChange: (String) -> Unit,
    hasRequiredInputs: Boolean
) {
    GlassCard(cornerRadius = 28.dp) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.MonitorWeight,
                contentDescription = null,
                tint = AccentTeal.copy(alpha = 0.8f),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "BODY FAT",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = TextMain
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Navy method result
        if (autoBodyFat != null) {
            GlassHeroCard(
                gradientStart = AccentTeal.copy(alpha = 0.8f),
                gradientMid = AccentTeal,
                gradientEnd = AccentBlue.copy(alpha = 0.7f),
                cornerRadius = 20.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Navy Method",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White.copy(alpha = 0.8f),
                                letterSpacing = 1.sp
                            )
                        )
                        Text(
                            text = "${"%.1f".format(autoBodyFat)}%",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.Calculate,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        } else if (!hasRequiredInputs) {
            Text(
                text = "Enter waist + neck measurements to auto-calculate body fat via the U.S. Navy method",
                style = MaterialTheme.typography.bodySmall,
                color = TextSub
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Manual override
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Manual",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = TextMain
                ),
                modifier = Modifier.width(72.dp)
            )
            GlassTextField(
                value = manualInput,
                onValueChange = onManualChange,
                suffix = {
                    Text(
                        "%",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSub
                    )
                },
                label = { Text("Override") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MeasurementSummaryCard(
    totalCount: Int,
    latestDate: String,
    latestBodyFat: Float?
) {
    GlassHeroCard(cornerRadius = 28.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "MEASUREMENT LOG",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$totalCount entries",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                )
                Text(
                    text = "Last: $latestDate",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White.copy(alpha = 0.7f)
                    )
                )
            }

            if (latestBodyFat != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "BODY FAT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    )
                    Text(
                        text = "${"%.1f".format(latestBodyFat)}%",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun TrendChartCard(
    title: String,
    data: List<Float>,
    unitSystem: UnitSystem?,
    color: Color,
    suffix: String = unitSystem?.lengthLabel ?: ""
) {
    GlassCard(cornerRadius = 28.dp) {
        SectionHeader(title = title)

        val latest = data.lastOrNull() ?: 0f
        val displayLatest = if (unitSystem != null) unitSystem.fromCm(latest) else latest
        Text(
            text = "Current: ${"%.1f".format(displayLatest)} $suffix",
            style = MaterialTheme.typography.labelSmall,
            color = TextSub
        )

        Spacer(modifier = Modifier.height(12.dp))

        MiniTrendChart(
            data = data.map { if (unitSystem != null) unitSystem.fromCm(it) else it },
            color = color,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        )
    }
}

@Composable
private fun MiniTrendChart(
    data: List<Float>,
    color: Color,
    modifier: Modifier = Modifier
) {
    if (data.size < 2) return

    val revealAnim = remember { Animatable(0f) }
    LaunchedEffect(data) {
        revealAnim.snapTo(0f)
        revealAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(1000, easing = FastOutSlowInEasing)
        )
    }

    Canvas(modifier = modifier) {
        val minVal = data.min() - 0.5f
        val maxVal = data.max() + 0.5f
        val range = (maxVal - minVal).coerceAtLeast(0.1f)

        val stepX = size.width / (data.size - 1)
        val paddingY = 8f

        fun yPos(value: Float): Float =
            size.height - paddingY - ((value - minVal) / range * (size.height - 2 * paddingY))

        clipRect(
            left = 0f,
            top = 0f,
            right = size.width * revealAnim.value,
            bottom = size.height
        ) {
            // Draw line
            val path = Path()
            data.forEachIndexed { index, value ->
                val x = index * stepX
                val y = yPos(value)
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(
                path = path,
                color = color,
                style = Stroke(width = 3f, cap = StrokeCap.Round)
            )

            // Draw dots
            data.forEachIndexed { index, value ->
                val x = index * stepX
                val y = yPos(value)
                drawCircle(
                    color = color,
                    radius = 4f,
                    center = Offset(x, y)
                )
            }
        }
    }
}

@Composable
private fun MeasurementHistoryItem(
    measurement: BodyMeasurementEntity,
    unitSystem: UnitSystem,
    onDelete: () -> Unit
) {
    GlassCard(cornerRadius = 24.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = measurement.date,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextMain
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Show non-null measurements in a flow
                val items = buildList {
                    measurement.waistCm?.let { add("Waist: ${"%.1f".format(unitSystem.fromCm(it))}") }
                    measurement.chestCm?.let { add("Chest: ${"%.1f".format(unitSystem.fromCm(it))}") }
                    measurement.hipsCm?.let { add("Hips: ${"%.1f".format(unitSystem.fromCm(it))}") }
                    measurement.neckCm?.let { add("Neck: ${"%.1f".format(unitSystem.fromCm(it))}") }
                    measurement.leftBicepCm?.let { add("L.Bi: ${"%.1f".format(unitSystem.fromCm(it))}") }
                    measurement.rightBicepCm?.let { add("R.Bi: ${"%.1f".format(unitSystem.fromCm(it))}") }
                    measurement.leftThighCm?.let { add("L.Th: ${"%.1f".format(unitSystem.fromCm(it))}") }
                    measurement.rightThighCm?.let { add("R.Th: ${"%.1f".format(unitSystem.fromCm(it))}") }
                    measurement.forearmCm?.let { add("Forearm: ${"%.1f".format(unitSystem.fromCm(it))}") }
                    measurement.calfCm?.let { add("Calf: ${"%.1f".format(unitSystem.fromCm(it))}") }
                }

                if (items.isNotEmpty()) {
                    Text(
                        text = items.joinToString(" · ") + " ${unitSystem.lengthLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSub,
                        maxLines = 2
                    )
                }

                measurement.bodyFatPct?.let { bf ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(AccentTeal, CircleShape)
                        )
                        Text(
                            text = "BF: ${"%.1f".format(bf)}%",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = AccentTeal
                            )
                        )
                    }
                }

                if (measurement.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = measurement.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSub.copy(alpha = 0.7f),
                        maxLines = 1
                    )
                }
            }

            // Delete button
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Delete",
                    tint = TextSub.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// UTILITY — clickable without ripple (for tab)
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier {
    return this.then(
        Modifier.clickable(
            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    )
}

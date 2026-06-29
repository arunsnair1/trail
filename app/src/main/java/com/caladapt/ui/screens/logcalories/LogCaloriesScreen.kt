package com.caladapt.ui.screens.logcalories

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.caladapt.data.db.entity.CalorieLogEntity
import com.caladapt.ui.components.*
import com.caladapt.ui.theme.*

@Composable
fun LogCaloriesScreen(
    viewModel: LogCaloriesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Show success snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.savedSuccessfully) {
        if (state.savedSuccessfully) {
            snackbarHostState.showSnackbar("Food logged! ✅")
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }

                // Header
                item {
                    Text(
                        text = "Log Food",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextMain
                    )
                }

                // Running total card — hero style
                item {
                    Box(modifier = Modifier.staggeredEntrance(0)) {
                        RunningTotalCard(
                            todayTotal = state.todayTotal,
                            target = state.calorieTarget,
                            remaining = state.caloriesRemaining
                        )
                    }
                }

                // Input form card
                item {
                    Box(modifier = Modifier.staggeredEntrance(1)) {
                        InputFormCard(
                            state = state,
                            onCaloriesChange = viewModel::updateCalories,
                            onProteinChange = viewModel::updateProtein,
                            onCarbsChange = viewModel::updateCarbs,
                            onFatChange = viewModel::updateFat,
                            onMealTypeChange = viewModel::updateMealType,
                            onNoteChange = viewModel::updateNote,
                            onSubmit = viewModel::logEntry
                        )
                    }
                }

                // Today's log header
                item {
                    SectionHeader(
                        title = "Today's Log",
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                if (state.todayLogs.isNotEmpty()) {
                    items(
                        items = state.todayLogs,
                        key = { it.id }
                    ) { entry ->
                        Box(modifier = Modifier.staggeredEntrance(2)) {
                            FoodLogItem(
                                entry = entry,
                                onDelete = { viewModel.deleteEntry(it) }
                            )
                        }
                    }
                } else {
                    item {
                        Box(modifier = Modifier.staggeredEntrance(2)) {
                            PremiumEmptyState(
                                icon = Icons.Default.Restaurant,
                                title = "No Meals Logged",
                                subtitle = "Start logging your meals to stay on track with your goals.",
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        }
                    }
                }

                // Bottom padding for floating nav
                item { Spacer(modifier = Modifier.height(120.dp)) }
            }
        }
    }
}

@Composable
private fun RunningTotalCard(
    todayTotal: Int,
    target: Int,
    remaining: Int
) {
    GlassHeroCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TotalColumn(label = "CONSUMED", value = "$todayTotal", color = Color.White)
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(40.dp)
                    .background(Color.White.copy(alpha = 0.4f))
            )
            TotalColumn(label = "TARGET", value = "$target", color = Color.White.copy(alpha = 0.8f))
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(40.dp)
                    .background(Color.White.copy(alpha = 0.4f))
            )
            TotalColumn(
                label = if (remaining >= 0) "REMAINING" else "OVER",
                value = "${kotlin.math.abs(remaining)}",
                color = Color.White
            )
        }
    }
}

@Composable
private fun TotalColumn(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            ),
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            ),
            color = color.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun InputFormCard(
    state: LogCaloriesState,
    onCaloriesChange: (String) -> Unit,
    onProteinChange: (String) -> Unit,
    onCarbsChange: (String) -> Unit,
    onFatChange: (String) -> Unit,
    onMealTypeChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    GlassCard {
        // Meal type chips
        Text(
            text = "MEAL TYPE",
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = TextMain
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            state.mealTypes.forEach { meal ->
                FilterChip(
                    selected = state.selectedMealType == meal,
                    onClick = { onMealTypeChange(meal) },
                    label = {
                        Text(
                            meal.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color.White.copy(alpha = 0.06f),
                        selectedContainerColor = AccentRed.copy(alpha = 0.15f),
                        selectedLabelColor = AccentRed,
                        labelColor = TextSub
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = GlassBorder,
                        selectedBorderColor = AccentRed.copy(alpha = 0.3f),
                        enabled = true,
                        selected = state.selectedMealType == meal
                    ),
                    shape = RoundedCornerShape(999.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Calories (required)
        GlassTextField(
            value = state.calories,
            onValueChange = onCaloriesChange,
            label = { Text("Calories *") },
            textStyle = MaterialTheme.typography.displayLarge.copy(
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = TextMain
            ),
            suffix = { Text("kcal") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Optional macros row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GlassTextField(
                value = state.proteinG,
                onValueChange = onProteinChange,
                label = { Text("Protein") },
                suffix = { Text("g") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.weight(1f)
            )
            GlassTextField(
                value = state.carbsG,
                onValueChange = onCarbsChange,
                label = { Text("Carbs") },
                suffix = { Text("g") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.weight(1f)
            )
            GlassTextField(
                value = state.fatG,
                onValueChange = onFatChange,
                label = { Text("Fat") },
                suffix = { Text("g") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Note
        GlassTextField(
            value = state.note,
            onValueChange = onNoteChange,
            label = { Text("Note (optional)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Submit button
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
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log Food", fontWeight = FontWeight.Bold)
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

@Composable
private fun FoodLogItem(
    entry: CalorieLogEntity,
    onDelete: (CalorieLogEntity) -> Unit
) {
    GlassListItem {
        // Icon container
        GlassIconContainer(
            icon = Icons.Default.Restaurant,
            contentDescription = null,
            tint = AccentRed
        )

        // Content
        Column(modifier = Modifier.weight(1f)) {
            Row {
                Text(
                    text = "${entry.calories} kcal",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = TextMain
                )
                if (entry.timeOfDay.isNotEmpty()) {
                    Text(
                        text = " · ${entry.timeOfDay}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSub
                    )
                }
            }
            if (entry.proteinG > 0 || entry.carbsG > 0 || entry.fatG > 0) {
                Text(
                    text = "P: ${entry.proteinG.toInt()}g · C: ${entry.carbsG.toInt()}g · F: ${entry.fatG.toInt()}g",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSub
                )
            }
            if (entry.note.isNotEmpty()) {
                Text(
                    text = entry.note,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSub.copy(alpha = 0.7f)
                )
            }
        }

        // Delete button
        IconButton(
            onClick = { onDelete(entry) },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete",
                tint = Error.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

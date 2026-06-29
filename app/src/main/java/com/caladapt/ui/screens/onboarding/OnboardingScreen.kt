package com.caladapt.ui.screens.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.caladapt.domain.model.Sex
import com.caladapt.domain.model.UnitSystem
import com.caladapt.ui.components.*
import com.caladapt.ui.theme.*

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.isComplete) {
        if (state.isComplete) onComplete()
    }

    MeshBackground(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Progress indicator
            StepProgressBar(
                currentStep = state.currentStep,
                totalSteps = state.totalSteps,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Step content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = state.currentStep,
                    transitionSpec = {
                        if (targetState > initialState) {
                            slideInHorizontally(
                                animationSpec = tween(400),
                                initialOffsetX = { it }
                            ) togetherWith slideOutHorizontally(
                                animationSpec = tween(400),
                                targetOffsetX = { -it }
                            )
                        } else {
                            slideInHorizontally(
                                animationSpec = tween(400),
                                initialOffsetX = { -it }
                            ) togetherWith slideOutHorizontally(
                                animationSpec = tween(400),
                                targetOffsetX = { it }
                            )
                        }
                    },
                    label = "onboarding_step"
                ) { step ->
                    when (step) {
                        0 -> WelcomeStep(
                            name = state.name,
                            onNameChange = viewModel::updateName
                        )
                        1 -> SexSelectionStep(
                            selectedSex = state.sex,
                            onSexChange = viewModel::updateSex
                        )
                        2 -> MeasurementsStep(
                            age = state.age,
                            height = state.heightValue,
                            weight = state.weightValue,
                            unitSystem = state.unitSystem,
                            onAgeChange = viewModel::updateAge,
                            onHeightChange = viewModel::updateHeight,
                            onWeightChange = viewModel::updateWeight,
                            onUnitSystemChange = viewModel::updateUnitSystem
                        )
                        3 -> SummaryStep(state = state)
                    }
                }
            }

            // Error
            state.error?.let { error ->
                Text(
                    text = error,
                    color = Error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Navigation buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (state.currentStep > 0) {
                    GlassOutlinedButton(
                        onClick = viewModel::previousStep
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Back", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                if (state.currentStep < state.totalSteps - 1) {
                    GlassAccentButton(
                        onClick = viewModel::nextStep,
                        enabled = state.isCurrentStepValid
                    ) {
                        Text("Next", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = "Next")
                    }
                } else {
                    val transition = rememberInfiniteTransition(label = "pulse")
                    val pulseScale by transition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.05f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulse_anim"
                    )

                    GlassAccentButton(
                        onClick = viewModel::completeOnboarding,
                        enabled = state.isCurrentStepValid && !state.isLoading,
                        modifier = Modifier.graphicsLayer {
                            if (state.isCurrentStepValid && !state.isLoading) {
                                scaleX = pulseScale
                                scaleY = pulseScale
                            }
                        }
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.RocketLaunch, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start Journey", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepProgressBar(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(totalSteps) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (index <= currentStep) {
                            Brush.horizontalGradient(listOf(AccentRed, AccentOrange))
                        } else {
                            Brush.horizontalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.08f),
                                    Color.White.copy(alpha = 0.08f)
                                )
                            )
                        }
                    )
            )
        }
    }
}

@Composable
private fun WelcomeStep(
    name: String,
    onNameChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Welcome to",
            style = MaterialTheme.typography.headlineMedium,
            color = TextMain
        )
        Text(
            text = "CalAdapt",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = AccentRed
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "The adaptive calorie tracker that\nlearns your metabolism",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = TextSub
        )
        Spacer(modifier = Modifier.height(48.dp))

        GlassTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("What's your name?") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SexSelectionStep(
    selectedSex: Sex,
    onSexChange: (Sex) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Biological Sex",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = TextMain
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Used for BMR calculation (metabolic variable,\nnot gender identity)",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = TextSub
        )
        Spacer(modifier = Modifier.height(40.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            SexCard(
                label = "Male",
                icon = Icons.Default.Male,
                isSelected = selectedSex == Sex.MALE,
                onClick = { onSexChange(Sex.MALE) }
            )
            SexCard(
                label = "Female",
                icon = Icons.Default.Female,
                isSelected = selectedSex == Sex.FEMALE,
                onClick = { onSexChange(Sex.FEMALE) }
            )
        }
    }
}

@Composable
private fun SexCard(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) AccentRed else GlassBorder
    val bgColor = if (isSelected) AccentRed.copy(alpha = 0.1f) else GlassBg

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "sex_card_scale"
    )

    Column(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = if (isSelected) 12.dp else 4.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = Color.Black.copy(alpha = 0.05f)
            )
            .clip(RoundedCornerShape(28.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(28.dp)
            )
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) AccentRed.copy(alpha = 0.15f)
                    else Color.White.copy(alpha = 0.06f)
                )
                .border(
                    1.dp,
                    Color.White.copy(alpha = 0.10f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) AccentRed else TextSub,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) AccentRed else TextMain
        )
        if (isSelected) {
            Spacer(modifier = Modifier.height(4.dp))
            Icon(
                Icons.Default.Check,
                contentDescription = "Selected",
                tint = AccentRed,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun MeasurementsStep(
    age: String,
    height: String,
    weight: String,
    unitSystem: UnitSystem,
    onAgeChange: (String) -> Unit,
    onHeightChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onUnitSystemChange: (UnitSystem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Your Measurements",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = TextMain
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "We need these to estimate your starting calorie target",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = TextSub
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Unit system toggle — glass styled
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White.copy(alpha = 0.06f))
                .border(1.dp, GlassBorder, RoundedCornerShape(999.dp)),
            horizontalArrangement = Arrangement.Center
        ) {
            UnitSystem.entries.forEach { unit ->
                val isSelected = unitSystem == unit
                Text(
                    text = if (unit == UnitSystem.METRIC) "Metric (kg/cm)" else "Imperial (lbs/in)",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        letterSpacing = if (isSelected) 0.5.sp else 0.3.sp
                    ),
                    color = if (isSelected) Color.White else TextSub,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            if (isSelected) AccentRed else Color.Transparent
                        )
                        .clickable { onUnitSystemChange(unit) }
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Age
        GlassTextField(
            value = age,
            onValueChange = onAgeChange,
            label = { Text("Age") },
            suffix = { Text("years") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Height
        GlassTextField(
            value = height,
            onValueChange = onHeightChange,
            label = { Text("Height") },
            suffix = { Text(unitSystem.lengthLabel) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Weight
        GlassTextField(
            value = weight,
            onValueChange = onWeightChange,
            label = { Text("Current Weight") },
            suffix = { Text(unitSystem.weightLabel) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SummaryStep(state: OnboardingState) {
    val unitSystem = state.unitSystem

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Ready to Go! 🚀",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = TextMain
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Here's what we'll start with",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSub
        )
        Spacer(modifier = Modifier.height(32.dp))

        // Summary card
        GlassCard(cornerRadius = 28.dp) {
            SummaryRow("Name", state.name)
            SummaryRow("Sex", state.sex.name.lowercase().replaceFirstChar { it.uppercase() })
            SummaryRow("Age", "${state.age} years")
            SummaryRow("Height", "${state.heightValue} ${unitSystem.lengthLabel}")
            SummaryRow("Weight", "${state.weightValue} ${unitSystem.weightLabel}")
            SummaryRow("Units", if (unitSystem == UnitSystem.METRIC) "Metric" else "Imperial")
        }

        Spacer(modifier = Modifier.height(24.dp))

        GlassHeroCard(
            gradientStart = AccentTeal.copy(alpha = 0.7f),
            gradientEnd = AccentTeal.copy(alpha = 0.4f)
        ) {
            Text(
                text = "📊 DISCOVERY PHASE",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = Color.White
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "We'll start by finding your true maintenance calories. " +
                        "Log your weight and food daily — CalAdapt will learn your metabolism " +
                        "and calculate your real TDEE within 2-4 weeks.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            ),
            color = TextSub
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = TextMain
        )
    }
}

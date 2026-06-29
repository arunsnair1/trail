package com.caladapt.ui.screens.weeklyreview

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.caladapt.domain.usecase.ReviewStatus
import com.caladapt.domain.usecase.WeeklyReview
import com.caladapt.ui.components.GlassCard
import com.caladapt.ui.components.MeshBackground
import com.caladapt.ui.theme.AccentRed
import com.caladapt.ui.theme.AccentTeal
import com.caladapt.ui.theme.AccentOrange
import com.caladapt.ui.theme.TextMain
import com.caladapt.ui.theme.TextSub

@Composable
fun WeeklyReviewScreen(
    onReviewComplete: () -> Unit,
    viewModel: WeeklyReviewViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val review = state.review

    BackHandler {
        if (state.currentStep > 0) {
            viewModel.previousStep()
        } else {
            onReviewComplete()
        }
    }

    if (state.isLoading || review == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AccentRed)
        }
        return
    }

    MeshBackground(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Text(
                text = "Weekly Review",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextMain
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (state.currentStep + 1) / 3f },
                modifier = Modifier.fillMaxWidth(),
                color = AccentRed,
                trackColor = Color.White.copy(alpha = 0.1f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            AnimatedContent(
                targetState = state.currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { width -> width } + fadeIn(tween(300))).togetherWith(
                            slideOutHorizontally { width -> -width } + fadeOut(tween(300)))
                    } else {
                        (slideInHorizontally { width -> -width } + fadeIn(tween(300))).togetherWith(
                            slideOutHorizontally { width -> width } + fadeOut(tween(300)))
                    }
                }, label = "review_steps"
            ) { step ->
                when (step) {
                    0 -> RecapStep(review)
                    1 -> MetabolismStep(review)
                    2 -> NewPlanStep(review)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (state.currentStep > 0) {
                    TextButton(onClick = { viewModel.previousStep() }) {
                        Text("Back", color = TextSub)
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Button(
                    onClick = {
                        if (state.currentStep == 2) onReviewComplete() else viewModel.nextStep()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                ) {
                    Text(if (state.currentStep == 2) "Accept Plan" else "Next")
                }
            }
        }
    }
}

@Composable
private fun RecapStep(review: WeeklyReview) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("Your Week in Review", style = MaterialTheme.typography.titleLarge, color = TextMain)
        Spacer(modifier = Modifier.height(24.dp))
        GlassCard {
            Column(modifier = Modifier.fillMaxWidth()) {
                val compliancePct = (review.compliance * 100).toInt()
                val color = if (compliancePct >= 80) AccentTeal else AccentOrange
                
                Text("Logging Compliance", color = TextSub, fontSize = 14.sp)
                Text("$compliancePct%", color = color, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Average Daily Intake", color = TextSub, fontSize = 14.sp)
                Text("${review.avgCalories.toInt()} kcal", color = TextMain, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                
                if (review.weightChangeKgPerWeek != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Weight Change", color = TextSub, fontSize = 14.sp)
                    val dir = if (review.weightChangeKgPerWeek > 0) "+" else ""
                    Text("$dir${"%.2f".format(review.weightChangeKgPerWeek)} kg", color = TextMain, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun MetabolismStep(review: WeeklyReview) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("Metabolism Update", style = MaterialTheme.typography.titleLarge, color = TextMain)
        Spacer(modifier = Modifier.height(24.dp))
        GlassCard {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Info, contentDescription = null, tint = AccentTeal, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Estimated TDEE", color = TextSub, fontSize = 14.sp)
                Text("${review.tdeeEstimate.toInt()} kcal", color = AccentTeal, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "This is how many calories you burn per day on average, calculated based on your recent weight and calorie logs.",
                    color = TextSub,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun NewPlanStep(review: WeeklyReview) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("Your New Plan", style = MaterialTheme.typography.titleLarge, color = TextMain)
        Spacer(modifier = Modifier.height(24.dp))
        
        GlassCard {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = if (review.status == ReviewStatus.POOR_COMPLIANCE || review.status == ReviewStatus.INSUFFICIENT_DATA) Icons.Default.Warning else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (review.status == ReviewStatus.POOR_COMPLIANCE || review.status == ReviewStatus.INSUFFICIENT_DATA) AccentOrange else AccentRed,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Daily Target", color = TextSub, fontSize = 14.sp)
                Text("${review.newCalorieTarget} kcal", color = TextMain, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = review.message,
                    color = TextSub,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        if (review.warnings.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            review.warnings.forEach { warning ->
                Text(
                    text = warning,
                    color = AccentOrange,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
    }
}

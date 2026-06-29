package com.caladapt.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.caladapt.ui.theme.*

/**
 * Horizontal progress bar with gradient fill for macro tracking.
 * Shows consumed vs target with animated fill and value labels.
 *
 * Spec styling:
 *   Track: 5-6dp height, rgba(0,0,0,0.1), border-radius 10dp
 *   Fill:  1200ms cubic-bezier(.22,1,.36,1), subtle glow
 *   Labels: uppercase, letter-spacing 1.2sp
 */
@Composable
fun MacroProgressBar(
    label: String,
    consumed: Float,
    target: Float,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier,
    unit: String = "g"
) {
    val progress = if (target > 0) (consumed / target).coerceIn(0f, 1.2f) else 0f
    val displayProgress = progress.coerceAtMost(1f)
    val isOver = progress > 1f

    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(displayProgress) {
        animatedProgress.animateTo(
            targetValue = displayProgress,
            animationSpec = tween(
                durationMillis = 1200,
                easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
            )
        )
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = TextMain
                )
            )
            Text(
                text = "${consumed.toInt()}${unit} / ${target.toInt()}${unit}",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                ),
                color = if (isOver) Error else TextSub
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Black.copy(alpha = 0.1f))
        ) {
            // Fill
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = animatedProgress.value)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        brush = Brush.horizontalGradient(gradientColors)
                    )
                    .shadow(
                        elevation = 2.dp,
                        shape = RoundedCornerShape(10.dp),
                        ambientColor = gradientColors.first().copy(alpha = 0.5f)
                    )
            )
        }
    }
}

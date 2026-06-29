package com.caladapt.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.caladapt.ui.theme.*

/**
 * Animated circular progress indicator for daily calorie tracking.
 *
 * Shows consumed/target as a smooth arc with status-driven colour,
 * center text showing remaining calories, and labels.
 *
 * Ring colour logic (per spec):
 *   < 10% → white-ish  (just started)
 *   ≤ 100% → green     (OK zone)
 *   > 100% → red       (over limit)
 */
@Composable
fun CalorieRing(
    consumed: Int,
    target: Int,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    strokeWidth: Dp = 14.dp,
    trackColor: Color = Color.Black.copy(alpha = 0.1f)
) {
    val progress = if (target > 0) consumed.toFloat() / target else 0f
    val isOver = progress > 1f
    val displayProgress = progress.coerceAtMost(1f)

    // Status-driven ring colour
    val ringColor = when {
        progress < 0.10f -> RingStart
        progress <= 1.0f -> Success
        else -> Error
    }

    // Animate the progress arc — 1.5s ease-out per spec
    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(displayProgress) {
        animatedProgress.animateTo(
            targetValue = displayProgress,
            animationSpec = tween(
                durationMillis = 1500,
                easing = androidx.compose.animation.core.CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
            )
        )
    }

    val remaining = target - consumed

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val padding = strokePx / 2
            val arcSize = Size(
                this.size.width - strokePx,
                this.size.height - strokePx
            )
            val topLeft = Offset(padding, padding)

            // Background track
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Progress arc with gradient
            val sweepAngle = animatedProgress.value * 360f
            val gradientColors = if (isOver) {
                listOf(Error, Error.copy(alpha = 0.7f))
            } else {
                listOf(ringColor, ringColor.copy(alpha = 0.7f))
            }

            drawArc(
                brush = Brush.sweepGradient(gradientColors),
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }

        // Center text
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (remaining >= 0) "$remaining" else "+${-remaining}",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                ),
                color = if (isOver) Error else TextMain
            )
            Text(
                text = if (remaining >= 0) "remaining" else "over",
                style = MaterialTheme.typography.bodySmall,
                color = TextSub
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$consumed / $target kcal",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                ),
                color = TextSub
            )
        }
    }
}

package com.caladapt.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.caladapt.domain.model.Phase
import com.caladapt.ui.theme.*

/**
 * Colored chip/badge showing the current phase of the CalAdapt journey.
 * Each phase has a distinct gradient with glass-style border.
 *
 *   Discovery — Teal/Cyan
 *   Goal      — Red/Orange (accent)
 *   Maintenance — Green
 */
@Composable
fun PhaseBadge(
    phase: String,
    modifier: Modifier = Modifier
) {
    val (gradient, textColor) = when {
        phase == Phase.DISCOVERY.name || phase == "Discovery" -> {
            listOf(AccentTeal, AccentTeal.copy(alpha = 0.7f)) to Color.White
        }
        phase == Phase.GOAL.name || phase.contains("Cut") || phase.contains("Bulk") || phase.contains("Recomp") -> {
            listOf(AccentRed, AccentOrange) to Color.White
        }
        phase == Phase.MAINTENANCE.name || phase == "Maintenance" -> {
            listOf(Success, Success.copy(alpha = 0.7f)) to Color.White
        }
        else -> {
            listOf(AccentBlue, AccentBlue.copy(alpha = 0.7f)) to Color.White
        }
    }

    Text(
        text = phase.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            color = textColor
        ),
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(brush = Brush.horizontalGradient(gradient))
            .border(
                1.dp,
                Color.White.copy(alpha = 0.4f),
                RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 14.dp, vertical = 6.dp)
    )
}

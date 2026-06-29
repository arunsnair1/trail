package com.caladapt.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.caladapt.ui.theme.*

// ═══════════════════════════════════════════════════════════════════════════
// MESH BACKGROUND — animated floating colour blobs
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun MeshBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mesh_motion")
    val drift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mesh_drift"
    )

    Box(modifier = modifier.background(AppBackground)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(AppBackgroundAlt, AppBackground, Color(0xFFFFEFE8))
                )
            )
            drawOval(
                color = AccentRed.copy(alpha = 0.18f),
                topLeft = Offset(-size.width * 0.35f + drift * 48f, -size.height * 0.12f + drift * 36f),
                size = Size(size.width * 0.90f, size.width * 0.90f),
                style = Fill
            )
            drawOval(
                color = AccentTeal.copy(alpha = 0.18f),
                topLeft = Offset(size.width * 0.48f - drift * 52f, size.height * 0.18f + drift * 42f),
                size = Size(size.width * 0.82f, size.width * 0.82f),
                style = Fill
            )
            drawOval(
                color = AccentYellow.copy(alpha = 0.16f),
                topLeft = Offset(size.width * 0.05f + drift * 34f, size.height * 0.68f - drift * 28f),
                size = Size(size.width * 0.70f, size.width * 0.70f),
                style = Fill
            )
        }
        Box(
            modifier = Modifier.fillMaxSize(),
            content = content
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// GLASS CARD — standard translucent container
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 32.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 18.dp,
                shape = RoundedCornerShape(cornerRadius),
                ambientColor = ShadowLow,
                spotColor = ShadowHigh
            )
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(GlassBg, Color.White.copy(alpha = 0.58f)),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(GlassBorderStart, GlassBorderEnd),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
            .padding(20.dp),
        content = content
    )
}

// ═══════════════════════════════════════════════════════════════════════════
// GLASS HERO CARD — coloured gradient card for primary features
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun GlassHeroCard(
    modifier: Modifier = Modifier,
    gradientStart: Color = HeroGradientStart,
    gradientMid: Color = HeroGradientMid,
    gradientEnd: Color = HeroGradientEnd,
    cornerRadius: Dp = 32.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 20.dp,
                shape = RoundedCornerShape(cornerRadius),
                ambientColor = ShadowLow,
                spotColor = ShadowRed
            )
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(gradientStart, gradientMid, gradientEnd),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(HeroBorder, HeroBorder.copy(alpha = 0.2f)),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
            .padding(24.dp),
        content = content
    )
}

// ═══════════════════════════════════════════════════════════════════════════
// GLASS BUTTON — pill-shaped CTA with press animation
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(isPressed) {
        if (isPressed) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "btn_scale"
    )
    val yOffset by animateFloatAsState(
        targetValue = if (isPressed) 4f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "btn_y"
    )

    Button(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        enabled = enabled,
        modifier = modifier
            .height(52.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationY = yOffset
            },
        interactionSource = interactionSource,
        shape = RoundedCornerShape(999.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = GlassBg,
            contentColor = TextMain,
            disabledContainerColor = GlassBg.copy(alpha = 0.5f),
            disabledContentColor = TextSub
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 8.dp,
            pressedElevation = 2.dp,
            disabledElevation = 0.dp
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (enabled) GlassBorder else GlassBorder.copy(alpha = 0.3f)
        ),
        content = content
    )
}

// ═══════════════════════════════════════════════════════════════════════════
// GLASS ACCENT BUTTON — pill-shaped with accent color fill
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun GlassAccentButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accentColor: Color = AccentRed,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(isPressed) {
        if (isPressed) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "abtn_scale"
    )

    Button(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        enabled = enabled,
        modifier = modifier
            .height(52.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        interactionSource = interactionSource,
        shape = RoundedCornerShape(999.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = accentColor,
            contentColor = Color.White,
            disabledContainerColor = accentColor.copy(alpha = 0.4f),
            disabledContentColor = Color.White.copy(alpha = 0.6f)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 12.dp,
            pressedElevation = 4.dp
        ),
        content = content
    )
}

// ═══════════════════════════════════════════════════════════════════════════
// GLASS OUTLINED BUTTON — for secondary actions (Back, etc.)
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun GlassOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = tween(durationMillis = if (isPressed) 80 else 340),
        label = "obtn_scale"
    )

    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .height(52.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale },
        interactionSource = interactionSource,
        shape = RoundedCornerShape(999.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White.copy(alpha = 0.50f),
            contentColor = TextMain
        ),
        content = content
    )
}

// ═══════════════════════════════════════════════════════════════════════════
// GLASS ICON BUTTON — circular glass button for +/- nudge
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = tween(durationMillis = if (isPressed) 80 else 340),
        label = "ibtn_scale"
    )

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(8.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.1f), spotColor = Color.Black.copy(alpha = 0.15f))
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, GlassBorder, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = TextMain,
            modifier = Modifier.size(22.dp)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// GLASS TEXT FIELD — input with glass background + focus glow
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge.copy(
        fontWeight = FontWeight.SemiBold,
        color = TextMain
    ),
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        suffix = suffix,
        singleLine = singleLine,
        textStyle = textStyle,
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White.copy(alpha = 0.74f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.58f),
            focusedBorderColor = AccentRed.copy(alpha = 0.6f),
            unfocusedBorderColor = GlassBorder,
            focusedLabelColor = AccentRed,
            unfocusedLabelColor = TextSub,
            cursorColor = AccentRed
        ),
        modifier = modifier
    )
}

// ═══════════════════════════════════════════════════════════════════════════
// GLASS LIST ITEM — row item with glass styling
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun GlassListItem(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .shadow(4.dp, RoundedCornerShape(24.dp), ambientColor = Color.Black.copy(alpha = 0.05f))
            .background(GlassBg)
            .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick)
                else Modifier
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        content = content
    )
}

// ═══════════════════════════════════════════════════════════════════════════
// GLASS ICON CONTAINER — icon holder within list items
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun GlassIconContainer(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = AccentRed,
    backgroundColor: Color = tint.copy(alpha = 0.1f)
) {
    Box(
        modifier = modifier
            .size(52.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(backgroundColor)
            .border(
                1.dp,
                Color.White.copy(alpha = 0.10f),
                RoundedCornerShape(18.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SECTION HEADER — section title + optional action link
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = TextMain
            )
        )
        if (action != null && onAction != null) {
            Text(
                text = action,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = AccentRed
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(BadgeRedBg)
                    .clickable(onClick = onAction)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

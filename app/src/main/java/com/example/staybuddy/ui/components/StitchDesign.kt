package com.example.staybuddy.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A custom modifier that draws a "Stitched" dashed border around a component.
 * This is a core element of the StayBuddy "Connected Stitch" design language.
 */
fun Modifier.stitchedBorder(
    strokeWidth: Dp = 1.5.dp,
    color: Color = com.example.staybuddy.ui.theme.Evergreen,
    dashWidth: Dp = 8.dp,
    dashGap: Dp = 4.dp,
    cornerRadius: Dp = 12.dp
): Modifier = drawBehind {
    val stroke = Stroke(
        width = strokeWidth.toPx(),
        pathEffect = PathEffect.dashPathEffect(
            intervals = floatArrayOf(dashWidth.toPx(), dashGap.toPx()),
            phase = 0f
        )
    )
    
    drawRoundRect(
        color = color,
        style = stroke,
        cornerRadius = CornerRadius(cornerRadius.toPx())
    )
}

/**
 * A tactile sectional separator that uses the dashed stitch motif.
 */
fun Modifier.stitchedSeparator(
    color: Color = com.example.staybuddy.ui.theme.Evergreen.copy(alpha = 0.3f),
    strokeWidth: Dp = 1.dp,
    dashWidth: Dp = 6.dp,
    dashGap: Dp = 4.dp
): Modifier = drawBehind {
    val stroke = Stroke(
        width = strokeWidth.toPx(),
        pathEffect = PathEffect.dashPathEffect(
            intervals = floatArrayOf(dashWidth.toPx(), dashGap.toPx()),
            phase = 0f
        )
    )
    
    drawLine(
        color = color,
        start = androidx.compose.ui.geometry.Offset(0f, size.height / 2),
        end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2),
        strokeWidth = stroke.width,
        pathEffect = stroke.pathEffect
    )
}

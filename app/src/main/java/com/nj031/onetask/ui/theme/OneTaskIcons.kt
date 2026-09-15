package com.nj031.onetask.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * One Task's custom icon set - simple, rounded-stroke, navy/blue line icons shared by the
 * whole app (not a mix of these plus generic system icons). Each icon is drawn on a plain
 * Canvas rather than as an ImageVector so its proportions and stroke weight stay consistent
 * with each other at any requested size.
 */

@Composable
fun OneTaskHamburgerIcon(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = this.size.minDimension * 0.09f
        val xStart = this.size.width * 0.12f
        val xEnd = this.size.width * 0.88f
        listOf(0.22f, 0.5f, 0.78f).forEach { fraction ->
            val y = this.size.height * fraction
            drawLine(
                color = tint,
                start = Offset(xStart, y),
                end = Offset(xEnd, y),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun OneTaskCalendarIcon(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = this.size.minDimension * 0.08f
        val bodyTop = this.size.height * 0.2f
        val bodyBottom = this.size.height * 0.92f
        val bodyLeft = this.size.width * 0.08f
        val bodyRight = this.size.width * 0.92f
        val cornerRadius = CornerRadius(this.size.minDimension * 0.14f)

        drawRoundRect(
            color = tint,
            topLeft = Offset(bodyLeft, bodyTop),
            size = Size(bodyRight - bodyLeft, bodyBottom - bodyTop),
            cornerRadius = cornerRadius,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        val headerY = bodyTop + (bodyBottom - bodyTop) * 0.28f
        drawLine(
            color = tint,
            start = Offset(bodyLeft, headerY),
            end = Offset(bodyRight, headerY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        val tabTop = bodyTop - this.size.height * 0.08f
        val tabBottom = bodyTop + this.size.height * 0.06f
        listOf(0.3f, 0.7f).forEach { fraction ->
            val x = bodyLeft + (bodyRight - bodyLeft) * fraction
            drawLine(
                color = tint,
                start = Offset(x, tabTop),
                end = Offset(x, tabBottom),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }

        val dotRadius = this.size.minDimension * 0.045f
        listOf(0.52f, 0.74f).forEach { rowFraction ->
            listOf(0.28f, 0.5f, 0.72f).forEach { colFraction ->
                drawCircle(
                    color = tint,
                    radius = dotRadius,
                    center = Offset(
                        bodyLeft + (bodyRight - bodyLeft) * colFraction,
                        bodyTop + (bodyBottom - bodyTop) * rowFraction
                    )
                )
            }
        }
    }
}

@Composable
fun OneTaskAddIcon(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    plusColor: Color = Color.White,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = this.size.minDimension * 0.09f
        drawCircle(color = containerColor, radius = this.size.minDimension / 2f)

        val inset = this.size.minDimension * 0.28f
        val centerX = this.size.width / 2f
        val centerY = this.size.height / 2f
        drawLine(
            color = plusColor,
            start = Offset(centerX, inset),
            end = Offset(centerX, this.size.height - inset),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = plusColor,
            start = Offset(inset, centerY),
            end = Offset(this.size.width - inset, centerY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun OneTaskJournalIcon(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = this.size.minDimension * 0.09f
        val canvasHeight = this.size.height
        val centerX = this.size.width / 2f
        val top = canvasHeight * 0.22f
        val bottom = canvasHeight * 0.8f
        val leftX = this.size.width * 0.1f
        val rightX = this.size.width * 0.9f
        val bulge = canvasHeight * 0.04f
        val curveInset = canvasHeight * 0.1f

        drawLine(
            color = tint,
            start = Offset(centerX, top),
            end = Offset(centerX, bottom),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        val leftPage = Path().apply {
            moveTo(centerX, top)
            quadraticBezierTo(leftX, top - bulge, leftX, top + curveInset)
            lineTo(leftX, bottom - bulge)
            quadraticBezierTo(leftX, bottom, centerX, bottom - bulge)
        }
        drawPath(leftPage, color = tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))

        val rightPage = Path().apply {
            moveTo(centerX, top)
            quadraticBezierTo(rightX, top - bulge, rightX, top + curveInset)
            lineTo(rightX, bottom - bulge)
            quadraticBezierTo(rightX, bottom, centerX, bottom - bulge)
        }
        drawPath(rightPage, color = tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
    }
}

@Composable
fun OneTaskProfileIcon(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = this.size.minDimension * 0.09f
        val headRadius = this.size.minDimension * 0.19f
        val headCenter = Offset(this.size.width / 2f, this.size.height * 0.3f)
        drawCircle(color = tint, radius = headRadius, center = headCenter, style = Stroke(width = strokeWidth))

        val shoulderSize = Size(this.size.width * 0.7f, this.size.height * 0.62f)
        val shoulderTopLeft = Offset(
            (this.size.width - shoulderSize.width) / 2f,
            this.size.height * 0.98f - shoulderSize.height
        )
        drawArc(
            color = tint,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = shoulderTopLeft,
            size = shoulderSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun OneTaskTasksIcon(
    modifier: Modifier = Modifier,
    active: Boolean = true,
    size: Dp = 24.dp
) {
    val containerColor = if (active) MaterialTheme.colorScheme.primary else Color.Transparent
    val markColor = if (active) Color.White else MaterialTheme.colorScheme.primary
    val outlineColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = this.size.minDimension * 0.09f
            val cornerRadius = CornerRadius(this.size.minDimension * 0.28f)
            if (active) {
                drawRoundRect(color = containerColor, cornerRadius = cornerRadius)
            } else {
                drawRoundRect(
                    color = outlineColor,
                    cornerRadius = cornerRadius,
                    style = Stroke(width = strokeWidth)
                )
            }

            val checkX = this.size.width * 0.24f
            val lineStartX = this.size.width * 0.42f
            val lineEndX = this.size.width * 0.8f
            val tickSize = this.size.minDimension * 0.08f
            val markStroke = this.size.minDimension * 0.07f

            listOf(0.38f, 0.66f).forEach { rowFraction ->
                val y = this.size.height * rowFraction
                val tickPath = Path().apply {
                    moveTo(checkX - tickSize, y)
                    lineTo(checkX - tickSize * 0.2f, y + tickSize * 0.8f)
                    lineTo(checkX + tickSize, y - tickSize * 0.7f)
                }
                drawPath(
                    tickPath,
                    color = markColor,
                    style = Stroke(width = markStroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                drawLine(
                    color = markColor,
                    start = Offset(lineStartX, y),
                    end = Offset(lineEndX, y),
                    strokeWidth = markStroke,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

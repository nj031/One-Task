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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

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
    size: Dp = 24.dp,
    drawContainer: Boolean = true
) {
    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = this.size.minDimension * 0.09f
        if (drawContainer) {
            drawCircle(color = containerColor, radius = this.size.minDimension / 2f)
        }

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
fun OneTaskSearchIcon(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = this.size.minDimension * 0.09f
        val glassRadius = this.size.minDimension * 0.32f
        val glassCenter = Offset(this.size.width * 0.42f, this.size.height * 0.42f)
        drawCircle(
            color = tint,
            radius = glassRadius,
            center = glassCenter,
            style = Stroke(width = strokeWidth)
        )
        val handleStart = Offset(
            glassCenter.x + glassRadius * 0.75f,
            glassCenter.y + glassRadius * 0.75f
        )
        drawLine(
            color = tint,
            start = handleStart,
            end = Offset(this.size.width * 0.88f, this.size.height * 0.88f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

/** A checklist/list-of-rows glyph: a small square bullet beside a line for each of three rows -
 * used for the Notes screen's List View toggle option. */
@Composable
fun OneTaskListViewIcon(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = this.size.minDimension * 0.1f
        val bulletSize = this.size.minDimension * 0.12f
        val bulletX = this.size.width * 0.14f
        val lineStartX = this.size.width * 0.34f
        val lineEndX = this.size.width * 0.88f

        listOf(0.22f, 0.5f, 0.78f).forEach { fraction ->
            val y = this.size.height * fraction
            drawRect(
                color = tint,
                topLeft = Offset(bulletX - bulletSize / 2f, y - bulletSize / 2f),
                size = Size(bulletSize, bulletSize)
            )
            drawLine(
                color = tint,
                start = Offset(lineStartX, y),
                end = Offset(lineEndX, y),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}

/** A 2x2 grid of rounded tiles - used for the Notes screen's Card View toggle option. */
@Composable
fun OneTaskCardViewIcon(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val tileSize = this.size.width * 0.38f
        val gap = this.size.width * 0.14f
        val cornerRadius = CornerRadius(this.size.minDimension * 0.08f)
        val left = (this.size.width - tileSize * 2 - gap) / 2f
        val top = (this.size.height - tileSize * 2 - gap) / 2f

        listOf(0, 1).forEach { row ->
            listOf(0, 1).forEach { col ->
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(left + col * (tileSize + gap), top + row * (tileSize + gap)),
                    size = Size(tileSize, tileSize),
                    cornerRadius = cornerRadius
                )
            }
        }
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

@Composable
fun OneTaskArchiveIcon(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = this.size.minDimension * 0.08f
        val lidTop = this.size.height * 0.2f
        val lidBottom = this.size.height * 0.38f
        val left = this.size.width * 0.1f
        val right = this.size.width * 0.9f
        val bodyBottom = this.size.height * 0.82f

        drawRoundRect(
            color = tint,
            topLeft = Offset(left, lidTop),
            size = Size(right - left, lidBottom - lidTop),
            cornerRadius = CornerRadius(this.size.minDimension * 0.06f),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawLine(
            color = tint,
            start = Offset(left * 1.3f, lidBottom),
            end = Offset(left * 1.3f, bodyBottom),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(right - left * 0.3f, lidBottom),
            end = Offset(right - left * 0.3f, bodyBottom),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(left * 1.3f, bodyBottom),
            end = Offset(right - left * 0.3f, bodyBottom),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(this.size.width * 0.38f, this.size.height * 0.58f),
            end = Offset(this.size.width * 0.62f, this.size.height * 0.58f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

/** A simple price-tag glyph (pointed body + a small punch hole), matching this file's existing
 * stroke-based icon style, for the Notes Labels feature. */
@Composable
fun OneTaskLabelIcon(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = this.size.minDimension * 0.09f
        val tipX = this.size.width * 0.12f
        val shoulderX = this.size.width * 0.4f
        val right = this.size.width * 0.86f
        val top = this.size.height * 0.22f
        val bottom = this.size.height * 0.78f
        val midY = this.size.height / 2f

        val tagPath = Path().apply {
            moveTo(tipX, midY)
            lineTo(shoulderX, top)
            lineTo(right, top)
            lineTo(right, bottom)
            lineTo(shoulderX, bottom)
            close()
        }
        drawPath(tagPath, color = tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(
            color = tint,
            radius = this.size.minDimension * 0.06f,
            center = Offset(right - this.size.width * 0.16f, midY),
            style = Stroke(width = strokeWidth * 0.8f)
        )
    }
}

@Composable
fun OneTaskRecycleBinIcon(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = this.size.minDimension * 0.08f
        val lidY = this.size.height * 0.28f
        val bodyLeft = this.size.width * 0.22f
        val bodyRight = this.size.width * 0.78f
        val bodyBottom = this.size.height * 0.88f

        drawLine(
            color = tint,
            start = Offset(this.size.width * 0.12f, lidY),
            end = Offset(this.size.width * 0.88f, lidY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawRoundRect(
            color = tint,
            topLeft = Offset(this.size.width * 0.38f, this.size.height * 0.12f),
            size = Size(this.size.width * 0.24f, this.size.height * 0.16f),
            cornerRadius = CornerRadius(this.size.minDimension * 0.04f),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        val bodyInset = this.size.width * 0.04f
        val bodyPath = Path().apply {
            moveTo(bodyLeft, lidY)
            lineTo(bodyLeft + bodyInset, bodyBottom)
            lineTo(bodyRight - bodyInset, bodyBottom)
            lineTo(bodyRight, lidY)
        }
        drawPath(bodyPath, color = tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
        listOf(0.4f, 0.5f, 0.6f).forEach { fraction ->
            drawLine(
                color = tint,
                start = Offset(this.size.width * fraction, lidY + this.size.height * 0.12f),
                end = Offset(this.size.width * fraction, bodyBottom - this.size.height * 0.08f),
                strokeWidth = strokeWidth * 0.8f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun OneTaskSettingsGearIcon(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = this.size.minDimension * 0.09f
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val outerRadius = this.size.minDimension * 0.3f
        val toothRadius = this.size.minDimension * 0.42f

        drawCircle(color = tint, radius = outerRadius, center = center, style = Stroke(width = strokeWidth))
        drawCircle(color = tint, radius = this.size.minDimension * 0.08f, center = center)

        for (i in 0 until 8) {
            val angle = (i * 45f) * (Math.PI / 180f)
            val start = Offset(
                center.x + (outerRadius * 0.95f) * kotlin.math.cos(angle).toFloat(),
                center.y + (outerRadius * 0.95f) * kotlin.math.sin(angle).toFloat()
            )
            val end = Offset(
                center.x + toothRadius * kotlin.math.cos(angle).toFloat(),
                center.y + toothRadius * kotlin.math.sin(angle).toFloat()
            )
            drawLine(color = tint, start = start, end = end, strokeWidth = strokeWidth, cap = StrokeCap.Round)
        }
    }
}

@Composable
fun OneTaskShieldIcon(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = this.size.minDimension * 0.09f
        val left = this.size.width * 0.16f
        val right = this.size.width * 0.84f
        val top = this.size.height * 0.14f
        val midY = this.size.height * 0.5f
        val bottom = this.size.height * 0.9f
        val centerX = this.size.width / 2f

        val upperY = top + this.size.height * 0.1f
        val curveY = this.size.height * 0.72f
        val shieldPath = Path().apply {
            moveTo(centerX, top)
            lineTo(right, upperY)
            lineTo(right, midY)
            quadraticBezierTo(right, curveY, centerX, bottom)
            quadraticBezierTo(left, curveY, left, midY)
            lineTo(left, upperY)
            close()
        }
        drawPath(shieldPath, color = tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun OneTaskInfoIcon(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = this.size.minDimension * 0.08f
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        drawCircle(color = tint, radius = this.size.minDimension * 0.42f, center = center, style = Stroke(width = strokeWidth))
        drawLine(
            color = tint,
            start = Offset(center.x, this.size.height * 0.46f),
            end = Offset(center.x, this.size.height * 0.72f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawCircle(color = tint, radius = strokeWidth * 0.6f, center = Offset(center.x, this.size.height * 0.3f))
    }
}

@Composable
fun OneTaskChatIcon(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = this.size.minDimension * 0.08f
        val left = this.size.width * 0.12f
        val right = this.size.width * 0.88f
        val top = this.size.height * 0.18f
        val bottom = this.size.height * 0.68f

        drawRoundRect(
            color = tint,
            topLeft = Offset(left, top),
            size = Size(right - left, bottom - top),
            cornerRadius = CornerRadius(this.size.minDimension * 0.14f),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        val tailStartX = this.size.width * 0.3f
        val tailTipX = this.size.width * 0.24f
        val tailTipY = this.size.height * 0.86f
        val tailEndX = this.size.width * 0.44f
        val tailPath = Path().apply {
            moveTo(tailStartX, bottom)
            lineTo(tailTipX, tailTipY)
            lineTo(tailEndX, bottom)
        }
        drawPath(tailPath, color = tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
        listOf(0.36f, 0.5f).forEach { fraction ->
            drawLine(
                color = tint,
                start = Offset(this.size.width * 0.26f, this.size.height * fraction),
                end = Offset(this.size.width * 0.74f, this.size.height * fraction),
                strokeWidth = strokeWidth * 0.85f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun OneTaskStarIcon(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = this.size.minDimension * 0.07f
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val outerRadius = this.size.minDimension * 0.46f
        val innerRadius = outerRadius * 0.42f

        val path = Path()
        for (i in 0 until 10) {
            val radius = if (i % 2 == 0) outerRadius else innerRadius
            val angle = (i * 36f - 90f) * (Math.PI / 180f)
            val point = Offset(
                center.x + radius * kotlin.math.cos(angle).toFloat(),
                center.y + radius * kotlin.math.sin(angle).toFloat()
            )
            if (i == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
        }
        path.close()
        drawPath(path, color = tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun OneTaskCrownIcon(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = this.size.minDimension * 0.08f
        val baseY = this.size.height * 0.74f
        val topY = this.size.height * 0.28f
        val midY = this.size.height * 0.46f

        val dipY = this.size.height * 0.56f
        val x14 = this.size.width * 0.14f
        val x10 = this.size.width * 0.1f
        val x30 = this.size.width * 0.3f
        val x50 = this.size.width * 0.5f
        val x70 = this.size.width * 0.7f
        val x90 = this.size.width * 0.9f
        val x86 = this.size.width * 0.86f
        val crownPath = Path().apply {
            moveTo(x14, baseY)
            lineTo(x10, midY)
            lineTo(x30, dipY)
            lineTo(x50, topY)
            lineTo(x70, dipY)
            lineTo(x90, midY)
            lineTo(x86, baseY)
            close()
        }
        drawPath(crownPath, color = tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawLine(
            color = tint,
            start = Offset(x14, baseY),
            end = Offset(x86, baseY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun OneTaskLogoutIcon(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = this.size.minDimension * 0.09f
        val doorLeft = this.size.width * 0.16f
        val doorRight = this.size.width * 0.52f
        val top = this.size.height * 0.16f
        val bottom = this.size.height * 0.84f

        val doorPath = Path().apply {
            moveTo(doorRight, top)
            lineTo(doorLeft, top)
            lineTo(doorLeft, bottom)
            lineTo(doorRight, bottom)
        }
        drawPath(doorPath, color = tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))

        val arrowY = this.size.height / 2f
        drawLine(
            color = tint,
            start = Offset(this.size.width * 0.38f, arrowY),
            end = Offset(this.size.width * 0.88f, arrowY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        val arrowTipX = this.size.width * 0.66f
        val arrowEndX = this.size.width * 0.88f
        val arrowSpread = this.size.height * 0.18f
        val arrowHead = Path().apply {
            moveTo(arrowTipX, arrowY - arrowSpread)
            lineTo(arrowEndX, arrowY)
            lineTo(arrowTipX, arrowY + arrowSpread)
        }
        drawPath(arrowHead, color = tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** A simple stopwatch face (with its top crown/button) - used for the Timer screen's own
 * "Stopwatch" segment tab. The bottom nav's Timer tab icon is a separate bitmap asset, unrelated
 * to this composable. */
@Composable
fun OneTaskTimerIcon(
    modifier: Modifier = Modifier,
    active: Boolean = true,
    size: Dp = 24.dp
) {
    val tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = this.size.minDimension * 0.08f
        val center = Offset(this.size.width / 2f, this.size.height * 0.56f)
        val radius = this.size.minDimension * 0.36f

        drawLine(
            color = tint,
            start = Offset(this.size.width * 0.4f, this.size.height * 0.06f),
            end = Offset(this.size.width * 0.6f, this.size.height * 0.06f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(center.x, this.size.height * 0.06f),
            end = Offset(center.x, this.size.height * 0.2f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawCircle(color = tint, radius = radius, center = center, style = Stroke(width = strokeWidth))
        drawLine(
            color = tint,
            start = center,
            end = Offset(center.x, center.y - radius * 0.6f),
            strokeWidth = strokeWidth * 0.85f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = center,
            end = Offset(center.x + radius * 0.45f, center.y),
            strokeWidth = strokeWidth * 0.85f,
            cap = StrokeCap.Round
        )
    }
}

/** A plain analog clock face (circle + two hands, no crown/button) - used for the Timer screen's
 * "Timer" segment tab, distinguishing it from [OneTaskTimerIcon]'s stopwatch-with-crown look. */
@Composable
fun OneTaskClockIcon(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = this.size.minDimension * 0.09f
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val radius = this.size.minDimension * 0.42f

        drawCircle(color = tint, radius = radius, center = center, style = Stroke(width = strokeWidth))
        drawLine(
            color = tint,
            start = center,
            end = Offset(center.x, center.y - radius * 0.55f),
            strokeWidth = strokeWidth * 0.85f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = center,
            end = Offset(center.x + radius * 0.4f, center.y),
            strokeWidth = strokeWidth * 0.85f,
            cap = StrokeCap.Round
        )
    }
}

/** A stopwatch face with its top crown/button - the tint-parameterized twin of [OneTaskTimerIcon]
 * (which only supports an active/inactive boolean), used for the Timer screen's own "Stopwatch"
 * segment tab so it can share [SegmentedTab]'s selected/locked tint logic with the Timer tab's
 * [OneTaskClockIcon]. */
@Composable
fun OneTaskStopwatchIcon(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = this.size.minDimension * 0.08f
        val center = Offset(this.size.width / 2f, this.size.height * 0.56f)
        val radius = this.size.minDimension * 0.36f

        drawLine(
            color = tint,
            start = Offset(this.size.width * 0.4f, this.size.height * 0.06f),
            end = Offset(this.size.width * 0.6f, this.size.height * 0.06f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(center.x, this.size.height * 0.06f),
            end = Offset(center.x, this.size.height * 0.2f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawCircle(color = tint, radius = radius, center = center, style = Stroke(width = strokeWidth))
        drawLine(
            color = tint,
            start = center,
            end = Offset(center.x, center.y - radius * 0.6f),
            strokeWidth = strokeWidth * 0.85f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = center,
            end = Offset(center.x + radius * 0.45f, center.y),
            strokeWidth = strokeWidth * 0.85f,
            cap = StrokeCap.Round
        )
    }
}

/** A filled right-pointing play triangle. */
@Composable
fun OneTaskPlayIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val left = this.size.width * 0.24f
        val right = this.size.width * 0.82f
        val top = this.size.height * 0.16f
        val bottom = this.size.height * 0.84f
        val middle = this.size.height / 2f
        val playPath = Path().apply {
            moveTo(left, top)
            lineTo(right, middle)
            lineTo(left, bottom)
            close()
        }
        drawPath(playPath, color = tint)
    }
}

/** Two vertical bars - a standard pause glyph. */
@Composable
fun OneTaskPauseIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val barWidth = this.size.width * 0.22f
        val top = this.size.height * 0.16f
        val bottom = this.size.height * 0.84f
        val leftBarX = this.size.width * 0.28f
        val rightBarX = this.size.width * 0.72f
        val cornerRadius = CornerRadius(barWidth * 0.3f)

        drawRoundRect(
            color = tint,
            topLeft = Offset(leftBarX - barWidth / 2f, top),
            size = Size(barWidth, bottom - top),
            cornerRadius = cornerRadius
        )
        drawRoundRect(
            color = tint,
            topLeft = Offset(rightBarX - barWidth / 2f, top),
            size = Size(barWidth, bottom - top),
            cornerRadius = cornerRadius
        )
    }
}

/** A filled rounded square - a standard stop glyph. */
@Composable
fun OneTaskStopIcon(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val inset = this.size.width * 0.2f
        drawRoundRect(
            color = tint,
            topLeft = Offset(inset, inset),
            size = Size(this.size.width - inset * 2f, this.size.height - inset * 2f),
            cornerRadius = CornerRadius(this.size.minDimension * 0.12f)
        )
    }
}

/** A simple flag-on-a-pole glyph - the Stopwatch's Lap placeholder control (no lap recording
 * yet, purely a future-functionality placeholder per spec). */
@Composable
fun OneTaskLapFlagIcon(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = this.size.minDimension * 0.09f
        val poleX = this.size.width * 0.28f
        val top = this.size.height * 0.15f
        val bottom = this.size.height * 0.88f

        drawLine(
            color = tint,
            start = Offset(poleX, top),
            end = Offset(poleX, bottom),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        val flagRight = this.size.width * 0.82f
        val flagMidY = this.size.height * 0.36f
        val flagBottomY = this.size.height * 0.52f
        val flagPath = Path().apply {
            moveTo(poleX, top)
            lineTo(flagRight, top + (flagMidY - top) * 0.5f)
            lineTo(poleX + (flagRight - poleX) * 0.55f, flagMidY)
            lineTo(flagRight, flagMidY + (flagBottomY - flagMidY) * 0.5f)
            lineTo(poleX, flagBottomY)
            close()
        }
        drawPath(flagPath, color = tint, style = Stroke(width = strokeWidth * 0.75f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** A simple monitor glyph - the Appearance screen's "System Default" display-mode option. */
@Composable
fun OneTaskSystemDefaultIcon(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = this.size.minDimension * 0.09f
        val screenLeft = this.size.width * 0.12f
        val screenRight = this.size.width * 0.88f
        val screenTop = this.size.height * 0.16f
        val screenBottom = this.size.height * 0.68f

        drawRoundRect(
            color = tint,
            topLeft = Offset(screenLeft, screenTop),
            size = Size(screenRight - screenLeft, screenBottom - screenTop),
            cornerRadius = CornerRadius(this.size.minDimension * 0.08f),
            style = Stroke(width = strokeWidth)
        )
        drawLine(
            color = tint,
            start = Offset(this.size.width / 2f, screenBottom),
            end = Offset(this.size.width / 2f, screenBottom + this.size.height * 0.08f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(this.size.width * 0.28f, this.size.height * 0.86f),
            end = Offset(this.size.width * 0.72f, this.size.height * 0.86f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

/** A sun glyph (circle + radiating rays) - the Appearance screen's "Light" display-mode option. */
@Composable
fun OneTaskSunIcon(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = this.size.minDimension * 0.09f
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val coreRadius = this.size.minDimension * 0.22f
        val rayInner = this.size.minDimension * 0.34f
        val rayOuter = this.size.minDimension * 0.46f

        drawCircle(color = tint, radius = coreRadius, center = center, style = Stroke(width = strokeWidth))

        for (i in 0 until 8) {
            val angle = Math.toRadians((i * 45).toDouble())
            val cosA = cos(angle).toFloat()
            val sinA = sin(angle).toFloat()
            drawLine(
                color = tint,
                start = Offset(center.x + rayInner * cosA, center.y + rayInner * sinA),
                end = Offset(center.x + rayOuter * cosA, center.y + rayOuter * sinA),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}

/** A crescent-moon glyph (a large circle minus a smaller offset circle, via a true path
 * subtraction rather than an overlaid "cutout" color) - the Appearance screen's "Dark"
 * display-mode option. */
@Composable
fun OneTaskMoonIcon(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val outerRadius = this.size.minDimension * 0.32f
        val outerCenter = Offset(this.size.width * 0.44f, this.size.height * 0.5f)
        val innerRadius = this.size.minDimension * 0.27f
        val innerCenter = Offset(this.size.width * 0.58f, this.size.height * 0.38f)

        val outerCircle = Path().apply {
            addOval(Rect(center = outerCenter, radius = outerRadius))
        }
        val innerCircle = Path().apply {
            addOval(Rect(center = innerCenter, radius = innerRadius))
        }
        val crescent = Path()
        crescent.op(outerCircle, innerCircle, PathOperation.Difference)
        drawPath(crescent, color = tint)
    }
}

/** A generic picture-frame glyph (rounded border + a small "sun" dot + a mountain diagonal) -
 * the Appearance screen's Wallpaper section placeholder tiles (No Wallpaper and the still-inert
 * Wallpaper 1-5 options, none of which have real image assets yet). */
@Composable
fun OneTaskWallpaperPlaceholderIcon(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = this.size.minDimension * 0.08f
        val left = this.size.width * 0.12f
        val right = this.size.width * 0.88f
        val top = this.size.height * 0.2f
        val bottom = this.size.height * 0.8f

        drawRoundRect(
            color = tint,
            topLeft = Offset(left, top),
            size = Size(right - left, bottom - top),
            cornerRadius = CornerRadius(this.size.minDimension * 0.1f),
            style = Stroke(width = strokeWidth)
        )
        drawCircle(
            color = tint,
            radius = this.size.minDimension * 0.07f,
            center = Offset(left + (right - left) * 0.28f, top + (bottom - top) * 0.32f)
        )
        val mountainPath = Path().apply {
            moveTo(left + (right - left) * 0.16f, bottom - (bottom - top) * 0.14f)
            lineTo(left + (right - left) * 0.42f, top + (bottom - top) * 0.5f)
            lineTo(left + (right - left) * 0.62f, top + (bottom - top) * 0.68f)
            lineTo(left + (right - left) * 0.84f, top + (bottom - top) * 0.38f)
            lineTo(right - (right - left) * 0.14f, bottom - (bottom - top) * 0.14f)
        }
        drawPath(
            mountainPath,
            color = tint,
            style = Stroke(width = strokeWidth * 0.85f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

/** A single leaf (almond outline + center vein) - Focus Mode's "Light" focus level option. */
@Composable
fun OneTaskLeafIcon(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = this.size.minDimension * 0.08f
        val tipX = this.size.width * 0.8f
        val tipY = this.size.height * 0.2f
        val baseX = this.size.width * 0.2f
        val baseY = this.size.height * 0.8f
        val topControlX = this.size.width * 0.2f
        val topControlY = this.size.height * 0.32f
        val bottomControlX = this.size.width * 0.68f
        val bottomControlY = this.size.height * 0.8f

        val leafPath = Path().apply {
            moveTo(baseX, baseY)
            quadraticBezierTo(topControlX, topControlY, tipX, tipY)
            quadraticBezierTo(bottomControlX, bottomControlY, baseX, baseY)
            close()
        }
        drawPath(leafPath, color = tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawLine(
            color = tint,
            start = Offset(baseX, baseY),
            end = Offset(tipX, tipY),
            strokeWidth = strokeWidth * 0.7f,
            cap = StrokeCap.Round
        )
    }
}

/** A bullseye/target glyph (two concentric rings + a center dot) - Focus Mode's "Focus level"
 * section icon and its "Deep" focus level option. */
@Composable
fun OneTaskTargetIcon(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = this.size.minDimension * 0.08f
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        drawCircle(color = tint, radius = this.size.minDimension * 0.42f, center = center, style = Stroke(width = strokeWidth))
        drawCircle(color = tint, radius = this.size.minDimension * 0.24f, center = center, style = Stroke(width = strokeWidth))
        drawCircle(color = tint, radius = this.size.minDimension * 0.07f, center = center)
    }
}

/** A padlock glyph (shackle arc + rounded body) - Focus Mode's "Strict" focus level option. */
@Composable
fun OneTaskLockIcon(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = this.size.minDimension * 0.09f
        val bodyLeft = this.size.width * 0.2f
        val bodyRight = this.size.width * 0.8f
        val bodyTop = this.size.height * 0.46f
        val bodyBottom = this.size.height * 0.86f
        val shackleCenterX = this.size.width / 2f
        val shackleTop = this.size.height * 0.14f
        val shackleRadius = this.size.width * 0.22f

        drawArc(
            color = tint,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(shackleCenterX - shackleRadius, shackleTop),
            size = Size(shackleRadius * 2f, shackleRadius * 2f),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        drawLine(
            color = tint,
            start = Offset(shackleCenterX - shackleRadius, shackleTop + shackleRadius),
            end = Offset(shackleCenterX - shackleRadius, bodyTop),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(shackleCenterX + shackleRadius, shackleTop + shackleRadius),
            end = Offset(shackleCenterX + shackleRadius, bodyTop),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawRoundRect(
            color = tint,
            topLeft = Offset(bodyLeft, bodyTop),
            size = Size(bodyRight - bodyLeft, bodyBottom - bodyTop),
            cornerRadius = CornerRadius(this.size.minDimension * 0.1f),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawCircle(
            color = tint,
            radius = this.size.minDimension * 0.05f,
            center = Offset(shackleCenterX, bodyTop + (bodyBottom - bodyTop) * 0.38f)
        )
    }
}

/** A simplified phone-handset glyph (a rotated rounded-pill outline) - Focus Mode's "Notifications
 * & Calls" row, shown alongside a bell icon. */
@Composable
fun OneTaskPhoneIcon(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = this.size.minDimension * 0.09f
        rotate(degrees = -45f) {
            val bodyWidth = this.size.width * 0.34f
            val bodyHeight = this.size.height * 0.78f
            val left = (this.size.width - bodyWidth) / 2f
            val top = (this.size.height - bodyHeight) / 2f
            drawRoundRect(
                color = tint,
                topLeft = Offset(left, top),
                size = Size(bodyWidth, bodyHeight),
                cornerRadius = CornerRadius(bodyWidth * 0.5f),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

/** A coffee-cup glyph (cup body + a side handle) - the Running Focus screen's manual Break
 * control. */
@Composable
fun OneTaskCoffeeCupIcon(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = this.size.minDimension * 0.09f
        val cupLeft = this.size.width * 0.2f
        val cupRight = this.size.width * 0.68f
        val cupTop = this.size.height * 0.28f
        val cupBottom = this.size.height * 0.78f
        val cupBottomLeft = cupLeft + this.size.width * 0.04f
        val cupBottomRight = cupRight - this.size.width * 0.04f

        val cupPath = Path().apply {
            moveTo(cupLeft, cupTop)
            lineTo(cupBottomLeft, cupBottom)
            lineTo(cupBottomRight, cupBottom)
            lineTo(cupRight, cupTop)
            close()
        }
        drawPath(cupPath, color = tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))

        val handleCenter = Offset(cupRight + this.size.width * 0.08f, (cupTop + cupBottom) / 2f)
        val handleRadius = this.size.width * 0.12f
        drawArc(
            color = tint,
            startAngle = -100f,
            sweepAngle = 200f,
            useCenter = false,
            topLeft = Offset(handleCenter.x - handleRadius, handleCenter.y - handleRadius),
            size = Size(handleRadius * 2f, handleRadius * 2f),
            style = Stroke(width = strokeWidth * 0.85f, cap = StrokeCap.Round)
        )

        drawLine(
            color = tint,
            start = Offset(this.size.width * 0.3f, cupTop - this.size.height * 0.1f),
            end = Offset(this.size.width * 0.34f, cupTop - this.size.height * 0.02f),
            strokeWidth = strokeWidth * 0.7f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(this.size.width * 0.46f, cupTop - this.size.height * 0.12f),
            end = Offset(this.size.width * 0.5f, cupTop - this.size.height * 0.04f),
            strokeWidth = strokeWidth * 0.7f,
            cap = StrokeCap.Round
        )
    }
}

/** A camera glyph (body + lens + top viewfinder bump) - the Note Editor's "+" menu's own
 * "Open Camera" row. */
@Composable
fun OneTaskCameraIcon(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = this.size.minDimension * 0.09f
        val bodyLeft = this.size.width * 0.12f
        val bodyRight = this.size.width * 0.88f
        val bodyTop = this.size.height * 0.32f
        val bodyBottom = this.size.height * 0.82f

        drawRoundRect(
            color = tint,
            topLeft = Offset(bodyLeft, bodyTop),
            size = Size(bodyRight - bodyLeft, bodyBottom - bodyTop),
            cornerRadius = CornerRadius(this.size.minDimension * 0.1f),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        val bumpWidth = this.size.width * 0.28f
        val bumpHeight = this.size.height * 0.12f
        drawRoundRect(
            color = tint,
            topLeft = Offset(this.size.width * 0.36f, bodyTop - bumpHeight * 0.8f),
            size = Size(bumpWidth, bumpHeight),
            cornerRadius = CornerRadius(bumpHeight * 0.3f),
            style = Stroke(width = strokeWidth * 0.85f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        drawCircle(
            color = tint,
            radius = (bodyBottom - bodyTop) * 0.26f,
            center = Offset((bodyLeft + bodyRight) / 2f, (bodyTop + bodyBottom) / 2f + this.size.height * 0.02f),
            style = Stroke(width = strokeWidth * 0.85f)
        )
    }
}

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
        val bodyPath = Path().apply {
            moveTo(bodyLeft, lidY)
            lineTo(bodyLeft + this.size.width * 0.04f, bodyBottom)
            lineTo(bodyRight - this.size.width * 0.04f, bodyBottom)
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

        val shieldPath = Path().apply {
            moveTo(centerX, top)
            lineTo(right, top + this.size.height * 0.1f)
            lineTo(right, midY)
            quadraticBezierTo(right, this.size.height * 0.72f, centerX, bottom)
            quadraticBezierTo(left, this.size.height * 0.72f, left, midY)
            lineTo(left, top + this.size.height * 0.1f)
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
        val tailPath = Path().apply {
            moveTo(this.size.width * 0.3f, bottom)
            lineTo(this.size.width * 0.24f, this.size.height * 0.86f)
            lineTo(this.size.width * 0.44f, bottom)
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

        val crownPath = Path().apply {
            moveTo(this.size.width * 0.14f, baseY)
            lineTo(this.size.width * 0.1f, midY)
            lineTo(this.size.width * 0.3f, this.size.height * 0.56f)
            lineTo(this.size.width * 0.5f, topY)
            lineTo(this.size.width * 0.7f, this.size.height * 0.56f)
            lineTo(this.size.width * 0.9f, midY)
            lineTo(this.size.width * 0.86f, baseY)
            close()
        }
        drawPath(crownPath, color = tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawLine(
            color = tint,
            start = Offset(this.size.width * 0.14f, baseY),
            end = Offset(this.size.width * 0.86f, baseY),
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
        val arrowHead = Path().apply {
            moveTo(this.size.width * 0.66f, arrowY - this.size.height * 0.18f)
            lineTo(this.size.width * 0.88f, arrowY)
            lineTo(this.size.width * 0.66f, arrowY + this.size.height * 0.18f)
        }
        drawPath(arrowHead, color = tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** A simple stopwatch face - used only for the bottom nav's placeholder Timer tab, which has no
 * functionality of its own yet. */
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

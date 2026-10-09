package com.devfahim00.trackyou.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Donut chart rendered on Canvas, with composables allowed in the center hole.
 */
@Composable
fun DonutChart(
    segments: List<Pair<Float, Color>>,
    modifier: Modifier = Modifier,
    stroke: Dp = 24.dp,
    center: @Composable BoxScope.() -> Unit = {}
) {
    val total = segments.map { it.first }.sum()
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    // Draw-in animation: arcs sweep from 0 to full when data (month) changes.
    val progress = remember { Animatable(0f) }
    LaunchedEffect(segments) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(750, easing = FastOutSlowInEasing))
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val strokeWidthPx = stroke.toPx()
            val diameter = minOf(size.width, size.height) - strokeWidthPx
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            if (total <= 0f) {
                drawArc(
                    color = trackColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(strokeWidthPx, cap = StrokeCap.Butt)
                )
            } else {
                var start = -90f
                val gap = if (segments.size > 1) 2.5f else 0f
                val p = progress.value
                segments.forEach { (value, color) ->
                    val sweep = (value / total) * 360f * p
                    val eff = (sweep - gap * p).coerceAtLeast(0f)
                    if (eff > 0f) {
                        drawArc(
                            color = color,
                            startAngle = start + gap / 2f,
                            sweepAngle = eff,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(strokeWidthPx, cap = StrokeCap.Butt)
                        )
                    }
                    start += sweep
                }
            }
        }
        center()
    }
}

/** Legend dot + label used next to charts. */
@Composable
fun LegendDot(color: Color, modifier: Modifier = Modifier) {
    Box(modifier.size(9.dp).background(color, CircleShape))
}

/**
 * Grouped bar chart of income vs expense for the last N months.
 * Labels are rendered as composables below the canvas.
 */
@Composable
fun MonthBarsChart(
    labels: List<String>,
    income: List<Float>,
    expense: List<Float>,
    modifier: Modifier = Modifier
) {
    val inc = incomeColor()
    val exp = expenseColor()
    // Bars grow from 0 to full height when data (month) changes.
    val progress = remember { Animatable(0f) }
    LaunchedEffect(labels, income, expense) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(750, easing = FastOutSlowInEasing))
    }
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(130.dp)) {
            val maxV = maxOf(income.maxOrNull() ?: 0f, expense.maxOrNull() ?: 0f, 1f)
            val usableH = size.height - 4f
            val n = labels.size.coerceAtLeast(1)
            val groupW = size.width / n
            val barW = (groupW * 0.26f).coerceAtLeast(6f)
            val gap = (groupW * 0.10f).coerceAtLeast(3f)
            val p = progress.value
            labels.forEachIndexed { i, _ ->
                val cx = groupW * i + groupW / 2f
                val ih = ((income.getOrNull(i) ?: 0f) / maxV) * usableH * p
                val eh = ((expense.getOrNull(i) ?: 0f) / maxV) * usableH * p
                if (ih >= 1f) {
                    drawRoundRect(
                        color = inc,
                        topLeft = Offset(cx - barW - gap, size.height - ih),
                        size = Size(barW, ih),
                        cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                    )
                }
                if (eh >= 1f) {
                    drawRoundRect(
                        color = exp,
                        topLeft = Offset(cx + gap, size.height - eh),
                        size = Size(barW, eh),
                        cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            labels.forEach { l ->
                Text(
                    l,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Chart legend row: income / expense color key. */
@Composable
fun ChartLegend(modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            LegendDot(incomeColor())
            Text("Income", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            LegendDot(expenseColor())
            Text("Expense", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

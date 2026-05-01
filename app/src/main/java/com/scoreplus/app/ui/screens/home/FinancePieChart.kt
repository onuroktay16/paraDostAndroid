package com.scoreplus.app.ui.screens.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class FlatExpenseItem(
    val label: String,
    val emoji: String,
    val amount: Double
)

val expenseChartColors = listOf(
    Color(0xFFFF6B6B), // coral
    Color(0xFFFFE66D), // yellow
    Color(0xFF4ECDC4), // teal
    Color(0xFFFF8B94), // pink
    Color(0xFFFFB347), // orange
    Color(0xFF87CEEB), // sky blue
    Color(0xFFDDA0DD), // plum
    Color(0xFFFF69B4), // hot pink
    Color(0xFFFFA07A), // salmon
    Color(0xFF87CEFA), // light blue
    Color(0xFFEEE8AA), // pale yellow
    Color(0xFFB5EAD7), // mint
)

@Composable
fun ExpenseChartSection(
    flatExpenses: List<FlatExpenseItem>,
    modifier: Modifier = Modifier
) {
    val total = flatExpenses.sumOf { it.amount }
    val isEmpty = flatExpenses.isEmpty() || total <= 0.0
    val primaryColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Donut chart
        Box(
            modifier = Modifier.size(130.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(130.dp)) {
                val strokeWidth = size.minDimension * 0.21f
                val radius = (size.minDimension - strokeWidth) / 2f
                val center = Offset(size.width / 2f, size.height / 2f)
                val topLeft = Offset(center.x - radius, center.y - radius)
                val arcSize = Size(radius * 2f, radius * 2f)

                if (isEmpty) {
                    // Empty state: single gray ring
                    drawArc(
                        color = Color.White.copy(alpha = 0.2f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                    )
                } else {
                    var startAngle = -90f
                    flatExpenses.forEachIndexed { i, item ->
                        val sweep = (item.amount / total * 360.0).toFloat()
                        val gap = if (flatExpenses.size > 1) 3f else 0f
                        drawArc(
                            color = expenseChartColors[i % expenseChartColors.size],
                            startAngle = startAngle,
                            sweepAngle = (sweep - gap).coerceAtLeast(2f),
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                        )
                        startAngle += sweep
                    }
                }

                // Center hole
                drawCircle(
                    color = primaryColor,
                    radius = radius - strokeWidth / 2f,
                    center = center
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (isEmpty) {
                    Text("Gider", fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f))
                    Text("yok", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                } else {
                    Text("Gider", fontSize = 9.sp, color = Color.White.copy(alpha = 0.65f))
                    Text(
                        text = formatCurrency(total),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Legend
        if (isEmpty) {
            Text(
                text = "Bu ay henüz\ngider eklenmemiş.",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.55f),
                modifier = Modifier.weight(1f)
            )
        } else {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                flatExpenses.forEachIndexed { i, item ->
                    val percentValue = item.amount / total * 100
                    val percentText = when {
                        percentValue >= 1.0  -> "%${percentValue.toInt()}"
                        percentValue >= 0.1  -> "%${"%.1f".format(percentValue)}"
                        percentValue >= 0.01 -> "%${"%.2f".format(percentValue)}"
                        else                 -> "%${"%.2f".format(percentValue)}"
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(expenseChartColors[i % expenseChartColors.size])
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${item.emoji} ${item.label}",
                            fontSize = 11.sp,
                            color = Color.White,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = percentText,
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }
                }
            }
        }
    }
}

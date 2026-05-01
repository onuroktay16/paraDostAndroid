package com.scoreplus.app.ui.screens.yearly

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.scoreplus.app.ScorePlusApp
import com.scoreplus.app.ui.screens.home.formatCurrency
import com.scoreplus.app.ui.theme.ExpenseRed
import com.scoreplus.app.ui.theme.IncomeGreen

@Composable
fun YearlyScreen() {
    val context = LocalContext.current
    val repository = (context.applicationContext as ScorePlusApp).repository
    val viewModel: YearlyViewModel = viewModel(factory = YearlyViewModelFactory(repository))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // ── Header ──────────────────────────────────────────────────────────
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.88f)
                            )
                        )
                    )
                    .statusBarsPadding()
                    .padding(bottom = 24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Yıllık Özet",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Sol ok — sadece 2026 var, disabled
                        IconButton(onClick = {}, enabled = false) {
                            Icon(
                                Icons.Default.KeyboardArrowLeft,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Text(
                            text = "${uiState.year}",
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        IconButton(onClick = {}, enabled = false) {
                            Icon(
                                Icons.Default.KeyboardArrowRight,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        }

        // ── Özet Kartlar ────────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .offset(y = (-8).dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                YearlySummaryCard(
                    modifier = Modifier.weight(1f),
                    emoji = "💰",
                    label = "Gelir",
                    value = formatCurrency(uiState.totalIncome),
                    valueColor = IncomeGreen
                )
                YearlySummaryCard(
                    modifier = Modifier.weight(1f),
                    emoji = "📉",
                    label = "Gider",
                    value = formatCurrency(uiState.totalExpenses),
                    valueColor = ExpenseRed
                )
                YearlySummaryCard(
                    modifier = Modifier.weight(1f),
                    emoji = "🏦",
                    label = "Net",
                    value = formatCurrency(uiState.net),
                    valueColor = if (uiState.net >= 0) IncomeGreen else ExpenseRed
                )
            }
        }

        // ── Kategori Başlığı ─────────────────────────────────────────────────
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Kategorilere Göre Giderler",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        if (uiState.categoryItems.isEmpty() && !uiState.isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Bu yıl henüz gider girilmemiş.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // ── Kategori Listesi ─────────────────────────────────────────────────
        items(uiState.categoryItems) { item ->
            CategoryYearlyCard(item = item, maxTotal = uiState.categoryItems.firstOrNull()?.total ?: 1.0)
        }
    }
}

@Composable
private fun YearlySummaryCard(
    modifier: Modifier,
    emoji: String,
    label: String,
    value: String,
    valueColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("$emoji $label", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = valueColor)
        }
    }
}

@Composable
private fun CategoryYearlyCard(item: YearlyCategoryItem, maxTotal: Double) {
    val barFraction = (item.total / maxTotal).coerceIn(0.0, 1.0).toFloat()
    val percentText = when {
        item.percent >= 1.0  -> "%${item.percent.toInt()}"
        item.percent >= 0.1  -> "%${"%.1f".format(item.percent)}"
        else                 -> "%${"%.2f".format(item.percent)}"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(item.category.icon, fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = item.category.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatCurrency(item.total),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = ExpenseRed
                    )
                    Text(
                        text = percentText,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Yatay progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(barFraction)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    ExpenseRed
                                )
                            )
                        )
                )
            }
        }
    }
}

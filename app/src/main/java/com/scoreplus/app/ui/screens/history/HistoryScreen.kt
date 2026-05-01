package com.scoreplus.app.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

private val turkishMonths = listOf(
    "Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran",
    "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(onMonthClick: (month: Int, year: Int) -> Unit = { _, _ -> }) {
    val context = LocalContext.current
    val repository = (context.applicationContext as ScorePlusApp).repository
    val viewModel: HistoryViewModel = viewModel(factory = HistoryViewModelFactory(repository))
    val summaries by viewModel.monthlySummaries.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Geçmiş Aylar") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                ),
            )
        }
    ) { padding ->
        if (summaries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Henüz kayıtlı ay yok.\nAnasayfadan gelir veya gider eklemeye başla.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 15.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(summaries) { summary ->
                    MonthSummaryCard(summary, onClick = { onMonthClick(summary.month, summary.year) })
                }
            }
        }
    }
}

@Composable
private fun MonthSummaryCard(summary: MonthSummaryItem, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "${turkishMonths[summary.month - 1]} ${summary.year}",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                SummaryMetric(
                    label = "Gelir",
                    value = formatCurrency(summary.income),
                    color = IncomeGreen
                )
                SummaryMetric(
                    label = "Gider",
                    value = formatCurrency(summary.totalExpenses),
                    color = ExpenseRed
                )
                SummaryMetric(
                    label = "Birikim",
                    value = formatCurrency(summary.displaySavings),
                    color = if (summary.displaySavings >= 0) IncomeGreen else ExpenseRed
                )
            }
            if (summary.income > 0 && summary.totalExpenses > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                val spent = (summary.totalExpenses / summary.income).coerceIn(0.0, 1.0).toFloat()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(spent)
                            .fillMaxHeight()
                            .background(if (spent < 0.8f) MaterialTheme.colorScheme.primary else ExpenseRed)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Gelirinizin %${(spent * 100).toInt()}'i harcandı",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SummaryMetric(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

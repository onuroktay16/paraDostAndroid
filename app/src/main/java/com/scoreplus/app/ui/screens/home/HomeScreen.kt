package com.scoreplus.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.scoreplus.app.ScorePlusApp
import com.scoreplus.app.data.local.entity.ExpenseEntity
import com.scoreplus.app.data.local.entity.IncomeItemEntity
import com.scoreplus.app.ui.theme.ExpenseRed
import com.scoreplus.app.ui.theme.IncomeGreen
import androidx.compose.ui.graphics.Brush

private val turkishMonths = listOf(
    "Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran",
    "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    initialMonth: Int = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1,
    initialYear: Int = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR),
    onNavigateBack: (() -> Unit)? = null,
    onNavigateToAddExpense: (Int, Int) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val repository = (context.applicationContext as ScorePlusApp).repository
    val viewModel: HomeViewModel = viewModel(
        key = "home_${initialMonth}_${initialYear}",
        factory = HomeViewModelFactory(repository, initialMonth, initialYear)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showAddIncomeDialog by remember { mutableStateOf(false) }
    var showEditIncomeDialog by remember { mutableStateOf<IncomeItemEntity?>(null) }
    var showDeleteIncomeDialog by remember { mutableStateOf<IncomeItemEntity?>(null) }
    var showDeleteExpenseDialog by remember { mutableStateOf<ExpenseEntity?>(null) }
    var showEditExpenseDialog by remember { mutableStateOf<ExpenseEntity?>(null) }
    var showSavingsDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNavigateToAddExpense(uiState.selectedMonth, uiState.selectedYear) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Gider Ekle", fontWeight = FontWeight.SemiBold) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = padding.calculateBottomPadding() + 80.dp)
        ) {
            // ── Header ──────────────────────────────────────────────────────
            item {
                val flatExpenses = remember(uiState.categoryExpenses) {
                    uiState.categoryExpenses
                        .flatMap { catItem ->
                            catItem.expenses.map { exp ->
                                FlatExpenseItem(
                                    label = if (exp.description.isNotBlank()) exp.description else catItem.category.name,
                                    emoji = catItem.category.icon,
                                    amount = exp.amount
                                )
                            }
                        }
                        .sortedByDescending { it.amount }
                }

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
                        .padding(bottom = 20.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Üst bar: geri butonu (sol) + info butonu (sağ)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (onNavigateBack != null) {
                                IconButton(onClick = onNavigateBack) {
                                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Geri", tint = Color.White, modifier = Modifier.size(28.dp))
                                }
                            } else {
                                Spacer(modifier = Modifier.size(48.dp))
                            }
                            Spacer(modifier = Modifier.weight(1f))
                        }

                        // Pie chart — mavi alanın en üstünden başlar
                        ExpenseChartSection(flatExpenses = flatExpenses)
                        Spacer(modifier = Modifier.height(4.dp))

                        // Month navigator
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val isFirstMonth = uiState.selectedMonth == 1 && uiState.selectedYear == 2026
                            IconButton(
                                onClick = { viewModel.previousMonth() },
                                enabled = !isFirstMonth
                            ) {
                                Icon(
                                    Icons.Default.KeyboardArrowLeft,
                                    contentDescription = "Önceki Ay",
                                    tint = if (isFirstMonth) Color.White.copy(alpha = 0.3f) else Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Text(
                                text = "${turkishMonths[uiState.selectedMonth - 1]} ${uiState.selectedYear}",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            IconButton(onClick = { viewModel.nextMonth() }) {
                                Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Sonraki Ay", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(32.dp))
                            }
                        }
                    }
                }
            }

            // ── Özet Kartlar ────────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .offset(y = (-8).dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SummaryCard(
                        modifier = Modifier.weight(1f),
                        emoji = "💰",
                        label = "Gelir",
                        value = formatCurrency(uiState.totalIncome),
                        valueColor = IncomeGreen
                    )
                    SummaryCard(
                        modifier = Modifier.weight(1f),
                        emoji = "📉",
                        label = "Gider",
                        value = formatCurrency(uiState.totalExpenses),
                        valueColor = ExpenseRed
                    )
                    // Birikim kartı
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showSavingsDialog = true }
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🏦", fontSize = 11.sp)
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("Birikim", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                                if (uiState.isSavingsCustom) {
                                    Icon(Icons.Default.Edit, contentDescription = null,
                                        modifier = Modifier.size(10.dp),
                                        tint = MaterialTheme.colorScheme.secondary)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formatCurrency(uiState.effectiveSavings),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.effectiveSavings >= 0) IncomeGreen else ExpenseRed
                            )
                        }
                    }
                }
            }

            // ── Bakiye + Birikim Kartları ────────────────────────────────────
            item {
                val spentPercent = if (uiState.totalIncome > 0)
                    (uiState.totalExpenses / uiState.totalIncome).coerceIn(0.0, 1.0).toFloat()
                else 0f

                // Bakiye Bandı
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Kalan Bakiye", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = formatCurrency(uiState.balance),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (uiState.balance >= 0) IncomeGreen else ExpenseRed
                            )
                        }
                        if (uiState.totalIncome > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(spentPercent)
                                        .fillMaxHeight()
                                        .background(if (spentPercent < 0.8f) MaterialTheme.colorScheme.primary else ExpenseRed)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Gelirinizin %${(spentPercent * 100).toInt()}'i harcandı", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

            }

            // ── Gelirler Başlığı ─────────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("💰 Gelirler", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    TextButton(onClick = { showAddIncomeDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ekle")
                    }
                }
            }

            if (uiState.incomeItems.isEmpty()) {
                item {
                    Text(
                        text = "Henüz gelir eklenmemiş.",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(uiState.incomeItems) { item ->
                    IncomeItemRow(
                        item = item,
                        onEdit = { showEditIncomeDialog = it },
                        onDelete = { showDeleteIncomeDialog = it }
                    )
                }
            }

            // ── Giderler Başlığı ─────────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "📉 Giderler",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            if (uiState.categoryExpenses.isEmpty()) {
                item {
                    Text(
                        text = "Bu ay henüz gider eklenmemiş.",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(uiState.categoryExpenses) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) { Text(item.category.icon, fontSize = 20.sp) }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(item.category.name, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            Text(formatCurrency(item.total), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ExpenseRed)
                        }
                        item.expenses.forEach { expense ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(start = 68.dp, end = 16.dp, bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (expense.description.isNotBlank()) expense.description else "—",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                if (item.expenses.size > 1) {
                                    Text(formatCurrency(expense.amount), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Icon(Icons.Default.Edit, contentDescription = "Düzenle",
                                    modifier = Modifier.size(16.dp).clickable { showEditExpenseDialog = expense },
                                    tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.Default.Delete, contentDescription = "Sil",
                                    modifier = Modifier.size(16.dp).clickable { showDeleteExpenseDialog = expense },
                                    tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Diyaloglar ───────────────────────────────────────────────────────────

    if (showSavingsDialog) {
        val currentSavings = uiState.effectiveSavings
        var savingsInput by remember(showSavingsDialog) {
            mutableStateOf(
                if (currentSavings == currentSavings.toLong().toDouble())
                    currentSavings.toLong().toString()
                else currentSavings.toString()
            )
        }
        AlertDialog(
            onDismissRequest = { showSavingsDialog = false },
            title = { Text("Birikim") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Otomatik hesaplanan: ${formatCurrency(uiState.balance)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = savingsInput,
                        onValueChange = { savingsInput = it.filter { c -> c.isDigit() || c == '.' || c == '-' } },
                        label = { Text("Birikim tutarı (₺)") },
                        prefix = { Text("₺ ", fontWeight = FontWeight.Bold) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Otomatik: Gelir − Gider = ${formatCurrency(uiState.balance)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (uiState.isSavingsCustom) {
                        TextButton(
                            onClick = { viewModel.resetSavings(); showSavingsDialog = false },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Otomatik hesaplamaya dön", fontSize = 12.sp) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val amount = savingsInput.toDoubleOrNull()
                    if (amount != null) { viewModel.saveSavings(amount); showSavingsDialog = false }
                }) { Text("Kaydet") }
            },
            dismissButton = { TextButton(onClick = { showSavingsDialog = false }) { Text("İptal") } }
        )
    }

    if (showAddIncomeDialog) {
        IncomeDialog(
            title = "Gelir Ekle",
            initialAmount = "",
            initialDescription = "",
            onDismiss = { showAddIncomeDialog = false },
            onConfirm = { amount, desc ->
                viewModel.addIncomeItem(amount, desc)
                showAddIncomeDialog = false
            }
        )
    }

    showEditIncomeDialog?.let { item ->
        IncomeDialog(
            title = "Geliri Düzenle",
            initialAmount = if (item.amount == item.amount.toLong().toDouble()) item.amount.toLong().toString() else item.amount.toString(),
            initialDescription = item.description,
            onDismiss = { showEditIncomeDialog = null },
            onConfirm = { amount, desc ->
                viewModel.updateIncomeItem(item, amount, desc)
                showEditIncomeDialog = null
            }
        )
    }

    showDeleteIncomeDialog?.let { item ->
        AlertDialog(
            onDismissRequest = { showDeleteIncomeDialog = null },
            title = { Text("Geliri Sil") },
            text = { Text("Bu gelir kaydını silmek istediğinden emin misin?") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteIncomeItem(item); showDeleteIncomeDialog = null }) {
                    Text("Sil", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteIncomeDialog = null }) { Text("İptal") } }
        )
    }

    showDeleteExpenseDialog?.let { expense ->
        AlertDialog(
            onDismissRequest = { showDeleteExpenseDialog = null },
            title = { Text("Gideri Sil") },
            text = { Text("Bu gideri silmek istediğinden emin misin?") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteExpense(expense); showDeleteExpenseDialog = null }) {
                    Text("Sil", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteExpenseDialog = null }) { Text("İptal") } }
        )
    }

    showEditExpenseDialog?.let { expense ->
        var editAmount by remember(expense.id) {
            mutableStateOf(if (expense.amount == expense.amount.toLong().toDouble()) expense.amount.toLong().toString() else expense.amount.toString())
        }
        var editDescription by remember(expense.id) { mutableStateOf(expense.description) }
        var editCategoryId by remember(expense.id) { mutableStateOf(expense.categoryId) }

        AlertDialog(
            onDismissRequest = { showEditExpenseDialog = null },
            title = { Text("Gideri Düzenle") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Kategori", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(uiState.allCategories) { cat ->
                            val isSelected = editCategoryId == cat.id
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { editCategoryId = cat.id }.padding(2.dp)
                            ) {
                                Box(
                                    modifier = Modifier.size(40.dp).clip(CircleShape)
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) { Text(cat.icon, fontSize = 18.sp) }
                                Text(cat.name, fontSize = 9.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1)
                            }
                        }
                    }
                    OutlinedTextField(
                        value = editAmount,
                        onValueChange = { editAmount = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Tutar (₺)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editDescription,
                        onValueChange = { editDescription = it },
                        label = { Text("Açıklama (isteğe bağlı)") },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val amount = editAmount.toDoubleOrNull()
                    if (amount != null && amount > 0) {
                        viewModel.updateExpense(expense, amount, editDescription.trim(), editCategoryId)
                        showEditExpenseDialog = null
                    }
                }) { Text("Kaydet") }
            },
            dismissButton = { TextButton(onClick = { showEditExpenseDialog = null }) { Text("İptal") } }
        )
    }
}

@Composable
private fun SummaryCard(modifier: Modifier, emoji: String, label: String, value: String, valueColor: Color) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("$emoji $label", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = valueColor)
        }
    }
}

@Composable
private fun IncomeItemRow(item: IncomeItemEntity, onEdit: (IncomeItemEntity) -> Unit, onDelete: (IncomeItemEntity) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) { Text("💵", fontSize = 20.sp) }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = if (item.description.isNotBlank()) item.description else "Gelir",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Text(formatCurrency(item.amount), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = IncomeGreen)
            Spacer(modifier = Modifier.width(12.dp))
            Icon(Icons.Default.Edit, contentDescription = "Düzenle",
                modifier = Modifier.size(16.dp).clickable { onEdit(item) },
                tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.Delete, contentDescription = "Sil",
                modifier = Modifier.size(16.dp).clickable { onDelete(item) },
                tint = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun IncomeDialog(
    title: String,
    initialAmount: String,
    initialDescription: String,
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit
) {
    var amountInput by remember { mutableStateOf(initialAmount) }
    var descriptionInput by remember { mutableStateOf(initialDescription) }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Tutar (₺)") },
                    prefix = { Text("₺ ", fontWeight = FontWeight.Bold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = showError && amountInput.isBlank(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = descriptionInput,
                    onValueChange = { descriptionInput = it },
                    label = { Text("Açıklama (isteğe bağlı)") },
                    placeholder = { Text("Örn: Maaş, Freelance...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (showError) Text("Lütfen geçerli bir tutar girin.", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val amount = amountInput.toDoubleOrNull()
                if (amount == null || amount <= 0) { showError = true }
                else onConfirm(amount, descriptionInput.trim())
            }) { Text("Kaydet") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("İptal") } }
    )
}

fun formatCurrency(amount: Double): String {
    return if (amount == amount.toLong().toDouble()) {
        "₺${"%,d".format(amount.toLong())}".replace(',', '.')
    } else {
        "₺${"%.2f".format(amount)}"
    }
}

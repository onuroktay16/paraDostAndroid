package com.scoreplus.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.scoreplus.app.data.local.entity.CategoryEntity
import com.scoreplus.app.data.local.entity.ExpenseEntity
import com.scoreplus.app.data.local.entity.IncomeItemEntity
import com.scoreplus.app.data.local.entity.SavingsEntity
import com.scoreplus.app.data.repository.FinanceRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

data class CategoryExpenseItem(
    val category: CategoryEntity,
    val expenses: List<ExpenseEntity>,
    val total: Double
)

data class HomeUiState(
    val selectedMonth: Int,
    val selectedYear: Int,
    val incomeItems: List<IncomeItemEntity>,
    val categoryExpenses: List<CategoryExpenseItem>,
    val allCategories: List<CategoryEntity>,
    val savings: SavingsEntity? = null,
    val isLoading: Boolean = true
) {
    val totalIncome: Double get() = incomeItems.sumOf { it.amount }
    val totalExpenses: Double get() = categoryExpenses.sumOf { it.total }
    val balance: Double get() = totalIncome - totalExpenses
    // Kullanıcı özel birikim girmişse onu, yoksa hesaplanan bakiyeyi göster
    val effectiveSavings: Double get() = savings?.amount ?: balance
    val isSavingsCustom: Boolean get() = savings != null
}

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val repository: FinanceRepository,
    private val initialMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    private val initialYear: Int = Calendar.getInstance().get(Calendar.YEAR)
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(initialMonth)
    private val _selectedYear = MutableStateFlow(initialYear)

    private val incomeItems: StateFlow<List<IncomeItemEntity>> = combine(_selectedMonth, _selectedYear) { m, y ->
        repository.getIncomeItemsByMonthYear(m, y)
    }.flatMapLatest { it }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val expenses: StateFlow<List<ExpenseEntity>> = combine(_selectedMonth, _selectedYear) { m, y ->
        repository.getExpensesByMonthYear(m, y)
    }.flatMapLatest { it }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val categories: StateFlow<List<CategoryEntity>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val savings: StateFlow<SavingsEntity?> = combine(_selectedMonth, _selectedYear) { m, y ->
        repository.getSavingsByMonthYear(m, y)
    }.flatMapLatest { it }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private data class CoreState(
        val month: Int, val year: Int,
        val incomeItems: List<IncomeItemEntity>,
        val expenses: List<ExpenseEntity>
    )

    private val coreState: StateFlow<CoreState> = combine(
        _selectedMonth, _selectedYear, incomeItems, expenses
    ) { month, year, inc, exp ->
        CoreState(month, year, inc, exp)
    }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000),
        CoreState(initialMonth, initialYear, emptyList(), emptyList())
    )

    val uiState: StateFlow<HomeUiState> = combine(
        coreState, categories, savings
    ) { core, cats, sav ->
        val grouped = cats.map { category ->
            val catExpenses = core.expenses.filter { it.categoryId == category.id }
                .sortedByDescending { it.amount }
            CategoryExpenseItem(category = category, expenses = catExpenses, total = catExpenses.sumOf { it.amount })
        }.filter { it.total > 0 }
            .sortedByDescending { it.total }

        HomeUiState(
            selectedMonth = core.month,
            selectedYear = core.year,
            incomeItems = core.incomeItems.sortedByDescending { it.amount },
            categoryExpenses = grouped,
            allCategories = cats,
            savings = sav,
            isLoading = false
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        HomeUiState(
            selectedMonth = initialMonth,
            selectedYear = initialYear,
            incomeItems = emptyList(),
            categoryExpenses = emptyList(),
            allCategories = emptyList()
        )
    )

    fun previousMonth() {
        val month = _selectedMonth.value
        val year = _selectedYear.value
        // Ocak 2026'dan öncesine gitme
        if (year == 2026 && month == 1) return
        if (month == 1) { _selectedMonth.value = 12; _selectedYear.value = year - 1 }
        else _selectedMonth.value = month - 1
    }

    fun nextMonth() {
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val month = _selectedMonth.value
        val year = _selectedYear.value
        if (year < currentYear || (year == currentYear && month < currentMonth)) {
            if (month == 12) { _selectedMonth.value = 1; _selectedYear.value = year + 1 }
            else _selectedMonth.value = month + 1
        }
    }

    fun addIncomeItem(amount: Double, description: String) {
        viewModelScope.launch {
            repository.insertIncomeItem(
                IncomeItemEntity(
                    amount = amount,
                    description = description,
                    month = _selectedMonth.value,
                    year = _selectedYear.value
                )
            )
        }
    }

    fun updateIncomeItem(item: IncomeItemEntity, newAmount: Double, newDescription: String) {
        viewModelScope.launch {
            repository.updateIncomeItem(item.copy(amount = newAmount, description = newDescription))
        }
    }

    fun deleteIncomeItem(item: IncomeItemEntity) {
        viewModelScope.launch { repository.deleteIncomeItem(item) }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch { repository.deleteExpense(expense) }
    }

    fun updateExpense(expense: ExpenseEntity, newAmount: Double, newDescription: String, newCategoryId: Int) {
        viewModelScope.launch {
            repository.updateExpense(expense.copy(amount = newAmount, description = newDescription, categoryId = newCategoryId))
        }
    }

    fun saveSavings(amount: Double) {
        viewModelScope.launch {
            repository.upsertSavings(
                SavingsEntity(month = _selectedMonth.value, year = _selectedYear.value, amount = amount)
            )
        }
    }

    fun resetSavings() {
        viewModelScope.launch {
            repository.deleteSavings(_selectedMonth.value, _selectedYear.value)
        }
    }
}

class HomeViewModelFactory(
    private val repository: FinanceRepository,
    private val initialMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    private val initialYear: Int = Calendar.getInstance().get(Calendar.YEAR)
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repository, initialMonth, initialYear) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

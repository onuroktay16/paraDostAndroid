package com.scoreplus.app.ui.screens.yearly

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.scoreplus.app.data.local.entity.CategoryEntity
import com.scoreplus.app.data.repository.FinanceRepository
import kotlinx.coroutines.flow.*

data class YearlyCategoryItem(
    val category: CategoryEntity,
    val total: Double,
    val percent: Double
)

data class YearlyUiState(
    val year: Int = 2026,
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val categoryItems: List<YearlyCategoryItem> = emptyList(),
    val isLoading: Boolean = true
) {
    val net: Double get() = totalIncome - totalExpenses
}

class YearlyViewModel(private val repository: FinanceRepository) : ViewModel() {

    private val _year = MutableStateFlow(2026)

    private val categories: StateFlow<List<CategoryEntity>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<YearlyUiState> = combine(
        _year,
        categories
    ) { year, cats ->
        Pair(year, cats)
    }.flatMapLatest { (year, cats) ->
        combine(
            repository.getYearlyExpensesByCategory(year),
            repository.getTotalIncomeByYear(year)
        ) { yearlySummaries, totalIncome ->
            val totalExpenses = yearlySummaries.sumOf { it.total }
            val items = yearlySummaries.mapNotNull { summary ->
                val cat = cats.find { it.id == summary.categoryId } ?: return@mapNotNull null
                YearlyCategoryItem(
                    category = cat,
                    total = summary.total,
                    percent = if (totalExpenses > 0) summary.total / totalExpenses * 100 else 0.0
                )
            }
            YearlyUiState(
                year = year,
                totalIncome = totalIncome ?: 0.0,
                totalExpenses = totalExpenses,
                categoryItems = items,
                isLoading = false
            )
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        YearlyUiState()
    )
}

class YearlyViewModelFactory(private val repository: FinanceRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(YearlyViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return YearlyViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

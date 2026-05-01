package com.scoreplus.app.ui.screens.addexpense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.scoreplus.app.data.local.entity.CategoryEntity
import com.scoreplus.app.data.local.entity.ExpenseEntity
import com.scoreplus.app.data.repository.FinanceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AddExpenseViewModel(private val repository: FinanceRepository) : ViewModel() {

    val categories: StateFlow<List<CategoryEntity>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveExpense(
        categoryId: Int,
        amount: Double,
        description: String,
        month: Int,
        year: Int,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            repository.insertExpense(
                ExpenseEntity(
                    categoryId = categoryId,
                    amount = amount,
                    description = description,
                    month = month,
                    year = year
                )
            )
            onSuccess()
        }
    }
}

class AddExpenseViewModelFactory(private val repository: FinanceRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddExpenseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AddExpenseViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

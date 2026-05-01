package com.scoreplus.app.ui.screens.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.scoreplus.app.data.local.entity.CategoryEntity
import com.scoreplus.app.data.repository.FinanceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CategoryViewModel(private val repository: FinanceRepository) : ViewModel() {

    val categories: StateFlow<List<CategoryEntity>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addCategory(name: String, icon: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.insertCategory(
                CategoryEntity(name = name.trim(), icon = icon.ifBlank { "📌" }, isDefault = false)
            )
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        if (category.isDefault) return
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }
}

class CategoryViewModelFactory(private val repository: FinanceRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CategoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CategoryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

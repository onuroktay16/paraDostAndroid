package com.scoreplus.app.ui.screens.auth

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.scoreplus.app.ScorePlusApp
import com.scoreplus.app.data.remote.NetworkClient
import com.scoreplus.app.data.remote.SyncWorker
import com.scoreplus.app.data.remote.TokenStore
import com.scoreplus.app.data.remote.dto.CategoryRequest
import com.scoreplus.app.data.remote.dto.LoginRequest
import com.scoreplus.app.data.remote.dto.RefreshRequest
import com.scoreplus.app.data.remote.dto.RegisterRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val isLoggedOut: Boolean = false
)

class AuthViewModel(private val tokenStore: TokenStore, private val context: Context) : ViewModel() {

    private val api = NetworkClient.create(tokenStore)

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState

    val isLoggedIn = tokenStore.isLoggedIn
    val userEmail  = tokenStore.email

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState(error = "Email ve şifre boş olamaz")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            try {
                val response = api.login(LoginRequest(email.trim(), password))
                if (response.isSuccessful) {
                    val body = response.body()!!
                    tokenStore.saveAuth(body.accessToken, body.refreshToken, body.userId, body.email)
                    val app = context.applicationContext as ScorePlusApp
                    app.applicationScope.launch {
                        pullFromBackend()
                        syncCategoriesNow()
                        SyncWorker.syncNow(context)
                    }
                    _uiState.value = AuthUiState(isSuccess = true)
                } else {
                    val msg = when (response.code()) {
                        401  -> "Email veya şifre yanlış"
                        else -> "Giriş başarısız (${response.code()})"
                    }
                    _uiState.value = AuthUiState(error = msg)
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState(error = "Sunucuya bağlanılamadı. İnternet bağlantını kontrol et.")
            }
        }
    }

    fun register(email: String, password: String, confirmPassword: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState(error = "Email ve şifre boş olamaz")
            return
        }
        if (password != confirmPassword) {
            _uiState.value = AuthUiState(error = "Şifreler eşleşmiyor")
            return
        }
        if (password.length < 6) {
            _uiState.value = AuthUiState(error = "Şifre en az 6 karakter olmalı")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            try {
                val response = api.register(RegisterRequest(email.trim(), password))
                if (response.isSuccessful) {
                    val body = response.body()!!
                    tokenStore.saveAuth(body.accessToken, body.refreshToken, body.userId, body.email)
                    syncCategoriesNow()
                    SyncWorker.syncNow(context)
                    _uiState.value = AuthUiState(isSuccess = true)
                } else {
                    val msg = when (response.code()) {
                        409  -> "Bu email adresi zaten kayıtlı"
                        else -> "Kayıt başarısız (${response.code()})"
                    }
                    _uiState.value = AuthUiState(error = msg)
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState(error = "Sunucuya bağlanılamadı. İnternet bağlantını kontrol et.")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                val refresh = tokenStore.refreshToken.firstOrNull()
                if (refresh != null) {
                    api.logout(RefreshRequest(refresh))
                }
            } catch (_: Exception) {}
            tokenStore.clearAuth()
            _uiState.value = AuthUiState(isLoggedOut = true)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private suspend fun pullFromBackend() {
        val app = context.applicationContext as ScorePlusApp
        val remoteApi = app.api
        val db = app.database
        val categoryMapping = mutableMapOf<Int, Int>()

        // 1. Kategoriler
        try {
            val remoteCategories = remoteApi.getCategories()
            if (remoteCategories.isSuccessful) {
                remoteCategories.body()?.forEach { remote ->
                    try {
                        val existing = db.categoryDao().getByServerId(remote.id)
                        if (existing != null) {
                            if (remote.localId != null) categoryMapping[remote.localId] = existing.id
                        } else {
                            val matchByName = db.categoryDao().getByName(remote.name)
                            if (matchByName != null) {
                                db.categoryDao().updateServerIdAndSynced(matchByName.id, remote.id)
                                if (remote.localId != null) categoryMapping[remote.localId] = matchByName.id
                            } else {
                                val newId = db.categoryDao().insertCategory(
                                    com.scoreplus.app.data.local.entity.CategoryEntity(
                                        name = remote.name, icon = remote.icon,
                                        isDefault = remote.isDefault, serverId = remote.id, isSynced = true
                                    )
                                )
                                if (remote.localId != null) categoryMapping[remote.localId] = newId.toInt()
                            }
                        }
                    } catch (e: Exception) { Log.w("AuthViewModel", "Category pull item failed: ${e.message}") }
                }
            }
        } catch (e: Exception) { Log.w("AuthViewModel", "Category pull failed: ${e.message}") }

        // 2. Gelirler
        try {
            remoteApi.getIncome().body()?.forEach { remote ->
                try {
                    if (db.incomeDao().getByServerId(remote.id) == null) {
                        db.incomeDao().insertIncomeItem(
                            com.scoreplus.app.data.local.entity.IncomeItemEntity(
                                amount = remote.amount, description = remote.description,
                                date = remote.date.toLong(), month = remote.month, year = remote.year,
                                serverId = remote.id, isSynced = true
                            )
                        )
                    }
                } catch (e: Exception) { Log.w("AuthViewModel", "Income pull item failed: ${e.message}") }
            }
        } catch (e: Exception) { Log.w("AuthViewModel", "Income pull failed: ${e.message}") }

        // 3. Giderler
        try {
            remoteApi.getExpenses().body()?.forEach { remote ->
                try {
                    if (db.expenseDao().getByServerId(remote.id) == null) {
                        val localCategoryId = categoryMapping[remote.categoryLocalId] ?: remote.categoryLocalId
                        db.expenseDao().insertExpense(
                            com.scoreplus.app.data.local.entity.ExpenseEntity(
                                categoryId = localCategoryId, amount = remote.amount,
                                description = remote.description, date = remote.date.toLong(),
                                month = remote.month, year = remote.year,
                                serverId = remote.id, isSynced = true
                            )
                        )
                    }
                } catch (e: Exception) { Log.w("AuthViewModel", "Expense pull item failed: ${e.message}") }
            }
        } catch (e: Exception) { Log.w("AuthViewModel", "Expenses pull failed: ${e.message}") }

        // 4. Birikimler
        try {
            remoteApi.getSavings().body()?.forEach { remote ->
                try {
                    if (db.savingsDao().getSavingsByMonthYearSync(remote.month, remote.year) == null) {
                        db.savingsDao().upsertSavings(
                            com.scoreplus.app.data.local.entity.SavingsEntity(
                                month = remote.month, year = remote.year, amount = remote.amount, isSynced = true
                            )
                        )
                    }
                } catch (e: Exception) { Log.w("AuthViewModel", "Savings pull item failed: ${e.message}") }
            }
        } catch (e: Exception) { Log.w("AuthViewModel", "Savings pull failed: ${e.message}") }

        Log.d("AuthViewModel", "Pull from backend completed. CategoryMapping: $categoryMapping")
    }

    private suspend fun syncCategoriesNow() {
        try {
            val db = (context.applicationContext as ScorePlusApp).database
            val categories = db.categoryDao().getUnsyncedCategories()
            if (categories.isEmpty()) return
            val requests = categories.map { CategoryRequest(it.name, it.icon, it.isDefault, it.id) }
            val resp = api.bulkCreateCategories(requests)
            if (resp.isSuccessful) {
                resp.body()?.forEach { remote ->
                    val local = categories.find { it.id == remote.localId }
                    if (local != null) db.categoryDao().markCategorySynced(local.id, remote.id)
                }
                Log.d("AuthViewModel", "Synced ${categories.size} categories")
            }
        } catch (e: Exception) {
            Log.w("AuthViewModel", "Category sync failed: ${e.message}")
        }
    }
}

class AuthViewModelFactory(private val tokenStore: TokenStore, private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(tokenStore, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

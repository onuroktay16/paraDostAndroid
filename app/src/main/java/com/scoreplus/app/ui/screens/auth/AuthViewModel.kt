package com.scoreplus.app.ui.screens.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.scoreplus.app.data.remote.NetworkClient
import com.scoreplus.app.data.remote.SyncWorker
import com.scoreplus.app.data.remote.TokenStore
import com.scoreplus.app.data.remote.dto.LoginRequest
import com.scoreplus.app.data.remote.dto.RefreshRequest
import com.scoreplus.app.data.remote.dto.RegisterRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

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
                    SyncWorker.syncNow(context)
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

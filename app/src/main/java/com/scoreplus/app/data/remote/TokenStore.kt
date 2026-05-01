package com.scoreplus.app.data.remote

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

class TokenStore(private val context: Context) {

    companion object {
        private val KEY_ACCESS_TOKEN   = stringPreferencesKey("access_token")
        private val KEY_REFRESH_TOKEN  = stringPreferencesKey("refresh_token")
        private val KEY_USER_ID        = intPreferencesKey("user_id")
        private val KEY_EMAIL          = stringPreferencesKey("email")
        private val KEY_GUEST_MODE     = booleanPreferencesKey("guest_mode")
    }

    val accessToken: Flow<String?> = context.dataStore.data.map { it[KEY_ACCESS_TOKEN] }
    val refreshToken: Flow<String?> = context.dataStore.data.map { it[KEY_REFRESH_TOKEN] }
    val userId: Flow<Int?> = context.dataStore.data.map { it[KEY_USER_ID] }
    val email: Flow<String?> = context.dataStore.data.map { it[KEY_EMAIL] }

    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map {
        it[KEY_ACCESS_TOKEN]?.isNotBlank() == true
    }

    // Giriş yapmadan devam et
    val isGuestMode: Flow<Boolean> = context.dataStore.data.map {
        it[KEY_GUEST_MODE] == true
    }

    // Giriş yapılmış VEYA misafir modunda → ana ekrana git
    val shouldShowHome: Flow<Boolean> = context.dataStore.data.map {
        it[KEY_ACCESS_TOKEN]?.isNotBlank() == true || it[KEY_GUEST_MODE] == true
    }

    suspend fun continueAsGuest() {
        context.dataStore.edit { it[KEY_GUEST_MODE] = true }
    }

    suspend fun saveAuth(accessToken: String, refreshToken: String, userId: Int, email: String) {
        context.dataStore.edit {
            it[KEY_ACCESS_TOKEN]  = accessToken
            it[KEY_REFRESH_TOKEN] = refreshToken
            it[KEY_USER_ID]       = userId
            it[KEY_EMAIL]         = email
            it[KEY_GUEST_MODE]    = false
        }
    }

    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        context.dataStore.edit {
            it[KEY_ACCESS_TOKEN]  = accessToken
            it[KEY_REFRESH_TOKEN] = refreshToken
        }
    }

    suspend fun clearAuth() {
        context.dataStore.edit { it.clear() }
    }

    suspend fun exitGuestMode() {
        context.dataStore.edit { it[KEY_GUEST_MODE] = false }
    }
}

package com.production.supervisor.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "production_supervisor_prefs")

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
        val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
        val USER_PHONE_KEY = stringPreferencesKey("user_phone")
        val USER_NAME_KEY = stringPreferencesKey("user_name")
        val USER_ROLE_KEY = stringPreferencesKey("user_role")
    }

    val accessTokenFlow: Flow<String?> = context.dataStore.data.map { it[ACCESS_TOKEN_KEY] }
    val userNameFlow: Flow<String?> = context.dataStore.data.map { it[USER_NAME_KEY] }

    suspend fun getAccessToken(): String? = context.dataStore.data.first()[ACCESS_TOKEN_KEY]

    suspend fun saveTokens(access: String, refresh: String, phone: String, name: String, role: String) {
        context.dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN_KEY] = access
            prefs[REFRESH_TOKEN_KEY] = refresh
            prefs[USER_PHONE_KEY] = phone
            prefs[USER_NAME_KEY] = name
            prefs[USER_ROLE_KEY] = role
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}

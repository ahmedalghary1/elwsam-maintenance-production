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
        val SELECTED_FACTORY_ID_KEY = stringPreferencesKey("selected_factory_id")
        val SELECTED_FACTORY_NAME_KEY = stringPreferencesKey("selected_factory_name")
    }

    val accessTokenFlow: Flow<String?> = context.dataStore.data.map { it[ACCESS_TOKEN_KEY] }
    val userNameFlow: Flow<String?> = context.dataStore.data.map { it[USER_NAME_KEY] }
    val factoryIdFlow: Flow<String?> = context.dataStore.data.map { it[SELECTED_FACTORY_ID_KEY] }
    val factoryNameFlow: Flow<String?> = context.dataStore.data.map { it[SELECTED_FACTORY_NAME_KEY] }

    suspend fun getAccessToken(): String? = context.dataStore.data.first()[ACCESS_TOKEN_KEY]
    suspend fun getRefreshToken(): String? = context.dataStore.data.first()[REFRESH_TOKEN_KEY]
    suspend fun getSelectedFactoryId(): String? = context.dataStore.data.first()[SELECTED_FACTORY_ID_KEY]
    suspend fun getSelectedFactoryName(): String? = context.dataStore.data.first()[SELECTED_FACTORY_NAME_KEY]

    suspend fun saveTokens(access: String, refresh: String, phone: String, name: String, role: String) {
        context.dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN_KEY] = access
            prefs[REFRESH_TOKEN_KEY] = refresh
            prefs[USER_PHONE_KEY] = phone
            prefs[USER_NAME_KEY] = name
            prefs[USER_ROLE_KEY] = role
        }
    }

    suspend fun updateAccessToken(newAccess: String, newRefresh: String? = null) {
        context.dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN_KEY] = newAccess
            if (!newRefresh.isNullOrBlank()) {
                prefs[REFRESH_TOKEN_KEY] = newRefresh
            }
        }
    }

    suspend fun saveSelectedFactory(id: String?, name: String?) {
        context.dataStore.edit { prefs ->
            if (id != null) prefs[SELECTED_FACTORY_ID_KEY] = id else prefs.remove(SELECTED_FACTORY_ID_KEY)
            if (name != null) prefs[SELECTED_FACTORY_NAME_KEY] = name else prefs.remove(SELECTED_FACTORY_NAME_KEY)
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}

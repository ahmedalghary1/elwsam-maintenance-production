package com.elwsam.operations

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.unifiedSessionStore by preferencesDataStore("elwsam_unified_session")

data class UnifiedSession(val role: String, val name: String)

@Singleton
class SessionStore @Inject constructor(@ApplicationContext private val context: Context) {
    private val roleKey = stringPreferencesKey("role")
    private val nameKey = stringPreferencesKey("name")

    val session: Flow<UnifiedSession?> = context.unifiedSessionStore.data.map { values ->
        values[roleKey]?.let { UnifiedSession(it, values[nameKey].orEmpty()) }
    }

    suspend fun save(role: String, name: String) = context.unifiedSessionStore.edit {
        it[roleKey] = role
        it[nameKey] = name
    }

    suspend fun clear() = context.unifiedSessionStore.edit { it.clear() }
}

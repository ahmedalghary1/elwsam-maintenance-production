package com.maintenance.supervisor.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Base64

private val Context.tokenDataStore by preferencesDataStore("secure_session")

@Singleton
class TokenStore @Inject constructor(@ApplicationContext private val context: Context) {
    private val accessKey = stringPreferencesKey("access")
    private val refreshKey = stringPreferencesKey("refresh")
    private val alias = "maintenance_session_key"
    val sessionActive = context.tokenDataStore.data.map { preferences ->
        preferences[refreshKey]?.let(::decrypt) != null
    }.distinctUntilChanged()

    suspend fun accessToken(): String? = context.tokenDataStore.data.first()[accessKey]?.let(::decrypt)
    suspend fun refreshToken(): String? = context.tokenDataStore.data.first()[refreshKey]?.let(::decrypt)
    suspend fun save(access: String, refresh: String?) = context.tokenDataStore.edit {
        it[accessKey] = encrypt(access)
        if (refresh != null) it[refreshKey] = encrypt(refresh)
    }
    suspend fun clear() = context.tokenDataStore.edit { it.remove(accessKey); it.remove(refreshKey) }
    suspend fun hasSession(): Boolean = refreshToken() != null

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
            generateKey()
        }
    }
    private fun encrypt(raw: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val encrypted = cipher.doFinal(raw.toByteArray())
        val packed = ByteBuffer.allocate(4 + cipher.iv.size + encrypted.size)
            .putInt(cipher.iv.size).put(cipher.iv).put(encrypted).array()
        return Base64.encodeToString(packed, Base64.NO_WRAP)
    }
    private fun decrypt(encoded: String): String? = runCatching {
        val buffer = ByteBuffer.wrap(Base64.decode(encoded, Base64.NO_WRAP)); val iv = ByteArray(buffer.int); buffer.get(iv)
        val encrypted = ByteArray(buffer.remaining()); buffer.get(encrypted)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv)) }
        String(cipher.doFinal(encrypted))
    }.getOrNull()
}

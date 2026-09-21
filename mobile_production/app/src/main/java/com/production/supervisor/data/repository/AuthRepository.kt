package com.production.supervisor.data.repository

import com.production.supervisor.data.local.datastore.TokenManager
import com.production.supervisor.data.remote.api.AuthApiService
import com.production.supervisor.data.remote.dto.LoginRequestDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authApiService: AuthApiService,
    private val tokenManager: TokenManager
) {
    val isLoggedInFlow: Flow<Boolean> = tokenManager.accessTokenFlow.map { !it.isNullOrBlank() }
    val userNameFlow: Flow<String?> = tokenManager.userNameFlow


    suspend fun login(phone: String, pass: String): Result<Unit> {
        return try {
            val res = authApiService.login(LoginRequestDto(phone.trim(), pass))
            if (res.isSuccessful && res.body() != null) {
                val body = res.body()!!
                tokenManager.saveTokens(
                    access = body.access,
                    refresh = body.refresh,
                    phone = body.user?.phone ?: phone,
                    name = body.user?.name ?: phone,
                    role = body.user?.role ?: ""
                )
                Result.success(Unit)
            } else {
                val err = res.errorBody()?.string() ?: "بيانات الدخول غير صحيحة"
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout() {
        tokenManager.clear()
    }
}

package com.elwsam.operations

import com.maintenance.supervisor.data.security.TokenStore
import com.production.supervisor.data.local.datastore.TokenManager
import com.production.supervisor.data.repository.AuthRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UnifiedAuthRepository @Inject constructor(
    private val productionAuth: AuthRepository,
    private val productionTokens: TokenManager,
    private val maintenanceTokens: TokenStore,
    private val sessions: SessionStore
) {
    val session = sessions.session

    suspend fun login(phone: String, password: String): Result<Unit> {
        val result = productionAuth.login(phone, password)
        if (result.isFailure) return result

        val role = productionTokens.userRoleFlow.first().orEmpty()
        if (role !in SUPPORTED_ROLES) {
            logout()
            return Result.failure(IllegalStateException("لا توجد صلاحية لاستخدام تطبيقات التشغيل أو الصيانة."))
        }
        val access = productionTokens.getAccessTokenValue()
        val refresh = productionTokens.getRefreshTokenValue()
        if (access.isNullOrBlank() || refresh.isNullOrBlank()) {
            logout()
            return Result.failure(IllegalStateException("لم يرسل الخادم جلسة صالحة."))
        }
        maintenanceTokens.save(access, refresh)
        val name = productionTokens.userNameFlow.first().orEmpty().ifBlank { phone.trim() }
        sessions.save(role, name)
        return Result.success(Unit)
    }

    suspend fun logout() {
        productionAuth.logout()
        maintenanceTokens.clear()
        sessions.clear()
    }

    companion object {
        val SUPPORTED_ROLES = setOf("MAINTENANCE_SUPERVISOR", "PRODUCTION_SUPERVISOR", "GENERAL_SUPERVISOR", "ADMIN")
    }
}

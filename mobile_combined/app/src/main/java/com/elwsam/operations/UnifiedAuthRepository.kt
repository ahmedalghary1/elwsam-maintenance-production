package com.elwsam.operations

import com.maintenance.supervisor.data.security.MaintenanceSession
import com.production.supervisor.data.local.datastore.TokenManager
import com.production.supervisor.data.repository.AuthRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import com.elwsam.operations.updates.UpdateCoordinator

@Singleton
class UnifiedAuthRepository @Inject constructor(
    private val productionAuth: AuthRepository,
    private val productionTokens: TokenManager,
    private val maintenanceSession: MaintenanceSession,
    private val sessions: SessionStore,
    private val updates: UpdateCoordinator
) {
    val session = sessions.session

    suspend fun login(phone: String, password: String): Result<Unit> {
        // Retire the old maintenance cache before installing another identity.
        maintenanceSession.clear()
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
        val userId = productionTokens.getUserId()
        if (userId == null) {
            logout()
            return Result.failure(IllegalStateException("تعذر تحديد بيانات الحساب. أعد تسجيل الدخول."))
        }
        if (role in setOf("MAINTENANCE_SUPERVISOR", "GENERAL_SUPERVISOR")) {
            maintenanceSession.activate(access, refresh, userId)
        }
        val name = productionTokens.userNameFlow.first().orEmpty().ifBlank { phone.trim() }
        sessions.save(role, name)
        updates.check(force = true)
        return Result.success(Unit)
    }

    suspend fun logout() {
        productionAuth.logout()
        maintenanceSession.clear()
        sessions.clear()
    }

    companion object {
        val SUPPORTED_ROLES = setOf("MAINTENANCE_SUPERVISOR", "PRODUCTION_SUPERVISOR", "GENERAL_SUPERVISOR", "ADMIN")
    }
}

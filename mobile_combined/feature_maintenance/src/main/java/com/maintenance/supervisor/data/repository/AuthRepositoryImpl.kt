package com.maintenance.supervisor.data.repository

import com.maintenance.supervisor.data.remote.LoginRequest
import com.maintenance.supervisor.data.remote.MaintenanceApi
import com.maintenance.supervisor.data.security.TokenStore
import com.maintenance.supervisor.data.security.MaintenanceSession
import kotlinx.coroutines.CancellationException
import com.maintenance.supervisor.domain.repository.AppResult
import com.maintenance.supervisor.domain.repository.AuthRepository
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(private val api: MaintenanceApi, private val tokens: TokenStore, private val session: MaintenanceSession) : AuthRepository {
    override suspend fun hasSession() = tokens.hasSession()
    override suspend fun login(phone: String, password: String): AppResult<Unit> {
        return try {
        val response = api.login(LoginRequest(phone.trim(), password))
        val refresh = response.refresh ?: return AppResult.Error("لم يرسل الخادم جلسة صالحة.")
        tokens.save(response.access, refresh)
        val user = api.me()
        if (user.role !in setOf("MAINTENANCE_SUPERVISOR", "GENERAL_SUPERVISOR")) {
            session.clear()
            return AppResult.Error("هذا التطبيق متاح لمشرفي الصيانة فقط.")
        }
        session.activate(response.access, refresh, user.id)
        AppResult.Success(Unit)
    } catch (t: Throwable) {
        if (t is CancellationException) throw t
        session.clear()
        AppResult.Error("تعذر تسجيل الدخول. تأكد من رقم الهاتف وكلمة المرور والاتصال.", t)
    }
    }

    override suspend fun logout() {
        session.clear()
    }
}

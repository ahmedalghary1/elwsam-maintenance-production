package com.production.supervisor.data.remote

import com.production.supervisor.data.local.datastore.TokenManager
import com.production.supervisor.data.remote.api.AuthApiService
import com.production.supervisor.data.remote.dto.RefreshTokenRequestDto
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import java.util.concurrent.locks.ReentrantLock
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import kotlin.concurrent.withLock

data class RequestOwner(val userId: Int?)

@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenManager: TokenManager,
    private val authApiProvider: Provider<AuthApiService>
) : Authenticator {

    private val lock = ReentrantLock()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 3) return null

        return lock.withLock {
            runBlocking {
                val owner = response.request.tag(RequestOwner::class.java) ?: return@runBlocking null
                val (access, userId) = tokenManager.requestIdentity()
                if (owner.userId != userId) return@runBlocking null
                val oldHeader = response.request.header("Authorization")
                val currentToken = access?.let { "Bearer $it" }

                if (currentToken != null && currentToken != oldHeader) {
                    return@runBlocking response.request.newBuilder()
                        .header("Authorization", currentToken)
                        .build()
                }

                val refreshToken = tokenManager.getRefreshToken() ?: return@runBlocking null
                try {
                    val authApi = authApiProvider.get()
                    val refreshResponse = authApi.refreshToken(RefreshTokenRequestDto(refreshToken))
                    // A response from a retired account must not replace the new login.
                    if (refreshResponse.isSuccessful && refreshResponse.body() != null) {
                        val body = refreshResponse.body()!!
                        if (!tokenManager.saveRefreshed(refreshToken, body.access, body.refresh)) return@runBlocking null
                        response.request.newBuilder()
                            .header("Authorization", "Bearer ${body.access}")
                            .build()
                    } else {
                        if (refreshResponse.code() == 401 || refreshResponse.code() == 400) {
                            tokenManager.clearIfCurrent(refreshToken)
                        }
                        null
                    }
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}

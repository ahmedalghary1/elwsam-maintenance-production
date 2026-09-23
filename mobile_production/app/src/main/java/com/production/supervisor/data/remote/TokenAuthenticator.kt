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
                val oldHeader = response.request.header("Authorization")
                val currentToken = tokenManager.getAccessToken()?.let { "Bearer $it" }

                if (currentToken != null && currentToken != oldHeader) {
                    return@runBlocking response.request.newBuilder()
                        .header("Authorization", currentToken)
                        .build()
                }

                val refreshToken = tokenManager.getRefreshToken() ?: return@runBlocking null
                try {
                    val authApi = authApiProvider.get()
                    val refreshResponse = authApi.refreshToken(RefreshTokenRequestDto(refreshToken))
                    if (refreshResponse.isSuccessful && refreshResponse.body() != null) {
                        val body = refreshResponse.body()!!
                        tokenManager.updateAccessToken(body.access, body.refresh)
                        response.request.newBuilder()
                            .header("Authorization", "Bearer ${body.access}")
                            .build()
                    } else {
                        if (refreshResponse.code() == 401 || refreshResponse.code() == 400) {
                            tokenManager.clear()
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

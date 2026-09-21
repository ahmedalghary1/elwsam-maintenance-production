package com.maintenance.supervisor.data.remote

import com.maintenance.supervisor.data.security.TokenStore
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import java.util.concurrent.locks.ReentrantLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.withLock

class BearerInterceptor @Inject constructor(private val tokens: TokenStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { tokens.accessToken() }
        val request = if (token == null) chain.request() else chain.request().newBuilder().header("Authorization", "Bearer $token").build()
        return chain.proceed(request)
    }
}

@Singleton
class TokenAuthenticator @Inject constructor(private val tokens: TokenStore, private val refreshApi: RefreshApi) : Authenticator {
    private val lock = ReentrantLock()
    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2) return null
        return lock.withLock {
            runBlocking {
                val old = response.request.header("Authorization")
                val current = tokens.accessToken()?.let { "Bearer $it" }
                if (current != null && current != old) return@runBlocking response.request.newBuilder().header("Authorization", current).build()
                val refresh = tokens.refreshToken() ?: return@runBlocking null
                try {
                    val result = refreshApi.refresh(RefreshRequest(refresh))
                    tokens.save(result.access, result.refresh ?: refresh)
                    response.request.newBuilder().header("Authorization", "Bearer ${result.access}").build()
                } catch (e: retrofit2.HttpException) {
                    if (e.code() == 401 || e.code() == 400) tokens.clear()
                    null
                } catch (e: java.io.IOException) {
                    null // Network error, do not clear tokens
                } catch (e: Exception) {
                    tokens.clear()
                    null
                }
            }
        }
    }
    private fun responseCount(response: Response): Int { var count = 1; var prior = response.priorResponse; while (prior != null) { count++; prior = prior.priorResponse }; return count }
}

interface RefreshApi { @retrofit2.http.POST("api/v1/auth/refresh/") suspend fun refresh(@retrofit2.http.Body body: RefreshRequest): TokenResponse }

package com.maintenance.supervisor.data.remote

import com.maintenance.supervisor.data.security.TokenStore
import io.mockk.*
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.*
import org.junit.Test

class TokenAuthenticatorTest {
    private val tokens = mockk<TokenStore>()
    private val api = mockk<RefreshApi>()
    private val authenticator = TokenAuthenticator(tokens, api)

    private fun unauthorized(userId: Int) = Response.Builder()
        .request(Request.Builder().url("https://example.test/api/v1/mobile/bootstrap/")
            .header("Authorization", "Bearer old-access")
            .tag(RequestOwner::class.java, RequestOwner(userId)).build())
        .protocol(Protocol.HTTP_1_1).code(401).message("Unauthorized").build()

    @Test fun oldRequestIsNeverReplayedWithNewAccountsCredentials() {
        coEvery { tokens.requestIdentity() } returns ("new-access" to 2)
        assertNull(authenticator.authenticate(null, unauthorized(1)))
        coVerify(exactly = 0) { api.refresh(any()) }
    }

    @Test fun refreshFromRetiredSessionCannotReplaceCurrentLogin() {
        coEvery { tokens.requestIdentity() } returns ("old-access" to 1)
        coEvery { tokens.refreshToken() } returns "old-refresh"
        coEvery { api.refresh(RefreshRequest("old-refresh")) } returns TokenResponse("refreshed-access", "refreshed-refresh")
        coEvery { tokens.saveRefreshed("old-refresh", "refreshed-access", "refreshed-refresh") } returns false
        assertNull(authenticator.authenticate(null, unauthorized(1)))
        coVerify(exactly = 0) { tokens.save(any(), any()) }
        coVerify(exactly = 0) { tokens.clear() }
    }

    @Test fun currentAccountCanRefreshAndRetryItsOwnRequest() {
        coEvery { tokens.requestIdentity() } returns ("old-access" to 1)
        coEvery { tokens.refreshToken() } returns "old-refresh"
        coEvery { api.refresh(any()) } returns TokenResponse("refreshed-access", "refreshed-refresh")
        coEvery { tokens.saveRefreshed("old-refresh", "refreshed-access", "refreshed-refresh") } returns true
        assertEquals("Bearer refreshed-access", authenticator.authenticate(null, unauthorized(1))?.header("Authorization"))
    }
}

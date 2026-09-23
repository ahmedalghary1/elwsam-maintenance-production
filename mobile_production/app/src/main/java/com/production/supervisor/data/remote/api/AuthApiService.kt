package com.production.supervisor.data.remote.api

import com.production.supervisor.data.remote.dto.LoginRequestDto
import com.production.supervisor.data.remote.dto.LoginResponseDto
import com.production.supervisor.data.remote.dto.RefreshTokenRequestDto
import com.production.supervisor.data.remote.dto.RefreshTokenResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {
    @POST("api/v1/auth/login/")
    suspend fun login(@Body body: LoginRequestDto): Response<LoginResponseDto>

    @POST("api/v1/auth/refresh/")
    suspend fun refreshToken(@Body body: RefreshTokenRequestDto): Response<RefreshTokenResponseDto>
}

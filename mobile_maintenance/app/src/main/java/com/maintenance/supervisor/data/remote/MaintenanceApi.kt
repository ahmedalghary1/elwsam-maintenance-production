package com.maintenance.supervisor.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface MaintenanceApi {
    @POST("api/v1/auth/login/") suspend fun login(@Body body: LoginRequest): TokenResponse
    @POST("api/v1/auth/refresh/") suspend fun refresh(@Body body: RefreshRequest): TokenResponse
    @GET("api/v1/auth/me/") suspend fun me(): UserDto
    @GET("api/v1/mobile/bootstrap/") suspend fun bootstrap(): BootstrapDto
    @GET("api/v1/mobile/current-maintenance/") suspend fun currentMaintenance(): BootstrapCurrentDto
    @POST("api/v1/mobile/current-maintenance/select/") suspend fun selectCurrentAsset(@Body body: CurrentAssetSelectionRequest): BootstrapCurrentDto
    @POST("api/v1/mobile/reports/") suspend fun createReport(@Body body: ReportInput): ReportResponseDto
    @PUT("api/v1/mobile/reports/{id}/") suspend fun updateReport(@Path("id") id: Int, @Body body: ReportInput): ReportResponseDto
    @POST("api/v1/mobile/sync/reports/") suspend fun syncReports(@Body body: BatchSyncRequest): BatchSyncResponse
}

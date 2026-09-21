package com.production.supervisor.data.remote.api

import com.production.supervisor.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ProductionApiService {

    @GET("api/v1/production/bootstrap/")
    suspend fun getBootstrap(): Response<ProductionBootstrapDto>

    @POST("api/v1/production/sync/shift-reports/")
    suspend fun syncShiftReport(@Body body: SyncShiftReportInputDto): Response<ShiftReportDto>

    @POST("api/v1/production/reports/confirm-handover/")
    suspend fun confirmHandover(@Body body: ConfirmHandoverInputDto): Response<ShiftReportDto>
}

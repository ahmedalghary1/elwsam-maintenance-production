package com.production.supervisor.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.production.supervisor.data.repository.ProductionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ProductionSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val repository: ProductionRepository
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val reportId = inputData.getString(REPORT_ID)
        val result = if (reportId != null) repository.syncReportNow(reportId) else repository.syncPendingReports()
        return if (result.isSuccess) Result.success() else Result.retry()
    }

    companion object {
        const val REPORT_ID = "client_report_id"
    }
}

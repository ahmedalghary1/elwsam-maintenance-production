package com.production.supervisor.data.sync

import android.content.Context
import androidx.work.*
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductionSyncScheduler @Inject constructor(@ApplicationContext private val context: Context) {
    private val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    fun requestSync(reportId: String) {
        val work = OneTimeWorkRequestBuilder<ProductionSyncWorker>()
            .setInputData(workDataOf(ProductionSyncWorker.REPORT_ID to reportId))
            .setConstraints(constraints)
            .setInitialDelay(1, TimeUnit.SECONDS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        // Append prevents cancelling a request already reaching the server. Later workers
        // read the latest snapshot and skip an already synced report, coalescing uploads.
        WorkManager.getInstance(context).enqueueUniqueWork(
            "production-report-$reportId", ExistingWorkPolicy.APPEND_OR_REPLACE, work
        )
    }

    fun scheduleRecovery() {
        val work = PeriodicWorkRequestBuilder<ProductionSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "production-pending-reports", ExistingPeriodicWorkPolicy.KEEP, work
        )
    }
}

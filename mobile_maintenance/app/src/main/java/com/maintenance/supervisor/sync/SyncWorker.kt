package com.maintenance.supervisor.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.maintenance.supervisor.domain.repository.AppResult
import com.maintenance.supervisor.domain.repository.MaintenanceRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: MaintenanceRepository
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = when (repository.sync()) {
        is AppResult.Success -> Result.success()
        is AppResult.Error -> if (runAttemptCount < 6) Result.retry() else Result.failure()
    }
}

@Singleton
class SyncScheduler @Inject constructor(private val workManager: WorkManager) {
    private val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
    fun enqueue() = workManager.enqueueUniqueWork(
        "maintenance-immediate-sync", ExistingWorkPolicy.REPLACE,
        OneTimeWorkRequestBuilder<SyncWorker>().setConstraints(constraints).setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS).build()
    )
    fun ensurePeriodic() = workManager.enqueueUniquePeriodicWork(
        "maintenance-periodic-sync", ExistingPeriodicWorkPolicy.KEEP,
        PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS).setConstraints(constraints).build()
    )
}

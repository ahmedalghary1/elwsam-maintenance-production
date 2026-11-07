package com.elwsam.operations

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import com.maintenance.supervisor.sync.SyncScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import com.elwsam.operations.updates.*

@HiltAndroidApp
class ElwsamApp : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var maintenanceSyncScheduler: SyncScheduler
    @Inject lateinit var updates: UpdateCoordinator

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        maintenanceSyncScheduler.ensurePeriodic()
        registerActivityLifecycleCallbacks(UpdateLifecycle(updates))
        UpdateSchedule.start(this)
        watchUpdateConnectivity(this)
    }
}

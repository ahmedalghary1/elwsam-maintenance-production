package com.elwsam.operations.updates

import android.app.Activity
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import androidx.hilt.work.HiltWorker
import androidx.work.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit
import javax.inject.Inject

object UpdateSchedule {
    private val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
    fun start(context: Context) {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("app-update-periodic", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<AppUpdateWorker>(15, TimeUnit.MINUTES).setConstraints(constraints).build())
        request(context)
    }
    fun request(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork("app-update-check", ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<AppUpdateWorker>().setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS).build())
    }
}

@HiltWorker
class AppUpdateWorker @AssistedInject constructor(
    @Assisted context: Context, @Assisted params: WorkerParameters,
    private val updates: UpdateCoordinator
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = if (updates.checkNow()) Result.success()
        else if (runAttemptCount < 4) Result.retry() else Result.failure()
}

class UpdateLifecycle(private val updates: UpdateCoordinator) : Application.ActivityLifecycleCallbacks {
    override fun onActivityResumed(activity: Activity) = updates.onResume(activity)
    override fun onActivityPaused(activity: Activity) = updates.onPause(activity)
    override fun onActivityCreated(activity: Activity, bundle: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}

fun watchUpdateConnectivity(context: Context) {
    val manager = context.getSystemService(ConnectivityManager::class.java)
    manager.registerNetworkCallback(NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(),
        object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) UpdateSchedule.request(context)
            }
        })
}

@AndroidEntryPoint
class InstallResultReceiver : BroadcastReceiver() {
    @Inject lateinit var updates: UpdateCoordinator
    override fun onReceive(context: Context, intent: Intent) {
        updates.installResult(intent)
    }
}

class UpdateReplacedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) UpdateSchedule.request(context)
    }
}

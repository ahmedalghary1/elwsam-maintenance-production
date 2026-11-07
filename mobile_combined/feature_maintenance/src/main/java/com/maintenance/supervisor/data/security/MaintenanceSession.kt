package com.maintenance.supervisor.data.security

import androidx.room.withTransaction
import com.maintenance.supervisor.data.local.AppDatabase
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/** Serializes account changes with maintenance requests and local writes. */
@Singleton
class MaintenanceSession @Inject constructor(
    private val db: AppDatabase,
    private val tokens: TokenStore
) {
    val mutex = Mutex()

    suspend fun activate(access: String, refresh: String, userId: Int) = mutex.withLock {
        clearCache()
        tokens.clear()
        tokens.bindUser(userId)
        tokens.save(access, refresh)
    }

    suspend fun clear() = mutex.withLock {
        tokens.clear()
        clearCache()
    }

    private suspend fun clearCache() = db.withTransaction {
        val dao = db.maintenanceDao()
        // Reports, answers and the upload queue survive logout under their owner.
        dao.clearUsers()
        dao.clearFactories()
        dao.clearCurrent()
        dao.clearAssets()
        dao.clearItems()
        dao.clearSections()
        dao.clearTemplates()
        dao.clearMetadata()
    }
}

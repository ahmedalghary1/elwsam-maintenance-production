package com.production.supervisor.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.production.supervisor.data.local.dao.ProductionDao
import com.production.supervisor.data.local.entity.*

@Database(
    entities = [
        AssetEntity::class,
        ProductEntity::class,
        OperatorEntity::class,
        ShiftReportEntity::class,
        MachineEntryEntity::class,
        StoppageEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class ProductionDatabase : RoomDatabase() {
    abstract fun productionDao(): ProductionDao
}

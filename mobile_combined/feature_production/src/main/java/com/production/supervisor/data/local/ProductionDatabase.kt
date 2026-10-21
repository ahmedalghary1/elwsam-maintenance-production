package com.production.supervisor.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.production.supervisor.data.local.dao.ProductionDao
import com.production.supervisor.data.local.entity.*

@Database(
    entities = [
        AssetEntity::class,
        ProductEntity::class,
        OperatorEntity::class,
        ProductionOptionEntity::class,
        ShiftReportEntity::class,
        MachineEntryEntity::class,
        StoppageEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class ProductionDatabase : RoomDatabase() {
    abstract fun productionDao(): ProductionDao

    companion object {
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE shift_reports ADD COLUMN ownerUserId INTEGER")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_shift_reports_ownerUserId_factoryId_reportDate_shift ON shift_reports(ownerUserId, factoryId, reportDate, shift)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE machine_entries ADD COLUMN dashboardDefaults TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE machine_entries ADD COLUMN originalCoolingTimeSeconds REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE machine_entries ADD COLUMN originalCycleTimeSeconds REAL NOT NULL DEFAULT 0.0")
                db.execSQL("UPDATE machine_entries SET originalCoolingTimeSeconds = coolingTimeSeconds, originalCycleTimeSeconds = cycleTimeSeconds")
                db.execSQL("ALTER TABLE shift_reports ADD COLUMN factoryId INTEGER")
                db.execSQL("ALTER TABLE shift_reports ADD COLUMN localRevision INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_shift_reports_factoryId_reportDate_shift ON shift_reports(factoryId, reportDate, shift)")
                db.execSQL("UPDATE shift_reports SET status = 'DRAFT' WHERE completedAtDevice IS NULL AND status = 'PENDING_HANDOVER'")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE assets ADD COLUMN defaultRawMaterialId INTEGER")
                db.execSQL("ALTER TABLE assets ADD COLUMN defaultRawMaterialName TEXT")
                db.execSQL("ALTER TABLE assets ADD COLUMN defaultFinalUnitId INTEGER")
                db.execSQL("ALTER TABLE assets ADD COLUMN defaultFinalUnitName TEXT")
                db.execSQL("ALTER TABLE assets ADD COLUMN targetCycleUnitId INTEGER")
                db.execSQL("ALTER TABLE assets ADD COLUMN targetCycleUnitName TEXT")
                db.execSQL("ALTER TABLE assets ADD COLUMN defaultPackagingId INTEGER")
                db.execSQL("ALTER TABLE assets ADD COLUMN defaultPackagingName TEXT")
                db.execSQL("CREATE TABLE IF NOT EXISTS production_options (id INTEGER NOT NULL, category TEXT NOT NULL, name TEXT NOT NULL, kgPerUnit REAL, PRIMARY KEY(id))")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_production_options_category_name ON production_options(category, name)")
                db.execSQL("ALTER TABLE machine_entries ADD COLUMN rawMaterialOptionId INTEGER")
                db.execSQL("ALTER TABLE machine_entries ADD COLUMN finalProductionQuantity REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE machine_entries ADD COLUMN finalProductionUnitId INTEGER")
                db.execSQL("ALTER TABLE machine_entries ADD COLUMN finalProductionUnitName TEXT NOT NULL DEFAULT 'كجم'")
                db.execSQL("ALTER TABLE machine_entries ADD COLUMN targetCycleUnitName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE machine_entries ADD COLUMN packagingOptionId INTEGER")
                db.execSQL("UPDATE machine_entries SET finalProductionQuantity = finalProductionWeightKg")
            }
        }
    }
}

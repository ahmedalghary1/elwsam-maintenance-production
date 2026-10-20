package com.maintenance.supervisor.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE reports ADD COLUMN isLocked INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `emergency_maintenances` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `clientReportId` TEXT NOT NULL,
                `assetId` INTEGER NOT NULL,
                `issueDescription` TEXT NOT NULL,
                `responsiblePerson` TEXT NOT NULL,
                `notes` TEXT NOT NULL,
                FOREIGN KEY(`clientReportId`) REFERENCES `reports`(`clientReportId`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_emergency_maintenances_clientReportId` ON `emergency_maintenances` (`clientReportId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_emergency_maintenances_assetId` ON `emergency_maintenances` (`assetId`)")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE reports ADD COLUMN cleanerName TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE reports ADD COLUMN mechanicalTechnician TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE reports ADD COLUMN electricalTechnician TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE reports ADD COLUMN maintenanceManager TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE users ADD COLUMN name TEXT NOT NULL DEFAULT ''")
    }
}

@Database(
    entities = [UserEntity::class, FactoryEntity::class, AssetEntity::class, ChecklistTemplateEntity::class,
        ChecklistSectionEntity::class, ChecklistItemEntity::class, CurrentMaintenanceEntity::class,
        MaintenanceReportEntity::class, MaintenanceAnswerEntity::class, SyncQueueEntity::class, MetadataEntity::class,
        EmergencyMaintenanceEntity::class],
    version = 5,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() { abstract fun maintenanceDao(): MaintenanceDao }


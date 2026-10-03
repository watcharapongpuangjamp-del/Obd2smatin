package com.example.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "vehicle_profiles")
data class VehicleProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val make: String,
    val model: String,
    val year: Int,
    val engineType: String,
    val licensePlate: String,
    val odometerKm: Int,
    val isDefault: Boolean = false
)

@Entity(tableName = "dtc_scan_records")
data class DtcScanRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val totalCodesFound: Int,
    val codesJson: String,
    val modeProvenance: String,
    val notes: String = ""
)

@Entity(tableName = "diagnostic_sessions")
data class DiagnosticSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val startTime: Long,
    val endTime: Long,
    val modeProvenance: String
)

@Entity(tableName = "telemetry_history")
data class TelemetryHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val timestamp: Long,
    val rpm: Int?,
    val speedKmh: Int?,
    val coolantTempC: Int?,
    val batteryVoltage: Float?
)

@Entity(tableName = "raw_communication_logs")
data class RawCommunicationLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val timestamp: Long,
    val direction: String,
    val rawHexOrText: String,
    val protocolId: String
)

@Entity(tableName = "maintenance_logs")
data class MaintenanceLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val titleTh: String,
    val dateTimestamp: Long,
    val costBaht: Double,
    val mileageKm: Int,
    val category: String,
    val notes: String = ""
)

@Entity(tableName = "service_intervals")
data class ServiceIntervalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val titleTh: String,
    val titleEn: String,
    val category: String,
    val targetIntervalKm: Int,
    val targetIntervalDays: Int,
    val lastServiceKm: Int,
    val lastServiceTimestamp: Long,
    val trackDtcClear: Boolean = false,
    val lastDtcClearKm: Int = 0,
    val lastDtcClearTimestamp: Long = 0,
    val isEnabled: Boolean = true,
    val notes: String = "",
    val isCustom: Boolean = false
)

@Entity(tableName = "dtc_clear_events")
data class DtcClearEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val odometerKm: Int,
    val clearedCodesJson: String = "[]",
    val clearedCount: Int = 0,
    val modeProvenance: String,
    val notes: String = ""
)

@Dao
interface VehicleProfileDao {
    @Query("SELECT * FROM vehicle_profiles ORDER BY isDefault DESC, id DESC")
    fun getAllProfiles(): Flow<List<VehicleProfileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: VehicleProfileEntity): Long

    @Query("DELETE FROM vehicle_profiles WHERE id = :id")
    suspend fun deleteProfile(id: Long)

    @Query("UPDATE vehicle_profiles SET odometerKm = :newOdoKm WHERE id = :vehicleId")
    suspend fun updateOdometer(vehicleId: Long, newOdoKm: Int)
}

@Dao
interface DtcScanDao {
    @Query("SELECT * FROM dtc_scan_records ORDER BY timestamp DESC")
    fun getAllScans(): Flow<List<DtcScanRecordEntity>>

    @Query("SELECT * FROM dtc_scan_records WHERE vehicleId = :vehicleId ORDER BY timestamp DESC")
    fun getScansForVehicle(vehicleId: Long): Flow<List<DtcScanRecordEntity>>

    @Query("SELECT * FROM dtc_scan_records WHERE id = :id")
    suspend fun getScanById(id: Long): DtcScanRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScanRecord(record: DtcScanRecordEntity): Long

    @Query("DELETE FROM dtc_scan_records WHERE id = :id")
    suspend fun deleteScanRecord(id: Long)

    @Query("DELETE FROM dtc_scan_records WHERE vehicleId = :vehicleId")
    suspend fun deleteScansForVehicle(vehicleId: Long)

    @Query("DELETE FROM dtc_scan_records")
    suspend fun clearAllScans()
}

@Dao
interface DiagnosticSessionDao {
    @Insert
    suspend fun insertSession(session: DiagnosticSessionEntity): Long
    
    @Insert
    suspend fun insertTelemetry(telemetry: List<TelemetryHistoryEntity>)
    
    @Insert
    suspend fun insertLogs(logs: List<RawCommunicationLogEntity>)
}

@Dao
interface MaintenanceLogDao {
    @Query("SELECT * FROM maintenance_logs WHERE vehicleId = :vehicleId ORDER BY dateTimestamp DESC")
    fun getLogsForVehicle(vehicleId: Long): Flow<List<MaintenanceLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: MaintenanceLogEntity): Long
}

@Dao
interface ServiceIntervalDao {
    @Query("SELECT * FROM service_intervals WHERE vehicleId = :vehicleId ORDER BY id ASC")
    fun getIntervalsForVehicle(vehicleId: Long): Flow<List<ServiceIntervalEntity>>

    @Query("SELECT * FROM service_intervals ORDER BY vehicleId ASC, id ASC")
    fun getAllIntervals(): Flow<List<ServiceIntervalEntity>>

    @Query("SELECT * FROM service_intervals WHERE id = :id")
    suspend fun getIntervalById(id: Long): ServiceIntervalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInterval(interval: ServiceIntervalEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIntervals(intervals: List<ServiceIntervalEntity>)

    @Query("UPDATE service_intervals SET lastServiceKm = :newKm, lastServiceTimestamp = :timestamp WHERE id = :id")
    suspend fun recordServiceCompleted(id: Long, newKm: Int, timestamp: Long)

    @Query("UPDATE service_intervals SET targetIntervalKm = :targetKm, targetIntervalDays = :targetDays WHERE id = :id")
    suspend fun updateTargetInterval(id: Long, targetKm: Int, targetDays: Int)

    @Query("UPDATE service_intervals SET lastDtcClearKm = :dtcClearKm, lastDtcClearTimestamp = :timestamp WHERE vehicleId = :vehicleId")
    suspend fun updateDtcClearMetadata(vehicleId: Long, dtcClearKm: Int, timestamp: Long)

    @Query("DELETE FROM service_intervals WHERE id = :id")
    suspend fun deleteInterval(id: Long)

    @Query("DELETE FROM service_intervals WHERE vehicleId = :vehicleId")
    suspend fun deleteIntervalsForVehicle(vehicleId: Long)
}

@Dao
interface DtcClearEventDao {
    @Query("SELECT * FROM dtc_clear_events WHERE vehicleId = :vehicleId ORDER BY timestamp DESC")
    fun getEventsForVehicle(vehicleId: Long): Flow<List<DtcClearEventEntity>>

    @Query("SELECT * FROM dtc_clear_events WHERE vehicleId = :vehicleId ORDER BY timestamp DESC LIMIT 1")
    fun getLatestEventForVehicle(vehicleId: Long): Flow<DtcClearEventEntity?>

    @Query("SELECT * FROM dtc_clear_events ORDER BY timestamp DESC LIMIT 1")
    fun getLatestEvent(): Flow<DtcClearEventEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: DtcClearEventEntity): Long

    @Query("DELETE FROM dtc_clear_events WHERE vehicleId = :vehicleId")
    suspend fun deleteEventsForVehicle(vehicleId: Long)
}

@Database(
    entities = [
        VehicleProfileEntity::class,
        DtcScanRecordEntity::class,
        DiagnosticSessionEntity::class,
        TelemetryHistoryEntity::class,
        RawCommunicationLogEntity::class,
        MaintenanceLogEntity::class,
        ServiceIntervalEntity::class,
        DtcClearEventEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class ObdDatabase : RoomDatabase() {
    abstract fun vehicleProfileDao(): VehicleProfileDao
    abstract fun dtcScanDao(): DtcScanDao
    abstract fun diagnosticSessionDao(): DiagnosticSessionDao
    abstract fun maintenanceLogDao(): MaintenanceLogDao
    abstract fun serviceIntervalDao(): ServiceIntervalDao
    abstract fun dtcClearEventDao(): DtcClearEventDao

    companion object {
        @Volatile
        private var INSTANCE: ObdDatabase? = null

        fun getDatabase(context: Context): ObdDatabase {
            val MIGRATION_1_2 = object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE IF NOT EXISTS `diagnostic_sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `vehicleId` INTEGER NOT NULL, `startTime` INTEGER NOT NULL, `endTime` INTEGER NOT NULL, `modeProvenance` TEXT NOT NULL)")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `telemetry_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sessionId` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, `rpm` INTEGER, `speedKmh` INTEGER, `coolantTempC` INTEGER, `batteryVoltage` REAL)")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `raw_communication_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sessionId` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, `direction` TEXT NOT NULL, `rawHexOrText` TEXT NOT NULL, `protocolId` TEXT NOT NULL)")
                }
            }

            val MIGRATION_2_3 = object : Migration(2, 3) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `service_intervals` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`vehicleId` INTEGER NOT NULL, " +
                            "`titleTh` TEXT NOT NULL, " +
                            "`titleEn` TEXT NOT NULL, " +
                            "`category` TEXT NOT NULL, " +
                            "`targetIntervalKm` INTEGER NOT NULL, " +
                            "`targetIntervalDays` INTEGER NOT NULL, " +
                            "`lastServiceKm` INTEGER NOT NULL, " +
                            "`lastServiceTimestamp` INTEGER NOT NULL, " +
                            "`trackDtcClear` INTEGER NOT NULL, " +
                            "`lastDtcClearKm` INTEGER NOT NULL, " +
                            "`lastDtcClearTimestamp` INTEGER NOT NULL, " +
                            "`isEnabled` INTEGER NOT NULL, " +
                            "`notes` TEXT NOT NULL, " +
                            "`isCustom` INTEGER NOT NULL)"
                    )
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `dtc_clear_events` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`vehicleId` INTEGER NOT NULL, " +
                            "`timestamp` INTEGER NOT NULL, " +
                            "`odometerKm` INTEGER NOT NULL, " +
                            "`clearedCodesJson` TEXT NOT NULL, " +
                            "`clearedCount` INTEGER NOT NULL, " +
                            "`modeProvenance` TEXT NOT NULL, " +
                            "`notes` TEXT NOT NULL)"
                    )
                }
            }

            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ObdDatabase::class.java,
                    "thai_car_obd_db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .fallbackToDestructiveMigration(true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

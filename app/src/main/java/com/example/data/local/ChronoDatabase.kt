package com.example.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "watch_presets")
data class WatchPresetEntity(
  @PrimaryKey val id: String,
  val name: String,
  val summary: String,
  val configJson: String,
  val isBuiltIn: Boolean = false,
  val updatedAtEpochMs: Long = System.currentTimeMillis(),
)

@Entity(tableName = "sync_logs")
data class SyncLogEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val targetNodeName: String,
  val statusLabel: String,
  val payloadBytes: Int,
  val includedVideo: Boolean,
  val detailMessage: String,
  val timestampMs: Long = System.currentTimeMillis(),
)

@Dao
interface ChronoDao {
  @Query("SELECT * FROM watch_presets ORDER BY isBuiltIn DESC, updatedAtEpochMs DESC")
  fun observePresets(): Flow<List<WatchPresetEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertPreset(preset: WatchPresetEntity)

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertPresetsIfMissing(presets: List<WatchPresetEntity>)

  @Query("DELETE FROM watch_presets WHERE id = :id AND isBuiltIn = 0")
  suspend fun deleteCustomPreset(id: String)

  @Query("SELECT * FROM sync_logs ORDER BY timestampMs DESC LIMIT 20")
  fun observeSyncLogs(): Flow<List<SyncLogEntity>>

  @Insert
  suspend fun insertSyncLog(log: SyncLogEntity)
}

@Database(
  entities = [WatchPresetEntity::class, SyncLogEntity::class],
  version = 1,
  exportSchema = false,
)
abstract class ChronoDatabase : RoomDatabase() {
  abstract fun chronoDao(): ChronoDao
}

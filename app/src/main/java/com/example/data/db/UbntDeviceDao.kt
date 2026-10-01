package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UbntDeviceDao {

    @Query("SELECT * FROM ubnt_devices ORDER BY lastSeenTimestamp DESC")
    fun getAllDevicesFlow(): Flow<List<UbntDeviceEntity>>

    @Query("SELECT * FROM ubnt_devices WHERE mac = :mac LIMIT 1")
    suspend fun getDeviceByMac(mac: String): UbntDeviceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(device: UbntDeviceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(devices: List<UbntDeviceEntity>)

    @Query("UPDATE ubnt_devices SET isFavorite = :isFavorite WHERE mac = :mac")
    suspend fun setFavorite(mac: String, isFavorite: Boolean)

    @Query("UPDATE ubnt_devices SET customNote = :note WHERE mac = :mac")
    suspend fun updateNote(mac: String, note: String)

    @Query("DELETE FROM ubnt_devices WHERE mac = :mac")
    suspend fun deleteDevice(mac: String)

    @Query("DELETE FROM ubnt_devices")
    suspend fun clearAll()
}

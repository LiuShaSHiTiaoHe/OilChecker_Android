package com.example.oilchecker.data.Dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.oilchecker.data.entity.Fuel

@Dao
interface FuelDataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFuelData(data: List<Fuel>)
    @Query("SELECT * FROM Fuel WHERE device_id = :id")
    fun getFuelData(id: String): List<Fuel>
    @Query("SELECT * FROM FUEL WHERE record_time_interval > :start AND record_time_interval < :end AND device_id = :id")
    fun getFuelDataInTimeRange(start: Long, end: Long, id: String): List<Fuel>
}
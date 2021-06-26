package com.example.oilchecker.data.entity

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FuelConsumeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFuelConsumeData(data: List<FuelConsume>)
    @Query("SELECT * FROM FuelConsume WHERE device_id = :id")
    fun getFuelConsumeData(id: String): List<FuelConsume>
}
package com.example.oilchecker.data.entity

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FuelDataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFuelData(data: List<Fuel>)
    @Query("SELECT * FROM Fuel WHERE device_id = :id")
    fun getFuelData(id: String): List<Fuel>

}
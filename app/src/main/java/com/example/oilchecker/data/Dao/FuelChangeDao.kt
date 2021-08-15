package com.example.oilchecker.data.Dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.oilchecker.data.entity.FuelChange

@Dao
interface FuelChangeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFuelChangedData(data: List<FuelChange>)
    @Query("SELECT * FROM FuelChange WHERE device_id = :id")
    fun getFuelChangedData(id: String): List<FuelChange>
}
package com.example.oilchecker.data.entity

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RefuelDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRefuelData(data: List<Refuel>)
    @Query("SELECT * FROM Refuel WHERE device_id = :id")
    fun getRefuelData(id: String): List<Refuel>
}
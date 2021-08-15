package com.example.oilchecker.data.Dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.oilchecker.data.entity.Refuel

@Dao
interface RefuelDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRefuelData(data: List<Refuel>)
    @Query("SELECT * FROM Refuel WHERE device_id = :id")
    fun getRefuelData(id: String): List<Refuel>
}
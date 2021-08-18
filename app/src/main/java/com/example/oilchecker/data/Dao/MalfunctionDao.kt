package com.example.oilchecker.data.Dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.oilchecker.data.entity.MalfunctionModel


@Dao
interface MalfunctionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMalfunctionData(data: List<MalfunctionModel>)
    @Query("SELECT * FROM MalfunctionModel WHERE device_id = :id")
    fun getMalfunctionData(id: String): List<MalfunctionModel>
}
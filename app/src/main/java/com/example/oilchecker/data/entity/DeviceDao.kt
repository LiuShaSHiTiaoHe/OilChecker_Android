package com.example.oilchecker.data.entity

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DeviceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg device: Device)

    /*  @Query("SELECT id FROM items WHERE device_id = :deviceid")
    fun getItemIdentify(deviceid: String): String*/
    @Query("SELECT * FROM Device WHERE device_id = :id")
    fun getDevice(id: String): Device?

    @Query("SELECT * FROM Device")
    fun getAllDevice(): List<Device>
    @Query("UPDATE Device SET status = :status, average = :average WHERE device_id = :id")
    fun insertStatusAndAverage(id: String, status: String, average: String)
    @Query("UPDATE Device SET length = :length, width = :width, height = :height, compare = :compare WHERE device_id = :id")
    fun insertDeviceSize(id: String, length: String, width: String, height: String, compare: String)
}
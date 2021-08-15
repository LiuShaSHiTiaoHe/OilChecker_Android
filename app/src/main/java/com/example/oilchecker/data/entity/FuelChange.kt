package com.example.oilchecker.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class FuelChange(
    @PrimaryKey
    @ColumnInfo(name = "id") var id: Int?,
    @ColumnInfo(name = "type") var type: String?,
    @ColumnInfo(name = "device_id") var deviceId: String?,
    @ColumnInfo(name = "fuelData") var fuelData: Double?,
    @ColumnInfo(name = "record_time") var recordTime: String?,
    @ColumnInfo(name = "record_time_interval") var recordTimeInterval: Long?
)

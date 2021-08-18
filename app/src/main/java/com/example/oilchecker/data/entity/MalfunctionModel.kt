package com.example.oilchecker.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity
data class MalfunctionModel(
    @PrimaryKey
    @ColumnInfo(name = "id") var id: Int?,
    @ColumnInfo(name = "device_id") var deviceId: String?,
    @ColumnInfo(name = "errorCode") var errorCode: String?,
    @ColumnInfo(name = "errorDes") var errorDes: String?,
    @ColumnInfo(name = "record_time") var recordTime: String?,
    @ColumnInfo(name = "record_time_interval") var recordTimeInterval: Long?
)

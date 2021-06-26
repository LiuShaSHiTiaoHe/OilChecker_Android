package com.example.oilchecker.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Device(@PrimaryKey
                  @ColumnInfo(name = "id") var id: Int?,
                  @ColumnInfo(name = "device_id") var deviceId: String?,
                  @ColumnInfo(name = "car_num") var num: String,

                  @ColumnInfo(name = "length") var length: String,
                  @ColumnInfo(name = "width") var width: String,
                  @ColumnInfo(name = "height") var height: String,
                  @ColumnInfo(name = "compare") var compare: String,
                  @ColumnInfo(name = "status") var status: String,
                  @ColumnInfo(name = "average") var average: String,
                  @ColumnInfo(name = "mac") var mac: String)
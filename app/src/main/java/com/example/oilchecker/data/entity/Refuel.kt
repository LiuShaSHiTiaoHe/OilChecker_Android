package com.example.oilchecker.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Refuel(@PrimaryKey
                  @ColumnInfo(name = "id") var id: Int?,
                  @ColumnInfo(name = "device_id") var deviceId: String?,
                  @ColumnInfo(name = "capacity") var capacity: String?)

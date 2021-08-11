package com.example.oilchecker.util

import java.util.*

class Contants {
    companion object{
        val SERVICE_UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
        val NOTIFY_UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")
        val WRITE_UUID = UUID.fromString("0000ffe2-0000-1000-8000-00805f9b34fb")

        val test = UUID.fromString("00001531-0000-3512-2118-0009af100700")

    }

}

enum class BleSendDataState{
    SendRequestDeviceInfo,
    SendSyncDeviceTime,
    SendRequstFuelData,
}
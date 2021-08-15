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

class ToastTips{
    companion object{
        val B_Connected = "ble_connected"
        val B_Disconnect = "ble_disconnect"
        val B_ConnectFailed = "ble_connect_failed"
        val B_SendDataFailed = "ble_send_data_failed"


        val R_DeviceInfo = "request_device_info"
        val R_SyncTime = "sync_device_time"

        val R_FuelData = "request_fuel_data"
        val R_ReceivedFuelData = "received_fuel_data"
        val S_ProcessFuelDataComplete = "process_fuel_data_done"

    }
}

enum class FuelChangedType(val type: String){
    REFUEL("refuel"),
    CONSUMPTION("consumption")
}

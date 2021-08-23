package com.example.oilchecker.fragment

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.oilchecker.ScanResultsAdapter
import com.example.oilchecker.base.MainApplication/*
import com.polidea.rxandroidble.exceptions.BleScanException
import com.polidea.rxandroidble.scan.ScanFilter
import com.polidea.rxandroidble.scan.ScanResult
import com.polidea.rxandroidble.scan.ScanSettings*/
import com.example.oilchecker.data.AppDatabase
import com.example.oilchecker.data.entity.Device
import com.example.oilchecker.data.entity.MalfunctionModel
import com.example.oilchecker.util.Contants
import com.example.oilchecker.util.UserPreference
import com.example.oilchecker.util.toDateLong
import com.polidea.rxandroidble2.NotificationSetupMode
import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.RxBleDevice
import com.polidea.rxandroidble2.exceptions.BleScanException
import com.polidea.rxandroidble2.samplekotlin.util.isConnected
import com.polidea.rxandroidble2.samplekotlin.util.toHex
import com.polidea.rxandroidble2.samplekotlin.util.toHexString
import com.polidea.rxandroidble2.scan.ScanFilter
import com.polidea.rxandroidble2.scan.ScanResult
import com.polidea.rxandroidble2.scan.ScanSettings
import dagger.hilt.android.internal.managers.FragmentComponentManager
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.lang.StringBuilder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class BleDeviceViewModel @Inject constructor(
    private val database: AppDatabase
): ViewModel() {
    // TODO: Implement the ViewModel
    private val TAG = "BleDeviceViewModel"
    var tipLiveData = MutableLiveData<String>()
    var deviceLiveData = MutableLiveData<String>()
    private lateinit var bleDevice: RxBleDevice
    private lateinit var connectionObservable: Observable<RxBleConnection>
    private val disconnectTriggerSubject = PublishSubject.create<Unit>()
    private val connectionDisposable = CompositeDisposable()
    private lateinit var mConnection: RxBleConnection

    fun recordMalfuntion(description: String){
        viewModelScope.launch {
            async(Dispatchers.IO) {
                val identify = UserPreference.getIdentify()
                val today = LocalDateTime.now()
                val myDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                var timestring = myDateTimeFormatter.format(today)
                val timeInterval = timestring.toDateLong()
                database.malfunctionDao().insertMalfunctionData(MalfunctionModel(0,identify,"123",description,timestring, timeInterval))
            }
        }
    }

    suspend fun addNewDevice(device: Device){
        Log.i(TAG, "addNewDevice: ")
        viewModelScope.launch {
            async(Dispatchers.IO) {
                database.deviceDao().deleteDevice(device.mac.toString())
                database.deviceDao().insert(device)
                UserPreference.setDevice(device.num.toString())
                UserPreference.setMac(device.mac.toString())
                UserPreference.setIdentify(device.deviceId.toString())
            }
        }
    }

    fun getDeviceById(id: String){
        viewModelScope.launch {
            async(Dispatchers.IO) {
                val device = database.deviceDao().getDevice(id)
                if (device != null){
                    deviceLiveData.postValue(device.num)
                }
            }
        }
    }

    fun doConnect(macAddress: String){
        bleDevice = MainApplication.rxBleClient.getBleDevice(macAddress!!)
        if (bleDevice.isConnected) {
            triggerDisconnect()
        } else {
            connectionObservable = bleDevice.establishConnection(false)
            connectionObservable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    mConnection = it
                    onConnectionReceived(it)
                },{
                    onConnectionFailure(it)
                })
                .let { connectionDisposable.add(it) }
        }
    }

    private fun onConnectionFailure(throwable: Throwable) {
        Log.i(TAG, "onConnectionFailure: $throwable")
        tipLiveData.postValue(throwable.message)
    }

    private fun onConnectionReceived(connection: RxBleConnection) {
        Log.i(TAG, "onConnectionReceived: --->")
        if(bleDevice.isConnected) {
            Log.i(TAG, "doConnect: isConnected-->")
            mConnection.setupNotification(Contants.NOTIFY_UUID, NotificationSetupMode.QUICK_SETUP)
                .doOnNext{
                    getDeviceInfo()
                    Log.i(TAG, "notification has been set up: ")}
                .flatMap { it }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ onNotificationReceived(it) }, { onNotificationSetupFailure(it) })
                .let { connectionDisposable.add(it) }
        }else {
            Log.i(TAG, "doConnect: disConnected")
        }
    }

    private fun getDeviceInfo() {
        var dataLen = 1.toString(16)   //数据长度 1字节
        val len = (256 - 1).toString(16) //数据长补数 1字节
        Log.i(TAG, "onClick: $len")
        //5+2
        // device identify --> 0038-->0000
        val data = "01ff0000018511"
        var sum = 0
        for (i in 0 until data.length/2){
            sum = sum xor data.substring(i*2,i*2+2).toInt(16)
        }
        if (dataLen.length == 1){
            dataLen = "0$dataLen"
        }
        var sumLen = sum.toString(16)
        if (sumLen.length == 1){
            sumLen = "0$sumLen"
        }
        var result = StringBuilder()
        result.append("02")
        result.append(dataLen)
        result.append(len)
        result.append("00")
        result.append("00")
        result.append("01")
        result.append("85")
        result.append("11")
        result.append(sumLen)
        result.append("03")
        val write = result.toString()
        Log.i(TAG, "onClick: $write")
        val inputBytes: ByteArray = write.toByteArray()
        Log.i(TAG, "getDeviceInfo: connect state -> ${bleDevice.isConnected}")
        mConnection.writeCharacteristic(Contants.WRITE_UUID,inputBytes)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ onWriteSuccess() }, { onWriteFailure(it) })
            .let { connectionDisposable.add(it) }

    }


    private fun onWriteSuccess(){
        Log.i(TAG, "onWriteSuccess: ")
    }

    private fun onWriteFailure(throwable: Throwable){
        Log.i(TAG, "onWriteFailure: ${throwable.message}")

    }

    private fun onNotificationReceived(bytes: ByteArray) {
        Log.i(TAG, "onNotificationReceived:  ${bytes.toHexString()}")
    }

    private fun onNotificationSetupFailure(throwable: Throwable){
        Log.i(TAG, "onNotificationSetupFailure: $throwable")
        tipLiveData.postValue(throwable.message)
    }
    private fun triggerDisconnect(){
        // connectionDisposable?.dispose()
        disconnectTriggerSubject.onNext(Unit)
    }


}


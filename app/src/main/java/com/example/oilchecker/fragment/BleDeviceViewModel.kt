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
import com.example.oilchecker.util.Contants
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
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class BleDeviceViewModel @Inject constructor(
    private val database: AppDatabase
): ViewModel() {
    // TODO: Implement the ViewModel
    private val TAG = "BleDeviceViewModel"
    private val rxBleClient = MainApplication.rxBleClient
    private var scanDisposable: Disposable ?= null

    private var hasClickedScan = false
    private var isScanning: Boolean= scanDisposable != null
//    private val resultsAdapter = ScanResultsAdapter {
//        Log.i(TAG, "scanResult: ${it.bleDevice.macAddress} ")
//    }
    var tipLiveData = MutableLiveData<String>()
    var deviceLiveData = MutableLiveData<String>()


    private lateinit var bleDevice: RxBleDevice

    private lateinit var connectionObservable: Observable<RxBleConnection>
    private val disconnectTriggerSubject = PublishSubject.create<Unit>()

    private val connectionDisposable = CompositeDisposable()
    private lateinit var mConnection: RxBleConnection


    private fun scan(){
        if(isScanning){
            scanDisposable?.dispose()
        }else {
            if (rxBleClient.isScanRuntimePermissionGranted){
                scanBleDevices()
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally {  }
                    .subscribe()
                    .let { scanDisposable = it }
            }else {
                hasClickedScan = true

            }
        }
    }

    private fun scanBleDevices(): Observable<ScanResult>{
        return rxBleClient.scanBleDevices(
            ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .build(),
            ScanFilter.Builder()
                .build()
        )
    }


    private fun displayResult(result: ScanResult){
        val name = result.bleDevice.name
        val na = result.bleDevice.bluetoothDevice.name
        val mac = result.bleDevice.macAddress

        Log.e(TAG, "--name---$name----mac---$mac")
    }

    private fun onScanFailure(throwable: Throwable) {
        Log.e(TAG, "scan fail")
        scanDisposable?.dispose()
        if (throwable is BleScanException) {
            handleBleScanException(throwable)
        }
    }

    private fun handleBleScanException(bleScanException: BleScanException) {
        val text: String
        text = when (bleScanException.reason) {
            BleScanException.BLUETOOTH_NOT_AVAILABLE -> "Bluetooth is not available"
            BleScanException.BLUETOOTH_DISABLED -> "打开蓝牙，再试一次"
            BleScanException.LOCATION_PERMISSION_MISSING ->                 //text = "On Android 6.0 location permission is required. Implement Runtime Permissions";
                "定位权限未打开"
            BleScanException.LOCATION_SERVICES_DISABLED ->                 //text = "Location services needs to be enabled on Android 6.0";
                "请打开定位服务"
            BleScanException.SCAN_FAILED_ALREADY_STARTED -> "Scan with the same filters is already started"
            BleScanException.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "Failed to register application for bluetooth scan"
            BleScanException.SCAN_FAILED_FEATURE_UNSUPPORTED -> "Scan with specified parameters is not supported"
            BleScanException.SCAN_FAILED_INTERNAL_ERROR -> "Scan failed due to internal error"
            BleScanException.SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES -> "Scan cannot start due to limited hardware resources"
            BleScanException.UNDOCUMENTED_SCAN_THROTTLE -> String.format(
                Locale.getDefault(),
                "Android 7+ does not allow more scans. Try in %d seconds",
                bleScanException.retryDateSuggestion?.let { secondsTill(it) }
            )
            BleScanException.UNKNOWN_ERROR_CODE, BleScanException.BLUETOOTH_CANNOT_START -> "Unable to start scanning"
            else -> "Unable to start scanning"
        }
        Log.e("EXCEPTION", text, bleScanException)
        Log.e(TAG, "throwable======>$text");
        tipLiveData.postValue(text)
        //Toast.makeText(mContext, text, Toast.LENGTH_SHORT).show()
    }

    private fun secondsTill(retryDateSuggestion: Date): Long {
        return TimeUnit.MILLISECONDS.toSeconds(retryDateSuggestion.time - System.currentTimeMillis())
    }

    private fun isScanning(): Boolean {
        return scanDisposable != null
    }

    private fun clearSubscription() {
        scanDisposable = null
    }

    suspend fun addNewDevice(device: Device){
        Log.i(TAG, "addNewDevice: ")
        viewModelScope.launch {
            async(Dispatchers.IO) {
                database.deviceDao().insert(device)
                HomeViewModel.setDevice(device.num.toString())
                HomeViewModel.setMac(device.mac.toString())
                HomeViewModel.setIdentify(device.deviceId.toString())
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
                //.flatMapSingle { it.discoverServices() }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    mConnection = it
                    //  test()
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
       // Toast.makeText(context, throwable.message, Toast.LENGTH_SHORT).show()

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

            /* connectionObservable
                 //  .doOnNext{it.setupNotification(Contants.NOTIFY_UUID)}
                 .doOnNext { it.writeCharacteristic(Contants.test, inputBytes) }
                 .flatMap { it.setupNotification(Contants.test) }
                 .flatMap { it }
                 .observeOn(AndroidSchedulers.mainThread())
                 .subscribe({ onNotificationReceived(it) }, { onNotificationSetupFailure(it) })
                 .let { connectionDisposable.add(it) }*/
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
            //Log.i(TAG, "onClick: sum $sum data "+data.substring(i*2,i*2+2)+" -->${data.substring(i*2,i*2+2).toInt(16)}")

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
        //mConnection.writeCharacteristic(Contants.WRITE_UUID,inputBytes)
        Log.i(TAG, "getDeviceInfo: connect state -> ${bleDevice.isConnected}")

        mConnection.writeCharacteristic(Contants.WRITE_UUID,inputBytes)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ onWriteSuccess() }, { onWriteFailure(it) })
            .let { connectionDisposable.add(it) }

    }


    private fun onWriteSuccess(){
        Log.i(TAG, "onWriteSuccess: ")
        /*mConnection.readCharacteristic(Contants.NOTIFY_UUID)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                Log.i(TAG, "read success :  ${it.toHex()}")

            }, {
                Log.i(TAG, "read Failure: ${it.message}")
            })
            .let { connectionDisposable.add(it) }*/
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

    private fun getFuelInfo() {

        // 02 len 256-len id id>>8 01 cmd data bcc 03
        //data 85 11
        // 02 01 256-1 id id>>8 01 85 11 bcc 03
        //256 128 64 32 16 8
        //val num: Int = res.substring(8, 10)
        //id 0038
        //bcc 02^ fe ^00
        //02 02 fe 00 38 01 85 11


        var dataLen = 1.toString(16)
        val len = (256 - 1).toString(16)
        Log.i(TAG, "onClick: $len")
        //5+2
        // device identify --> 0038
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
        result.append(dataLen)  //数据长度 1字节
        result.append(len)  //数据长补数 1字节
        result.append("00")
        result.append("00")
        result.append("01")
        result.append("81")
        result.append("00")
        result.append(sumLen)
        result.append("03")


        val write = result.toString()
        Log.i(TAG, "onClick: $write")

        val inputBytes: ByteArray = write.toByteArray()
        mConnection.writeCharacteristic(Contants.WRITE_UUID,inputBytes)
    }

    private fun triggerDisconnect(){
        // connectionDisposable?.dispose()
        disconnectTriggerSubject.onNext(Unit)
    }


}


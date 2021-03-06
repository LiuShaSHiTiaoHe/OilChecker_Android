package com.example.oilchecker.fragment

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.text.Editable
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.*
import com.example.oilchecker.R
import com.example.oilchecker.base.MainApplication
import com.example.oilchecker.data.AppDatabase
import com.example.oilchecker.data.entity.Device
import com.example.oilchecker.data.entity.Fuel
import com.example.oilchecker.data.entity.FuelConsume
import com.example.oilchecker.data.entity.Refuel
import com.example.oilchecker.di.DatabaseModel
import com.example.oilchecker.util.Contants
import com.example.oilchecker.util.SpUtils
import com.polidea.rxandroidble2.NotificationSetupMode
import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.RxBleDevice
import com.polidea.rxandroidble2.samplekotlin.util.hex2byte
import com.polidea.rxandroidble2.samplekotlin.util.isConnected
import com.polidea.rxandroidble2.samplekotlin.util.toHex
import com.polidea.rxandroidble2.samplekotlin.util.toHexString
import dagger.hilt.android.internal.managers.FragmentComponentManager
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.*
import java.lang.StringBuilder
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList
import kotlin.concurrent.schedule

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val database: AppDatabase

) : ViewModel() {
    private val TAG = "FuelData"
    private val connectionDisposable = CompositeDisposable()
    private val disconnectTriggerSubject = PublishSubject.create<Unit>()
    private var stateDisposable: Disposable? = null
    private lateinit var mConnection: RxBleConnection
    private lateinit var bleDevice: RxBleDevice
    private var isRequestFuelData = false
    private var receiveFuelData: String = ""

    var fuelLiveData = MutableLiveData<ArrayList<String>>()
    var averageFuelConsumeLiveData = MutableLiveData<String>()
    var fuelStatusLiveData = MutableLiveData<String>()
    var tipLiveData = MutableLiveData<String>()
    var status: Boolean = false


    // TODO: Implement the ViewModel
    companion object {

        var currentDevice: String = ""
        fun getDevice(): String? {
            val current = SpUtils.getString("current")
            return current
        }
        fun setDevice(num: String) {
            SpUtils.put("current",num)
        }

        fun getMac(): String? {
            return SpUtils.getString("mac")
        }
        fun setMac(mac: String) {
            SpUtils.put("mac",mac)
        }
        fun getIdentify(): String? {
            return SpUtils.getString("identify")
        }
        fun setIdentify(identify: String) {
            SpUtils.put("identify",identify)
        }
        fun getAverageOil(): String? {
            return SpUtils.getString("average")
        }
        fun setAverageOil(average: String) {
            SpUtils.put("average",average)
        }
        fun getStatus(): String? {
            return SpUtils.getString("status")
        }

        fun setThresholdValue(value: Double){
            SpUtils.put("threshold", value)
        }

        fun getThreshold():Double{
            val value = SpUtils.getDouble("threshold")
            if (value == 0.0){
                return  5.00
            }else{
                return  value!!
            }
        }

        //??????segment index
        fun setSegmentIndex(value: Int){
            SpUtils.put("segmentIndex",value)
        }
        fun getSegmentIndex(): Int{
            val value = SpUtils.getInt("segmentIndex")
            if (value == null){
                return  0
            }else{
                return  value
            }
        }

    }

    fun doConnect(mac: String){
        bleDevice = MainApplication.rxBleClient.getBleDevice(mac!!)
        if (bleDevice.isConnected) {
            return
        }else
        {
            triggerDisconnect()
            Timer().schedule(1000){
                setupConnection()
            }
        }

    }

    private fun setupConnection(){
        bleDevice.establishConnection(false)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                onConnectionReceived(it)
            },{
                onConnectionFailure(it)
            })
            .let { connectionDisposable.add(it) }

        bleDevice.observeConnectionStateChanges()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { onConnectionStateChange(it) }
            .let { stateDisposable = it }
    }

    private fun onConnectionStateChange(newState: RxBleConnection.RxBleConnectionState) {
        val text = newState.name.toString()
        Log.i(TAG, "onConnectionStateChange: $text")
        if (text == "DISCONNECTED"){
            tipLiveData.postValue("disconnect")
        }
    }

    private fun onConnectionFailure(throwable: Throwable) {
        Log.i(TAG, "onConnectionFailure: $throwable")
        tipLiveData.postValue("connectionfail")
    }

    private fun onConnectionReceived(connection: RxBleConnection) {
        mConnection = connection
        if(bleDevice.isConnected) {
            Log.i(TAG, "doConnect: isConnected-->")
            mConnection.setupNotification(Contants.NOTIFY_UUID, NotificationSetupMode.QUICK_SETUP)
                .doOnNext{
                    getDeviceInfo()
                   // getFuelInfo()
                    Log.i(TAG, "notification has been set up: ")}
                .flatMap { it }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ onNotificationReceived(it) }, { onNotificationSetupFailure(it) })
                .let { connectionDisposable.add(it) }
        }else {
            Log.i(TAG, "doConnect: disConnected")
        }
    }

    private fun onNotificationReceived(bytes: ByteArray) {
        Log.i(TAG, "---onNext--->>>>$bytes --->${bytes.toHex()}")
        if (!status) {
            status = true
            val result = bytes.toHex()
            if (result.length > 14) {
                if (result.substring(12, 14) == "86") {
                    val identify = result.substring(14, 18)
                    val length = result.substring(18, 22).toInt(16).toString()
                    val width = result.substring(22, 26).toInt(16).toString()
                    val height = result.substring(26, 30).toInt(16).toString()
                    val compare = result.substring(30, 34).toInt(16).toString()
                    tipLiveData.postValue("request")
                    viewModelScope.launch {
                        async(Dispatchers.IO) {
                            database.deviceDao().insertDeviceSize(identify, length, width, height,compare)
                        }
                    }
                    Timer().schedule(1000){
                        getFuelInfo()
                    }
                }
            }

        }else {
            if (isRequestFuelData) {
                if (receiveFuelData.isEmpty()) {
                    val result = bytes.toHex()
                    if (result.length < 14){
                        return
                    }
                }
                receiveFuelData += bytes.toHex()
                if (checkEndOfReceiveData()){
                    status = false
                    Log.i(TAG,"Complete --->${receiveFuelData}")
                    isRequestFuelData = false
                    tipLiveData.postValue("rev")
                    getFuelInfoSuccess()//??????????????????????????????????????????????????????
                    processFuelData()
                    //??????????????????????????????????????????????????????????????????????????????????????????????????????????????? APP?????????
                    if (bleDevice.isConnected){
                        triggerDisconnect()
                    }
                }
            }

        }
    }

    fun checkEndOfReceiveData() : Boolean {
        val tempData = receiveFuelData
        var dataSize = tempData.length
        var cursor = 0
        var isEndofFuelData = false
        if (dataSize > 16){
            var isContinue = true
            while (isContinue){
                val length = tempData.substring(cursor + 2,cursor + 4).toInt(16) * 2
                val firstDataByte = tempData.substring(cursor + 14, cursor + 16)
                if (firstDataByte == "FF"){
                    isContinue = false
                    isEndofFuelData = true
                }else{
                    if (dataSize < cursor + length + 18){
                        isContinue = false
                        isEndofFuelData = false
                    }else{
                        if (dataSize == cursor + length + 18){
                            isContinue = false
                            isEndofFuelData = false
                        }else{
                            if (dataSize > cursor + length + 18 && dataSize < cursor +length + 18 + 16){
                                isContinue = false
                                isEndofFuelData = false
                            }else{
                                cursor += length + 18
                            }
                        }
                    }
                }

            }
        }
        return  isEndofFuelData
    }

    private fun getFuelInfoSuccess() {

        var dataLen = 1.toString(16)
        val len = (256 - 1).toString(16)
        Log.i(TAG, "onClick: $len")

        val identify = getIdentify()
        val data = "01ff"+identify+"0181ff"

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
        result.append(dataLen)  //???????????? 1??????
        result.append(len)  //??????????????? 1??????
        result.append(identify?.substring(0,2)) //identify 00
        result.append(identify?.substring(2,4))  //identify 00
        result.append("01")
        result.append("81")
        result.append("ff")
        result.append(sumLen)
        result.append("03")
        
        val write = result.toString()
        Log.i(TAG, "onClick: $write")

        val inputBytes: ByteArray = write.toByteArray()

        if(bleDevice.isConnected) {
            mConnection.writeCharacteristic(Contants.WRITE_UUID,inputBytes.hex2byte())
                .map { Log.i(TAG, "getDeviceInfo: write --> ${it.toHex()}") }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ onWriteSuccess() }, { onWriteFailure(it) })
                .let { connectionDisposable.add(it) }
        }else{
            Log.i(TAG, "getFuelInfo: ble is disconnected")
        }

    }


    private fun onNotificationSetupFailure(throwable: Throwable){
        Log.i(TAG, "onNotificationSetupFailure: $throwable")
        tipLiveData.postValue("connectionfail")
    }


    private fun triggerDisconnect(){
        connectionDisposable.clear()
        stateDisposable?.dispose()
        disconnectTriggerSubject.onNext(Unit)
    }


    fun processFuelData(){
        viewModelScope.launch {
            async(Dispatchers.IO) {
                val id = getIdentify() ?: return@async
                val device = database.deviceDao().getDevice(id)
                if (device != null){
                    val length = device.length.toInt()
                    val width = device.width.toInt()
                    Log.i(TAG,"processFuelData --->${receiveFuelData}")
                    val fuelList = ArrayList<String>()
                    var isContinue = true
                    var cursor = 0
//                    var new = StringBuilder()
                    var bigToLittelEnd = StringBuilder()
                    var realHeightIntValue: Int
                    var indexOfFuelData = 0
                    var indexOfConsumptionData = 0
                    var indexOfRefuelData = 0

                    val fuelConsumeList = ArrayList<String>()
                    val refuelList = ArrayList<String>()
//                    var average = String()

                    while (isContinue) {
                        val len = receiveFuelData.substring(cursor + 2, cursor + 4).toInt(16) * 2
                        Log.i(TAG,"processFuelData length --->${len}")
                        val firstDataByte = receiveFuelData.substring(cursor + 14, cursor + 16)
                        Log.i(TAG,"processFuelData firstDataByte --->${firstDataByte}")

                        cursor += 16
                        if (firstDataByte == "FF") {
                            isContinue = false
                        } else {
                            for (index in 0..((len - 2)/4 - 1)) {
                                val fuelData = receiveFuelData.substring(cursor + index*4 , cursor+ 4*(index + 1))
                                Log.i(TAG, "processFuelData: fueldata --------------------> $fuelData")
                                if (fuelData == "FDFF" || fuelData == "FEFF") {
                                    //??????
                                }else{
                                    bigToLittelEnd.append(fuelData.substring(2,4))
                                    bigToLittelEnd.append(fuelData.substring(0,2))
                                    realHeightIntValue = bigToLittelEnd.toString().toInt(16)
                                    bigToLittelEnd.clear()
                                    val realFuelData = (realHeightIntValue.toDouble()*length * width)/1000000
                                    Log.i(TAG, "processFuelData: height:-> $realHeightIntValue   fueldata real capavity -> $realFuelData")
                                    fuelList.add(indexOfFuelData,realFuelData.toString())
                                    if(indexOfFuelData > 0){
                                        var record = (fuelList[indexOfFuelData-1].toDouble() - realFuelData)
                                        if(record > 0){
                                            //fuel consume
//                                            average = String.format("%.2f", record)
                                            fuelConsumeList.add(indexOfConsumptionData, String.format("%.2f", record))
                                            indexOfConsumptionData++
                                        }
                                        // refuel next-current
                                        record = (realFuelData - fuelList[indexOfFuelData-1].toDouble())
                                        if(record > 0) {
                                            //refuel
                                            refuelList.add(indexOfRefuelData, String.format("%.2f", record))
                                            indexOfRefuelData++
                                        }
                                    }
                                    indexOfFuelData++
                                }
                            }
                            cursor += len - 2
                            cursor += 4
//                            Log.i(TAG,"processFuelData cursor --->${cursor}")
                        }
                    }
                    Log.i(TAG,"processFuelData fuelList All data --->${fuelList}")
//                    Log.i(TAG, "onNotificationReceived: max-> $maxFuel fuel ->${fuelList.size} fuel consume ${fuelConsumeList.size}  refuel ${refuelList.size}")
//                    setAverageOil(average)

                    var isNormal = true
                    val threshold = getThreshold()
                    var indexForException = 0

                    if (fuelList.size > 50){
                        indexForException = fuelList.size - 50
                    }else{
                        indexForException = fuelList.size
                    }
                    for (index in indexForException until fuelList.size){
                        if (index + 1 < fuelList.size - 1){
                            val firstData = fuelList[index].toDouble()
                            val nextData = fuelList[index+1].toDouble()
                            if (firstData - nextData > threshold){
                                isNormal = false
                                break
                            }
                        }
                    }
                    val averageConsumption = fuelConsumeList.last()
                    if (isNormal){
                        database.deviceDao().insertStatusAndAverage(id, "??????", averageConsumption)
                        fuelStatusLiveData.postValue("??????")
                    }else{
                        database.deviceDao().insertStatusAndAverage(id, "??????", averageConsumption)
                        fuelStatusLiveData.postValue("??????")
                    }
                    fuelLiveData.postValue(fuelList)

                    val dataList = arrayListOf<Fuel>()
                    for (i in 0 until fuelList.size){
                        dataList.add(Fuel(i,id,fuelList[i]))
                    }
                    Log.i(TAG, "processFuelData: datalist size --> ${dataList.size}")
                    database.fuelDataDao().insertFuelData(dataList)

                    val fuelConsumedataList = arrayListOf<FuelConsume>()
                    for (i in 0 until fuelConsumeList.size){
                        fuelConsumedataList.add(FuelConsume(i,id,fuelConsumeList[i]))
                    }
                    database.fuelConsuemDataDao().insertFuelConsumeData(fuelConsumedataList)
                    val refuelDataList = arrayListOf<Refuel>()
                    for (i in 0 until refuelList.size){
                        refuelDataList.add(Refuel(i,id,refuelList[i]))
                    }
                    if (fuelConsumeList.size > 0){
                        averageFuelConsumeLiveData.postValue(fuelConsumeList.last())
                    }
                    database.refuelDataDao().insertRefuelData(refuelDataList)
                    tipLiveData.postValue("process")
                }

            }
        }
    }


    private fun getDeviceInfo() {
        var dataLen = 1.toString(16)   //???????????? 1??????
        val len = (256 - 1).toString(16) //??????????????? 1??????
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
        //mConnection.writeCharacteristic(Contants.WRITE_UUID,inputBytes)
        Log.i(TAG, "getDeviceInfo: connect state -> ${bleDevice.isConnected}")

        mConnection.writeCharacteristic(Contants.WRITE_UUID,inputBytes.hex2byte())
            .map { Log.i(TAG, "getDeviceInfo: write --> ${it.toHex()}") }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ onWriteSuccess() }, { onWriteFailure(it) })
            .let { connectionDisposable.add(it) }
    }


    private fun getFuelInfo() {
        isRequestFuelData = true

        var dataLen = 1.toString(16)
        val len = (256 - 1).toString(16)
        Log.i(TAG, "onClick: $len")

        val identify = getIdentify()
        val data = "01ff"+identify+"018100"

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
        result.append(dataLen)  //???????????? 1??????
        result.append(len)  //??????????????? 1??????
        result.append(identify?.substring(0,2)) //identify 00
        result.append(identify?.substring(2,4))  //identify 00
        result.append("01")
        result.append("81")
        result.append("00")
        result.append(sumLen)
        result.append("03")


        val write = result.toString()
        Log.i(TAG, "onClick: $write")

        val inputBytes: ByteArray = write.toByteArray()

        if(bleDevice.isConnected) {
            tipLiveData.postValue("requestFuelData")
            mConnection.writeCharacteristic(Contants.WRITE_UUID,inputBytes.hex2byte())
                .map { Log.i(TAG, "getDeviceInfo: write --> ${it.toHex()}") }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ onWriteSuccess() }, { onWriteFailure(it) })
                .let { connectionDisposable.add(it) }
        }else{
            Log.i(TAG, "getFuelInfo: ble is disconnected")
        }

    }

    private fun onWriteSuccess(){
        Log.i(TAG, "onWriteSuccess: ")
    }

    private fun onWriteFailure(throwable: Throwable){
        Log.i(TAG, "onWriteFailure: ${throwable.message}")
        tipLiveData.postValue("writeDatafail")
    }

    fun getFuelData(){
        viewModelScope.launch {
            async(Dispatchers.IO) {
                val id = getIdentify() ?: return@async
                val list = database.fuelDataDao().getFuelData(id)
                val fuelList = ArrayList<String>()
                for (element in list){
                    element.capacity?.let { fuelList.add(it) }
                }
                fuelLiveData.postValue(fuelList)
                val device = database.deviceDao().getDevice(id)
                if (device != null){
                    val consumeModelList = database.fuelConsuemDataDao().getFuelConsumeData(id)
                    val fuelConsumeList = ArrayList<String>()
                    for (element in consumeModelList){
                        element.capacity?.let { fuelConsumeList.add(it) }
                        Log.i(TAG, "consumeModelList: --> ${element.capacity}")
                    }
                    if (fuelConsumeList.size > 0){
                        var isNormal = true
                        val threshold = getThreshold()
                        var indexForException = 0

                        if (fuelConsumeList.size > 50){
                            indexForException = fuelConsumeList.size - 50
                        }else{
                            indexForException = 0
                        }

                        for (index in indexForException until fuelConsumeList.size){
                            val firstConsumptionData = fuelConsumeList[index]
                            if (firstConsumptionData.toDouble() > threshold){
                                isNormal = false
                                break
                            }
                        }

                        val averageConsumption = fuelConsumeList.last()
                        if (isNormal){
                            database.deviceDao().insertStatusAndAverage(id, "??????", averageConsumption)
                            fuelStatusLiveData.postValue("??????")
                        }else{
                            database.deviceDao().insertStatusAndAverage(id, "??????", averageConsumption)
                            fuelStatusLiveData.postValue("??????")
                        }
                        averageFuelConsumeLiveData.postValue(averageConsumption)
                    }
                }
            }
        }
    }

}
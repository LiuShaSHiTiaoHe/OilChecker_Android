package com.example.oilchecker.fragment

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.icu.util.LocaleData
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
import com.example.oilchecker.util.*
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
import khronos.*
import kotlinx.coroutines.*
import java.lang.StringBuilder
import java.time.*
import java.time.format.DateTimeFormatter
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

//    var fuelLiveData = MutableLiveData<ArrayList<String>>()
    var fuelLiveData = MutableLiveData<ArrayList<Map<String, String>>>()
    var averageFuelConsumeLiveData = MutableLiveData<String>()
    var fuelStatusLiveData = MutableLiveData<String>()
    var tipLiveData = MutableLiveData<String>()
    var status: Boolean = false
    var bleSendState: BleSendDataState = BleSendDataState.SendRequestDeviceInfo

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

        //首页segment index
        fun setSegmentIndex(value: Int){
            SpUtils.put("segmentIndex",value)
        }

        fun getSegmentIndex(): Int{
            val value = SpUtils.getInt("segmentIndex")
            if (value == null){
                return  2
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

    private fun onWriteSuccess(){
        Log.i(TAG, "onWriteSuccess: ")
    }

    private fun onWriteFailure(throwable: Throwable){
        Log.i(TAG, "onWriteFailure: ${throwable.message}")
        tipLiveData.postValue("writeDatafail")
    }

    private fun onNotificationSetupFailure(throwable: Throwable){
        Log.i(TAG, "onNotificationSetupFailure: $throwable")
        tipLiveData.postValue("connectionfail")
    }

    //断开蓝牙连接
    private fun triggerDisconnect(){
        connectionDisposable.clear()
        stateDisposable?.dispose()
        disconnectTriggerSubject.onNext(Unit)
    }


    private fun onNotificationReceived(bytes: ByteArray) {
        Log.i(TAG, "---onNext--->>>>$bytes --->${bytes.toHex()}")
        //TODO  将错误信息存入数据库，在设置页面展示
        if (!status) {
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
                        setDeviceTime()
                    }
                }
                if (result.substring(12,14) == "88"){
                    status = true
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
                    getFuelInfoSuccess()//接收到全部数据之后，发送接收成功指令
                    processFuelData()
                    //接收数据成功之后，最好是断开蓝牙连接！！！！！！不然我让蓝牙设备断电之后。 APP会闪退
                    if (bleDevice.isConnected){
                        triggerDisconnect()
                    }
                }
            }

        }
    }

    //在接收的过程中，实时检查，数据是否传输完成且完整
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

    //接收油量数据完成之后，发送成功信息给硬件设备
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
        result.append(dataLen)  //数据长度 1字节
        result.append(len)  //数据长补数 1字节
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
    //时间设置。同步油量数据之前
    private fun setDeviceTime() {
        var dataLen = 6.toString(16)   //数据长度 1字节
        val len = (256 - 6).toString(16) //数据长补数 1字节
        if (dataLen.length == 1){
            dataLen = "0$dataLen"
        }
        val date = LocalDate.now()
        val fmt = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
        val timeNow = date.format(fmt).substring(2,12)
        val identify = getIdentify()
        val data = dataLen + len + identify + "01" + "87" + timeNow
        var sum = 0
        for (i in 0 until data.length/2){
            sum = sum xor data.substring(i*2,i*2+2).toInt(16)
        }
        var sumLen = sum.toString(16)
        if (sumLen.length == 1){
            sumLen = "0$sumLen"
        }

        var result = StringBuilder()
        result.append("02")
        result.append(dataLen)  //数据长度 1字节
        result.append(len)  //数据长补数 1字节
        result.append(identify?.substring(0,2)) //identify 00
        result.append(identify?.substring(2,4))  //identify 00
        result.append("01")
        result.append("87")
        result.append(timeNow)
        result.append(sumLen)
        result.append("03")

        val write = result.toString()
        Log.i(TAG, "setDeviceTime: $write")

        val inputBytes: ByteArray = write.toByteArray()
        if(bleDevice.isConnected) {
            mConnection.writeCharacteristic(Contants.WRITE_UUID,inputBytes.hex2byte())
                .map { Log.i(TAG, "setDeviceTime: write --> ${it.toHex()}") }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ onWriteSuccess() }, { onWriteFailure(it) })
                .let { connectionDisposable.add(it) }
        }else{
            Log.i(TAG, "setDeviceTime: ble is disconnected")
        }
    }
    //请求设备基本信息
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
        //mConnection.writeCharacteristic(Contants.WRITE_UUID,inputBytes)
        Log.i(TAG, "getDeviceInfo: connect state -> ${bleDevice.isConnected}")

        mConnection.writeCharacteristic(Contants.WRITE_UUID,inputBytes.hex2byte())
            .map { Log.i(TAG, "getDeviceInfo: write --> ${it.toHex()}") }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ onWriteSuccess() }, { onWriteFailure(it) })
            .let { connectionDisposable.add(it) }
    }

    //发送请求油量数据的请求
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
        result.append(dataLen)  //数据长度 1字节
        result.append(len)  //数据长补数 1字节
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

    fun getTimeRange() : List<String>{
        val segmentIndex = getSegmentIndex()
        val today = Dates.today
        val currentTimeInterval = today.toString("yyyy-MM-dd HH:mm:ss").toDateLong()
        val startOfWeek = getFirstDayOfWeek()
        val endOfWeek = today + 1.week
        val startOfMonth = today.beginningOfMonth
        val endOfMonth = today.endOfMonth
        val startOfYear = today.beginningOfYear
        val endOfYear = today.endOfYear



        var startDate: String = ""
        var endDate: String = ""
        when (segmentIndex){
            0 -> startDate =
        }

    }
    //从数据库 获取油量数据
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
                            database.deviceDao().insertStatusAndAverage(id, "正常", averageConsumption)
                            fuelStatusLiveData.postValue("正常")
                        }else{
                            database.deviceDao().insertStatusAndAverage(id, "异常", averageConsumption)
                            fuelStatusLiveData.postValue("异常")
                        }
                        averageFuelConsumeLiveData.postValue(averageConsumption)
                    }
                }
            }
        }
    }

    //处理油量数据
    fun processFuelData(){
        viewModelScope.launch {
            async(Dispatchers.IO) {
                val id = getIdentify() ?: return@async
                val device = database.deviceDao().getDevice(id)
                val testTimeString = "202107291209"
                if (device != null){
                    val length = device.length.toInt()
                    val width = device.width.toInt()

//                    val fuelList = ArrayList<String>()
//                    val fuelConsumeList = ArrayList<String>()
//                    val refuelList = ArrayList<String>()

                    var newFuelList = ArrayList<Map<String, String>>()
                    var newFuelConsumeList = ArrayList<Map<String, String>>()
                    var newRefuelList = ArrayList<Map<String, String>>()

                    var isContinue = true
                    var bigToLittelEnd = StringBuilder()
                    var realHeightIntValue: Int

                    var cursor = 0
                    var indexOfFuelData = 0
                    var indexOfConsumptionData = 0
                    var indexOfRefuelData = 0

                    var deviceTimeString = ""
                    while (isContinue) {
                        val len = receiveFuelData.substring(cursor + 2, cursor + 4).toInt(16) * 2
                        val firstDataByte = receiveFuelData.substring(cursor + 14, cursor + 16)

                        Log.i(TAG,"processFuelData length --->${len}")
                        Log.i(TAG,"processFuelData firstDataByte --->${firstDataByte}")

                        cursor += 16
                        if (firstDataByte == "FF") {//数据区第一个字节为FF表示 传输结束
                            isContinue = false
                        } else {
//                            var isTimeDataFlag = false
                            for (index in 0..((len - 2)/4 - 1)) {
                                val fuelData = receiveFuelData.substring(cursor + index*4 , cursor+ 4*(index + 1))
//                                if (isTimeDataFlag){//接收到时间信息，处理FCFF后面的时间数据 6字节 十进制
//                                    if (index < 7){
//                                        deviceTimeString = deviceTimeString + fuelData
//                                        if (index == 6){//20 + 21 + 08 + 11 + 11 + 53 + 22 = 20210811115322
//                                            deviceTimeString = "20" + deviceTimeString
//                                        }
//                                    }
//                                }
                                Log.i(TAG, "processFuelData: fueldata --------------------> $fuelData")
                                if (fuelData == "FDFF" || fuelData == "FEFF" || fuelData == "FBFF") {
                                    //过滤
                                }else if (fuelData == "FCFF"){
//                                    //增加的时间，后面的数据都有加上时间数据，每条数据的时间间隔为2分钟
//                                    isTimeDataFlag = true
//                                    deviceTimeString = ""
                                } else{
                                    //设备时间信息之前的数据，视为无效的数据，过滤掉
//                                    if (deviceTimeString.isEmpty()){
//                                        continue
//                                    }
//                                    val date = deviceTimeString.toDate("yyyyMMddHHmmss") + 2.minutes
                                    val date = testTimeString.toDate("yyyyMMddHHmmss") + 2.minutes
                                    bigToLittelEnd.append(fuelData.substring(2,4))
                                    bigToLittelEnd.append(fuelData.substring(0,2))
                                    realHeightIntValue = bigToLittelEnd.toString().toInt(16)
                                    bigToLittelEnd.clear()
                                    val realFuelData = (realHeightIntValue.toDouble()*length * width)/1000000
                                    Log.i(TAG, "processFuelData: height:-> $realHeightIntValue   fueldata real capavity -> $realFuelData")

//                                    fuelList.add(indexOfFuelData,realFuelData.toString())
                                    val timeInterval = date.toString("yyyy-MM-dd HH:mm:ss").toDateLong().toString()
                                    val timeFormatterString = date.toString("yyyy-MM-dd HH:mm:ss")
                                    newFuelList.add(mapOf("time" to timeFormatterString,"timeInterval" to timeInterval, "fuel" to realFuelData.toString(), "index" to indexOfFuelData.toString()))
                                    if(indexOfFuelData > 0){
//                                        var record = (fuelList[indexOfFuelData-1].toDouble() - realFuelData)
//                                        if(record > 0){
//                                            //fuel consume
//                                            fuelConsumeList.add(indexOfConsumptionData, String.format("%.2f", record))
//
//                                            indexOfConsumptionData++
//                                        }
//                                        // refuel next-current
//                                        record = (realFuelData - fuelList[indexOfFuelData-1].toDouble())
//                                        if(record > 0) {
//                                            //refuel
//                                            refuelList.add(indexOfRefuelData, String.format("%.2f", record))
//                                            indexOfRefuelData++
//                                        }
                                        val preFuelDataMap = newFuelList[indexOfFuelData - 1]
                                        val changedFuelDataBetweenRecords = preFuelDataMap.get("fuel")!!.toDouble() - realFuelData
                                        if (changedFuelDataBetweenRecords < 0){
                                            //油量在增加，有加油的操作
                                            newRefuelList.add(mapOf("time" to timeFormatterString, "timeInterval" to timeInterval, "refuel" to changedFuelDataBetweenRecords.toString(), "index" to indexOfRefuelData.toString()))
                                            indexOfRefuelData++
                                        }else{
                                            //耗油操作
                                            newFuelConsumeList.add(mapOf("time" to timeFormatterString, "timeInterval" to timeInterval, "consumption" to changedFuelDataBetweenRecords.toString(), "index" to indexOfConsumptionData.toString()))
                                            indexOfConsumptionData++
                                        }
                                    }
                                    indexOfFuelData++
                                }
                            }
                            cursor += len - 2
                            cursor += 4
                        }
                    }
                    Log.i(TAG,"processFuelData fuelList All data --->${newFuelList}")
                    Log.i(TAG, "processFuelData: datalist size --> ${newFuelList.size}")


                    val dataList = arrayListOf<Fuel>()
                    for (index in 0 until newFuelList.size){
//                        dataList.add(Fuel(i,id,fuelList[i]))TODO：数据库改变，增加了时间字段，入库的操作
                        val fuelDataMap = newFuelList[index]
                        dataList.add(Fuel(index, id, fuelDataMap.get("fuel")!!, fuelDataMap.get("time"), fuelDataMap.get("timeInterval")!!.toLong()))
                    }
                    database.fuelDataDao().insertFuelData(dataList)

                    val fuelConsumedataList = arrayListOf<FuelConsume>()
                    for (index in 0 until newFuelConsumeList.size){
//                        fuelConsumedataList.add(FuelConsume(i,id,fuelConsumeList[i]))
                        val fuelDataMap = newFuelConsumeList[index]
                        fuelConsumedataList.add(FuelConsume(index, id, fuelDataMap.get("fuel")!!, fuelDataMap.get("time"), fuelDataMap.get("timeInterval")!!.toLong()))
                    }

                    database.fuelConsuemDataDao().insertFuelConsumeData(fuelConsumedataList)
                    val refuelDataList = arrayListOf<Refuel>()
                    for (index in 0 until newRefuelList.size){
//                        refuelDataList.add(Refuel(i,id,refuelList[i]))
                        val fuelDataMap = newRefuelList[index]
                        refuelDataList.add(Refuel(index, id, fuelDataMap.get("fuel")!!, fuelDataMap.get("time"), fuelDataMap.get("timeInterval")!!.toLong()))
                    }
//                    if (fuelConsumeList.size > 0){
//                        averageFuelConsumeLiveData.postValue(fuelConsumeList.last())
//                    }
                    database.refuelDataDao().insertRefuelData(refuelDataList)
                    tipLiveData.postValue("process")
                    fuelLiveData.postValue(newFuelList)
                }

            }
        }
    }

}
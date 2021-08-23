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
import com.example.oilchecker.data.entity.*
import com.example.oilchecker.di.DatabaseModel
import com.example.oilchecker.util.*
import com.polidea.rxandroidble2.NotificationSetupMode
import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.RxBleDevice
import com.polidea.rxandroidble2.samplekotlin.util.*
import com.polidea.rxandroidble2.samplekotlin.util.isConnected
import dagger.hilt.android.internal.managers.FragmentComponentManager
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import khronos.*
import khronos.Duration
import kotlinx.coroutines.*
import java.lang.StringBuilder
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList
import kotlin.concurrent.schedule
import kotlin.math.abs

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

    var fuelLiveData = MutableLiveData<ArrayList<Fuel>>()
    var fuelChangedLiveData = MutableLiveData<ArrayList<FuelChange>>()
    var tipLiveData = MutableLiveData<String>()

    var fuelChartLiveData = MutableLiveData<ArrayList<ChartDateModel>>()
    var refuelChartLiveData = MutableLiveData<ArrayList<ChartDateModel>>()
    var consumptionChartLiveData = MutableLiveData<ArrayList<ChartDateModel>>()

    var status: Boolean = false

    // TODO: Implement the ViewModel
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
            tipLiveData.postValue(ToastTips.B_Disconnect)
        }
    }

    private fun onConnectionFailure(throwable: Throwable) {
        Log.i(TAG, "onConnectionFailure: $throwable")
        tipLiveData.postValue(ToastTips.B_ConnectFailed)
    }

    private fun onConnectionReceived(connection: RxBleConnection) {
        mConnection = connection
        tipLiveData.postValue(ToastTips.B_Connected)
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
        tipLiveData.postValue(ToastTips.B_SendDataFailed)
    }

    private fun onNotificationSetupFailure(throwable: Throwable){
        Log.i(TAG, "onNotificationSetupFailure: $throwable")
        tipLiveData.postValue(ToastTips.B_ConnectFailed)
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
                    tipLiveData.postValue(ToastTips.R_ReceivedFuelData)
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

        val identify = UserPreference.getIdentify()
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

        val today = LocalDateTime.now()
        val myDateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
        var timestring = myDateTimeFormatter.format(today)
        val timeNow = timestring.substring(2,14)

        var timeDataString = ""
        for (i in 0 until timeNow.length/2){
            var sub = timeNow.substring(i*2, i*2+2).toInt().toString(16)
            if (sub.length == 1){
                sub = "0$sub"
            }
            timeDataString += sub
        }

        val identify = UserPreference.getIdentify()
        val data = dataLen + len + identify + "01" + "87" + timeDataString
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
        result.append(timeDataString)
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

        val identify = UserPreference.getIdentify()
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
            tipLiveData.postValue(ToastTips.R_FuelData)
            mConnection.writeCharacteristic(Contants.WRITE_UUID,inputBytes.hex2byte())
                .map { Log.i(TAG, "getDeviceInfo: write --> ${it.toHex()}") }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ onWriteSuccess() }, { onWriteFailure(it) })
                .let { connectionDisposable.add(it) }
        }else{
            Log.i(TAG, "getFuelInfo: ble is disconnected")
        }

    }



    //处理油量数据
    fun processFuelData(){
        viewModelScope.launch {
            async(Dispatchers.IO) {
                val id = UserPreference.getIdentify() ?: return@async
                val device = database.deviceDao().getDevice(id)
                if (device != null){
                    val length = device.length.toInt()
                    val width = device.width.toInt()
                    var newFuelList = ArrayList<Map<String, String>>()
                    var newChangedFuelList = ArrayList<Map<String, String>>()
                    var isContinue = true
                    var bigToLittelEnd = StringBuilder()
                    var realHeightIntValue: Int
                    var cursor = 0
                    var indexOfFuelData = 0
                    var startOfGetTime = 0
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
                            var isTimeDataFlag = false
                            for (index in 0..((len - 2)/4 - 1)) {
                                val fuelData = receiveFuelData.substring(cursor + index*4 , cursor+ 4*(index + 1))
                                if (fuelData == "FDFF" || fuelData == "FEFF" || fuelData == "FBFF") {
                                    //过滤
                                }else if (fuelData == "FCFF"){
                                    //增加的时间，后面的数据都有加上时间数据，每条数据的时间间隔为1分钟
                                    isTimeDataFlag = true
                                    deviceTimeString = ""
                                    startOfGetTime = 0
                                } else{
                                    if (isTimeDataFlag) {//接收到时间信息，处理FCFF后面的时间数据 6字节 十进制

                                        Log.i(TAG, "processFuelData: TimeTimeTime:-> $fuelData")
                                        var time1 = fuelData.substring(0, 2).toInt(16).toString()
                                        var time2 = fuelData.substring(2, 4).toInt(16).toString()
                                        if (time1.length == 1){
                                            time1 = "0$time1"
                                        }
                                        if (time2.length == 1){
                                            time2 = "0$time2"
                                        }
                                        deviceTimeString = deviceTimeString + time1 + time2
                                        if (startOfGetTime == 0) {//年，月
                                            if (time1.toInt() < 21 || time2.toInt() > 12 || time2.toInt() < 1) {
                                                deviceTimeString = ""
                                            }
                                        }
                                        if (startOfGetTime == 1) {//日，时
                                            if (time1.toInt() < 1 || time1.toInt() > 31 || time2.toInt() > 24 || time2.toInt() < 0) {
                                                deviceTimeString = ""
                                            }
                                        }
                                        if (startOfGetTime == 2) {//分，秒。20 + 21 + 08 + 11 + 11 + 53 + 22 = 20210811115322
                                            isTimeDataFlag = false
                                            if (time1.toInt() < 0 || time1.toInt() > 59 || time2.toInt() > 59 || time2.toInt() < 0) {
                                                deviceTimeString = ""
                                            }else{
                                                deviceTimeString = "20" + deviceTimeString
                                            }
                                        }
                                        startOfGetTime++
                                        continue
                                    }

                                    //设备时间信息之前的数据，视为无效的数据，过滤掉
                                    if (deviceTimeString.length < 14){
                                        continue
                                    }
                                    val date = deviceTimeString.toDate("yyyyMMddHHmmss") + 1.minutes
                                    deviceTimeString = date.toString("yyyyMMddHHmmss")
                                    bigToLittelEnd.append(fuelData.substring(2,4))
                                    bigToLittelEnd.append(fuelData.substring(0,2))
                                    realHeightIntValue = bigToLittelEnd.toString().toInt(16)
                                    bigToLittelEnd.clear()
                                    val realFuelData = (realHeightIntValue.toDouble()*length * width)/1000000
                                    Log.i(TAG, "processFuelData: height:-> $realHeightIntValue   fueldata real capavity -> $realFuelData")

                                    val timeInterval = date.toString("yyyy-MM-dd HH:mm:ss").toDateLong().toString()
                                    val timeFormatterString = date.toString("yyyy-MM-dd HH:mm:ss")
                                    newFuelList.add(mapOf("time" to timeFormatterString,"timeInterval" to timeInterval, "fuel" to realFuelData.toString(), "index" to indexOfFuelData.toString()))
                                    if(indexOfFuelData > 0){
                                        val preFuelDataMap = newFuelList[indexOfFuelData - 1]
                                        val changedFuelDataBetweenRecords = preFuelDataMap.get("fuel")!!.toDouble() - realFuelData
                                        if (changedFuelDataBetweenRecords < 0){
                                            newChangedFuelList.add(mapOf(
                                                "time" to timeFormatterString,
                                                "timeInterval" to timeInterval,
                                                "fueldata" to abs(changedFuelDataBetweenRecords).toString(),
                                                "index" to indexOfFuelData.toString(),
                                                "type" to FuelChangedType.REFUEL.type))
                                        }else if(changedFuelDataBetweenRecords > 0){
                                            newChangedFuelList.add(mapOf(
                                                "time" to timeFormatterString,
                                                "timeInterval" to timeInterval,
                                                "fueldata" to changedFuelDataBetweenRecords.toString(),
                                                "index" to indexOfFuelData.toString(),
                                                "type" to FuelChangedType.CONSUMPTION.type))
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
                        val fuelDataMap = newFuelList[index]
                        dataList.add(Fuel(index, id, fuelDataMap["fuel"]!!, fuelDataMap["time"], fuelDataMap["timeInterval"]!!.toLong()))
                    }
                    database.fuelDataDao().insertFuelData(dataList)

                    val fuelChangedDataList = arrayListOf<FuelChange>()
                    for (index in 0 until newChangedFuelList.size){
                        val fuelchangedDataMap = newChangedFuelList[index]
                        fuelChangedDataList.add(FuelChange(index,
                            fuelchangedDataMap["type"], id, fuelchangedDataMap["fueldata"]!!.toDouble(),
                            fuelchangedDataMap["time"], fuelchangedDataMap["timeInterval"]!!.toLong()))
                    }
                    database.fuelChangeDao().insertFuelChangedData(fuelChangedDataList)
                    tipLiveData.postValue(ToastTips.S_ProcessFuelDataComplete)
                }

            }
        }
    }



    fun getDisplayTimeRange(): String{
        val timeRange = getTimeRange()
        var startTime = timeRange[0].toDate("yyyy-MM-dd HH:mm:ss")
        val endTime = timeRange[1].toDate("yyyy-MM-dd HH:mm:ss")
        val segmentIndex = UserPreference.getSegmentIndex()
        var displayTimeString: String = ""
        when (segmentIndex){
            0 -> {
                displayTimeString = startTime.toString("yyyy/MM/dd")
            }
            1 -> {
                displayTimeString = startTime.toString("MM/dd") + " - " + endTime.toString("MM/dd")
            }
            2 -> {
                displayTimeString = startTime.toString("yyyy/MM")
            }
            3 -> {
                displayTimeString = startTime.toString("yyyy")
            }
        }

        return displayTimeString
    }

    fun getTimeRange() : ArrayList<String>{
        val segmentIndex = UserPreference.getSegmentIndex()
        val offset = UserPreference.getDateOffset()
        var startDate = ""
        var endDate = ""
        val today = Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant())


        when (segmentIndex){
            0 -> {
                val startOfDay = today.beginningOfDay - offset.days
                val endOfDay = today.endOfDay - offset.days
                startDate = startOfDay.toString("yyyy-MM-dd HH:mm:ss")
                endDate = endOfDay.toString("yyyy-MM-dd HH:mm:ss")
            }
            1 -> {
                val startOfWeek = getFirstDayOfWeek() - offset.weeks
                val endOfWeek = startOfWeek + 1.week - 1.days
                startDate = startOfWeek.toString("yyyy-MM-dd HH:mm:ss")
                endDate = endOfWeek.endOfDay.toString("yyyy-MM-dd HH:mm:ss")
            }
            2 -> {
                val startOfMonth = today.beginningOfMonth - (offset+1).months
                val endOfMonth = today.endOfMonth - (offset+1).months
                startDate = startOfMonth.toString("yyyy-MM-dd HH:mm:ss")
                endDate = endOfMonth.toString("yyyy-MM-dd HH:mm:ss")
            }
            3 -> {
                val startOfYear = today.beginningOfYear - offset.years
                val endOfYear = today.endOfYear - offset.years
                startDate = startOfYear.toString("yyyy-MM-dd HH:mm:ss")
                endDate = endOfYear.toString("yyyy-MM-dd HH:mm:ss")
            }
        }
        Log.i(TAG, "$startDate ------- $endDate -----$offset")
        val rangList = ArrayList<String>()
        rangList.add(startDate)
        rangList.add(endDate)
        return rangList

    }

    //从数据库 获取油量数据
    fun getFuelData(){
        viewModelScope.launch {
            async(Dispatchers.IO) {
                val id = UserPreference.getIdentify() ?: return@async
                val list = database.fuelDataDao().getFuelData(id)
                val fuelChangedDataList = database.fuelChangeDao().getFuelChangedData(id)
                val timeRange = getTimeRange()
                val startDateTime = timeRange[0].toDateLong()
                val endDateTime = timeRange[1].toDateLong()
                val threshold = UserPreference.getThreshold()
                //油量数据   暂时不需要了
                var newFuelList = ArrayList<Fuel>()
                for (item in list){
                    item.recordTimeInterval?.let {
                        if (it > startDateTime && it < endDateTime){
                            newFuelList.add(item)
                        }
                    }
                }
                fuelLiveData.postValue(newFuelList)


                //油量变化数据
                var newFuelChangedList = ArrayList<FuelChange>()
                for (item in fuelChangedDataList){
                    if (item.recordTimeInterval!! > startDateTime && item.recordTimeInterval!! < endDateTime){
                        newFuelChangedList.add(item)
                    }
                }
                newFuelChangedList.sortBy { it.recordTimeInterval }
                var sortedFuelChangedList = getSortedUnusualFuelChangedData(newFuelChangedList)
                fuelChangedLiveData.postValue(sortedFuelChangedList)
            }
        }
    }

    fun getSortedUnusualFuelChangedData(newFuelChangedList: ArrayList<FuelChange>) : ArrayList<FuelChange>{
        var sortedFuelChangedList = ArrayList<FuelChange>()
        var consumptionCount = 0
        val threshold = UserPreference.getThreshold()

        for (item in newFuelChangedList) {
            if (item.type == FuelChangedType.REFUEL.type) {
                consumptionCount = 0
                if (sortedFuelChangedList.size > 0){
                    val lastOne = sortedFuelChangedList.last()
                    val lastTimeInterval = lastOne.recordTimeInterval!!.toLong()
                    val currentTimeInterval = item.recordTimeInterval!!.toLong()
                    val interval = currentTimeInterval - lastTimeInterval
                    val fixation: Long = 60000
                    if (lastOne.type == item.type){//连续的加油记录，并且时间连续,合并处理
                        if (interval == fixation){
                            val model = FuelChange(item.id,item.type,item.deviceId,item.fuelData!! + lastOne.fuelData!!,item.recordTime, item.recordTimeInterval)
                            sortedFuelChangedList.removeLast()
                            sortedFuelChangedList.add(model)
                        }else{
                            if (lastOne.fuelData!! < threshold){
                                sortedFuelChangedList.removeLast()
                            }
                            sortedFuelChangedList.add(item)
                        }
                    }else{
                        if (lastOne.fuelData!! < threshold){
                            sortedFuelChangedList.removeLast()
                        }
                        sortedFuelChangedList.add(item)
                    }
                }else{
                    sortedFuelChangedList.add(item)
                }
            }else{
                consumptionCount++
                if (sortedFuelChangedList.size > 0){
                    val lastOne = sortedFuelChangedList.last()
                    val lastTimeInterval = lastOne.recordTimeInterval!!.toLong()
                    val currentTimeInterval = item.recordTimeInterval!!.toLong()
                    val interval = currentTimeInterval - lastTimeInterval
                    val fixation: Long = 60000

                    if (lastOne.type == item.type){
                        if (interval == fixation){
                            var totalConsumprion = item.fuelData!! + lastOne.fuelData!!
                            if (totalConsumprion > threshold){
                                if (totalConsumprion/consumptionCount > 1){
                                    val model = FuelChange(item.id,item.type,item.deviceId,item.fuelData!! + lastOne.fuelData!!,item.recordTime, item.recordTimeInterval)
                                    sortedFuelChangedList.removeLast()
                                    sortedFuelChangedList.add(model)

                                }else{
                                    sortedFuelChangedList.removeLast()
                                    consumptionCount = 0
                                }
                            }else{
                                val model = FuelChange(item.id,item.type,item.deviceId,item.fuelData!! + lastOne.fuelData!!,item.recordTime, item.recordTimeInterval)
                                sortedFuelChangedList.removeLast()
                                sortedFuelChangedList.add(model)
                            }
                        }else{
                            consumptionCount = 0
                            if (lastOne.fuelData!! < threshold){
                                sortedFuelChangedList.removeLast()
                            }
                            sortedFuelChangedList.add(item)
                        }
                    }else{
                        if (lastOne.fuelData!! < threshold){
                            sortedFuelChangedList.removeLast()
                        }
                        sortedFuelChangedList.add(item)
                    }
                }else{
                    sortedFuelChangedList.add(item)
                }
            }
        }
        if (sortedFuelChangedList.size > 0){
            val lastOne = sortedFuelChangedList.last()
            if (lastOne.fuelData!! < threshold){
                sortedFuelChangedList.removeLast()
            }
        }

        return sortedFuelChangedList
    }

    fun getChartStyleFuelData(){
        viewModelScope.launch {
            async(Dispatchers.Default) {
                val id = UserPreference.getIdentify() ?: return@async
                val list = database.fuelDataDao().getFuelData(id)
                val originalFuelChangedDataList = database.fuelChangeDao().getFuelChangedData(id)
                val timeRange = getTimeRange()
                val startDateTime = timeRange[0].toDateLong()
                val endDateTime = timeRange[1].toDateLong()
                val segmentIndex = UserPreference.getSegmentIndex()
                var chartFuelArray = ArrayList<ChartDateModel>()
                var chartRefuelArray = ArrayList<ChartDateModel>()
                var chartconsumptionArray = ArrayList<ChartDateModel>()

                var beforeSortFuelChangedDataList = ArrayList<FuelChange>()
                for (item in originalFuelChangedDataList){
                    if (item.recordTimeInterval!! > startDateTime && item.recordTimeInterval!! < endDateTime){
                        beforeSortFuelChangedDataList.add(item)
                    }
                }
                var fuelChangedDataList = getSortedUnusualFuelChangedData(beforeSortFuelChangedDataList)

                when (segmentIndex){
                    0 -> {
                        for (index in 0 until 24){
                            chartFuelArray.add(ChartDateModel(index.toLong(),0.0, 0))
                            chartRefuelArray.add(ChartDateModel(index.toLong(),0.0, 0))
                            chartconsumptionArray.add(ChartDateModel(index.toLong(),0.0, 0))
                        }
                    }
                    1 -> {
                        val startOfDay = startDateTime.toDateStr().toDate("yyyy-MM-dd HH:mm:ss").beginningOfDay
                        for (index in 0 until 7){
                            val time = startOfDay + index.days
                            chartFuelArray.add(ChartDateModel(time.toString("yyyy-MM-dd HH:mm:ss").toDateLong().getDateDay().toLong(),0.0, 0))
                            chartRefuelArray.add(ChartDateModel(time.toString("yyyy-MM-dd HH:mm:ss").toDateLong().getDateDay().toLong(),0.0, 0))
                            chartconsumptionArray.add(ChartDateModel(time.toString("yyyy-MM-dd HH:mm:ss").toDateLong().getDateDay().toLong(),0.0, 0))
                        }
                    }
                    2 -> {
                        val startOfDay = startDateTime.toDateStr().toDate("yyyy-MM-dd HH:mm:ss").beginningOfDay
                        val end = endDateTime.getDateDay()
                        for (index in 0 until end){
                            val time = startOfDay + index.days
                            chartFuelArray.add(ChartDateModel(time.toString("yyyy-MM-dd HH:mm:ss").toDateLong().getDateDay().toLong(),0.0, 0))
                            chartRefuelArray.add(ChartDateModel(time.toString("yyyy-MM-dd HH:mm:ss").toDateLong().getDateDay().toLong(),0.0, 0))
                            chartconsumptionArray.add(ChartDateModel(time.toString("yyyy-MM-dd HH:mm:ss").toDateLong().getDateDay().toLong(),0.0, 0))
                        }
                    }
                    3 -> {
                        for (index in 1 until 13){
                            chartFuelArray.add(ChartDateModel(index.toLong(),0.0, 0))
                            chartRefuelArray.add(ChartDateModel(index.toLong(),0.0, 0))
                            chartconsumptionArray.add(ChartDateModel(index.toLong(),0.0, 0))
                        }
                    }
                }

                //油量数据
                var newFuelList = ArrayList<Fuel>()
                for (item in list){
                    item.recordTimeInterval?.let {
                        if (it >= startDateTime && it <= endDateTime){
                            newFuelList.add(item)
                        }
                    }
                }

                //油量变化数据
//                var newFuelChangedList = ArrayList<FuelChange>()
//                for (item in fuelChangedDataList){
//                    item.recordTimeInterval?.let {
//                        if (it >= startDateTime && it <= endDateTime){
//                            newFuelChangedList.add(item)
//                        }
//                    }
//                }

                var refuelArray = ArrayList<FuelChange>()
                var consumptionArray = ArrayList<FuelChange>()

//                for (item in newFuelChangedList){
//                    if (item.type == FuelChangedType.REFUEL.type){
//                        refuelArray.add(item)
//                    }else{
//                        consumptionArray.add(item)
//                    }
//                }
                //加油数据单独计算，经过过滤处理之后的加油数据
                for (item in fuelChangedDataList){
                    if (item.type == FuelChangedType.REFUEL.type){
                        refuelArray.add(item)
                    }
                }
                //油耗数据单独计算
                for (item in beforeSortFuelChangedDataList){
                    if (item.type == FuelChangedType.CONSUMPTION.type){
                        consumptionArray.add(item)
                    }
                }

                for (item in newFuelList){
                    var current: ChartDateModel? = null
                    when (segmentIndex){
                        0 -> {
                            current =  chartFuelArray.find { it.timeInterval == item.recordTimeInterval!!.getDateHour().toLong() }
                        }
                        1 -> {
                            current =  chartFuelArray.find { it.timeInterval == item.recordTimeInterval!!.getDateDay().toLong() }
                        }
                        2 -> {
                            current =  chartFuelArray.find { it.timeInterval == item.recordTimeInterval!!.getDateDay().toLong() }
                        }
                        3 -> {
                            current =  chartFuelArray.find { it.timeInterval == item.recordTimeInterval!!.getDateMonth().toLong() }
                        }
                    }
                    current?.let {
                        chartFuelArray.remove(it)
                        it.fuelData += item.capacity!!.toDouble()
                        it.totalCount ++
                        chartFuelArray.add(it)
                    }
                }

                chartFuelArray.forEach{
                    if (it.totalCount > 0){
                        it.fuelData = it.fuelData/it.totalCount
                        val fuelDataStringValue = String.format("%.1f", it.fuelData)
                        it.fuelData = fuelDataStringValue.toDouble()
                    }
                }

                for (item in refuelArray){
                    var current: ChartDateModel? = null
                    when (segmentIndex){
                        0 -> {
                            current =  chartRefuelArray.find { it.timeInterval == item.recordTimeInterval!!.getDateHour().toLong() }
                        }
                        1 -> {
                            current =  chartRefuelArray.find { it.timeInterval == item.recordTimeInterval!!.getDateDay().toLong() }
                        }
                        2 -> {
                            current =  chartRefuelArray.find { it.timeInterval == item.recordTimeInterval!!.getDateDay().toLong() }
                        }
                        3 -> {
                            current =  chartRefuelArray.find { it.timeInterval == item.recordTimeInterval!!.getDateMonth().toLong() }
                        }
                    }

                    current?.let {
                        chartRefuelArray.remove(it)
                        it.fuelData += item.fuelData!!
                        chartRefuelArray.add(it)
                    }
                }

                for (item in consumptionArray){
                    var current: ChartDateModel? = null
                    when (segmentIndex){
                        0 -> {
                            current =  chartconsumptionArray.find { it.timeInterval == item.recordTimeInterval!!.getDateHour().toLong() }
                        }
                        1 -> {
                            current =  chartconsumptionArray.find { it.timeInterval == item.recordTimeInterval!!.getDateDay().toLong() }
                        }
                        2 -> {
                            current =  chartconsumptionArray.find { it.timeInterval == item.recordTimeInterval!!.getDateDay().toLong() }
                        }
                        3 -> {
                            current =  chartconsumptionArray.find { it.timeInterval == item.recordTimeInterval!!.getDateMonth().toLong() }
                        }
                    }
                    current?.let {
                        chartconsumptionArray.remove(it)
                        it.fuelData += abs(item.fuelData!!)
                        chartconsumptionArray.add(it)
                    }

                }
                Log.i("chartData", "$newFuelList")
                Log.i("chartData", "$refuelArray")
                Log.i("chartData", "$consumptionArray")

                Log.i("chartData", "$chartFuelArray")
                Log.i("chartData", "$chartRefuelArray")
                Log.i("chartData", "$chartconsumptionArray")

                fuelChartLiveData.postValue(chartFuelArray)
                refuelChartLiveData.postValue(chartRefuelArray)
                consumptionChartLiveData.postValue(chartconsumptionArray)

            }
        }
    }


}
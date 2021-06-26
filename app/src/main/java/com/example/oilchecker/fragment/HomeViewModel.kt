package com.example.oilchecker.fragment

import android.app.Activity
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
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val database: AppDatabase

) : ViewModel() {
    private val TAG = "FuelData"
    private lateinit var connectionObservable: Observable<RxBleConnection>
    private val connectionDisposable = CompositeDisposable()
    private val disconnectTriggerSubject = PublishSubject.create<Unit>()
    private var stateDisposable: Disposable? = null


    private lateinit var mConnection: RxBleConnection
    private lateinit var bleDevice: RxBleDevice
    private val allRevData = StringBuilder()

    private val allData = StringBuilder()
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

    }

    fun doConnect(mac: String){
        bleDevice = MainApplication.rxBleClient.getBleDevice(mac!!)

        if (bleDevice.isConnected) {
            triggerDisconnect()
        } else {
            connectionObservable = bleDevice.establishConnection(false)
            connectionObservable
                //.flatMapSingle { it.discoverServices() }
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

    }

    private fun onConnectionStateChange(newState: RxBleConnection.RxBleConnectionState) {
        val text = newState.name.toString()
        Log.i(TAG, "onConnectionStateChange: $text")
//        if (text == "CONNECTED" || text == "DISCONNECTED"){
//            Log.i(TAG, "onConnectionStateChange: ---> invisible")
//            tipLiveData.postValue("disconnect")
//        }
        if (text == "DISCONNECTED"){
            tipLiveData.postValue("disconnect")
        }
    }

    private fun onConnectionFailure(throwable: Throwable) {
        Log.i(TAG, "onConnectionFailure: $throwable")
        //Toast.makeText(context, throwable.message, Toast.LENGTH_SHORT).show()
        tipLiveData.postValue("fail")


    }

    private fun onConnectionReceived(connection: RxBleConnection) {
        //Toast.makeText(context, "连接成功", Toast.LENGTH_SHORT).show()
        mConnection = connection

        Log.i(TAG, "onConnectionReceived: --->")
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
        Log.i(TAG, "onNotificationReceived:  ${bytes.toHexString()}")
        //Toast.makeText(context, bytes.toHex(), Toast.LENGTH_SHORT).show()
        Log.i(TAG, "---onNext--->>>>$bytes --->${bytes.toHex()}")

        //process fuel data
       /* val result = bytes.toHex()
        //receiveFuelData(result)
        revFuelData(result)*/
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
                    Thread.sleep(4000)
                    getFuelInfo()
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
//                    tipLiveData.postValue("rev")
                    getFuelInfoSuccess()//接收到全部数据之后，发送接收成功指令
                    processFuelData()
//                    tipLiveData.postValue("process")
                    //接收数据成功之后，最好是断开蓝牙连接！！！！！！不然我让蓝牙设备断电之后。 APP会闪退
                    triggerDisconnect()
                }
            }

        }
    }

    fun checkEndOfReceiveData() : Boolean {
        val tempData = receiveFuelData
        var dataSize = tempData.length
//        Log.i(TAG," ------> in  checkEndOfReceiveData ${dataSize}")
        var cursor = 0
        var isEndofFuelData = false
        if (dataSize > 16){
            var isContinue = true
            while (isContinue){

//                Log.i(TAG," ------> in  isContinue  ${tempData}  ${dataSize}")
                val length = tempData.substring(cursor + 2,cursor + 4).toInt(16) * 2
                val firstDataByte = tempData.substring(cursor + 14, cursor + 16)
//                Log.i(TAG," ------> in  checkEndOfReceiveData length ====== ${length} ")
//                Log.i(TAG," ------> in  checkEndOfReceiveData firstDataByte ====== ${firstDataByte} ")

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
//                                dataSize = dataSize - length - 18
                                cursor += length + 18
//                                Log.i(TAG," ------> in  checkEndOfReceiveData cursor ====== ${cursor} ")
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


    private fun onNotificationSetupFailure(throwable: Throwable){

        Log.i(TAG, "onNotificationSetupFailure: $throwable")
       // Toast.makeText(context, throwable.message, Toast.LENGTH_SHORT).show()
        tipLiveData.postValue("fail")


    }


    private fun triggerDisconnect(){
        tipLiveData.postValue("disconnect")
        connectionDisposable?.dispose()
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
                    var new = StringBuilder()
                    var realHeight: Int
                    var v: Double
                    var i = 0
                    var j = 0
                    var k = 0
                    var record: Double

                    val fuelConsumeList = ArrayList<String>()
                    val refuelList = ArrayList<String>()
                    var maxFuel = 0.0
                    var average = String()

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
                                    //过滤
                                }else{
                                    new.append(fuelData.substring(2,4))
                                    new.append(fuelData.substring(0,2))
                                    realHeight = new.toString().toInt(16)
                                    new.clear()
                                    Log.i(TAG, "processFuelData: fueldata real height-> $realHeight")
                                    v = (realHeight.toDouble()*length * width)/1000000
                                    realHeight *= length * width

                                    Log.i(TAG, "processFuelData: fueldata real capavity->$v  --> ${v.toString()}")

                                    fuelList.add(i,v.toString())
                                    if(i > 0){
                                        record = (fuelList[i-1].toDouble() - v)
                                        if(record > 0){
                                            //fuel consume
                                            if (maxFuel < record){
                                                maxFuel = record
                                            }
                                            average = String.format("%.2f", record)
                                            fuelConsumeList.add(j, String.format("%.2f", record))
                                            j++
                                        }
                                        // refuel next-current
                                        record = (v - fuelList[i-1].toDouble())

                                        if(record > 0) {
                                            //refuel
                                            refuelList.add(k, String.format("%.2f", record))
                                            k++
                                        }
                                    }

                                    i++
                                }
                            }
                            cursor += len - 2
                            cursor += 4
                            Log.i(TAG,"processFuelData cursor --->${cursor}")
                        }
                    }
                    Log.i(TAG,"processFuelData fuelList All data --->${fuelList}")

                    Log.i(TAG, "onNotificationReceived: max-> $maxFuel fuel ->${fuelList.size} fuel consume ${fuelConsumeList.size}  refuel ${refuelList.size}")

                    setAverageOil(average)
                    if (maxFuel > 5.0){
                        database.deviceDao().insertStatusAndAverage(id, "异常", average)
                        fuelStatusLiveData.postValue("异常")
                    }else {
                        database.deviceDao().insertStatusAndAverage(id, "正常", average)
                        fuelStatusLiveData.postValue("正常")
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
                    for (i in 0 until fuelConsumeList.size){
                        refuelDataList.add(Refuel(i,id,refuelList[i]))
                    }
                    database.refuelDataDao().insertRefuelData(refuelDataList)
                    tipLiveData.postValue("process")
                }

            }
        }
    }

    fun receiveFuelData(result :String){
//        tipLiveData.postValue("request")

        if (result.length > 14){
            //02FF018585008201

            val lenStr = result.substring(2,4)
            var len = 0
            val cmd = result.substring(12,14)
            val num = result.substring(14,16)
            if (cmd == "82") {
                // result.substring(14,16)  ->01
                Log.i(TAG, "receiveFuelData: ${result.substring(14,16)} ")
                allData.append(result.substring(16, result.length-4))
                len = lenStr.toInt(16)
                if (result.substring(14,16) == "FF"){
                    processFuelData(allData.toString())
                    allData.clear()
                }else {
                    processFuelData(allData.toString())
                }

            }
        }
    }

    fun processFuelData(data: String){

        viewModelScope.launch {
            async(Dispatchers.IO) {
                val id = getIdentify() ?: return@async
                val device = database.deviceDao().getDevice(id)
                Log.i(TAG, "processFuelData: $device")
                if (device != null){
                    val length = device.length.toInt()
                    val width = device.width.toInt()

                    val len = data.length
                    var i = 0
                    var index = 0
                    var j = 0
                    var k = 0
                    var h: Int
                    var record: Double
                    var v: Double
                    var str: String
                    var n = StringBuilder()
                    val fuelList = ArrayList<String>()
                    val fuelConsumeList = ArrayList<String>()
                    val refuelList = ArrayList<String>()
                    var average = String()
                    var maxFuel = 0.0


                    while (i < (len-4)/4){
                        //height --> FD
                        // h = data.substring(i*4, i*4+4).toInt(16)
                        str =data.substring(i*4, i*4+4)
                        if (str == "FDFF" || str == "FEFF"){
                            i++
                            continue
                        }

                        Log.i(TAG, "processFuelData: fueldata str-> $str")

                        n.append(str.substring(2,4))
                        n.append(str.substring(0,2))
                        h = n.toString().toInt(16)
                        n.clear()
                        //Length*Width*Height
                        //h 0001

                        Log.i(TAG, "processFuelData: fueldata real height-> $h")
                        v = (h.toDouble()*length * width)/1000000

                        h *= length * width
                        Log.i(TAG, "processFuelData: fueldata real capavity->$v  --> ${v.toString()}")

                        fuelList.add(index,v.toString())
                        if(index > 0){
                            //fuel consume pre-current
                            record = (fuelList[index-1].toDouble() - v)
                            if(record > 0){
                                //fuel consume
                                fuelConsumeList.add(j, String.format("%.2f", record))
                                average = String.format("%.2f", record)
                                if (maxFuel < record){
                                    maxFuel = record
                                }
                                j++
                            }
                            // refuel next-current
                            record = (v - fuelList[index-1].toDouble())

                            if(record > 0) {
                                //refuel
                                refuelList.add(k, String.format("%.2f", record))
                                k++
                            }
                        }
                        index++
                        i++
                    }
                    Log.i(TAG, "onNotificationReceived: fuel ->${fuelList.size} fuel consume ${fuelConsumeList.size}  refuel ${refuelList.size}")

                    setAverageOil(average)
                    averageFuelConsumeLiveData.postValue(average)
                    fuelLiveData.postValue(fuelList)

                    if (maxFuel > 5.0){
                        database.deviceDao().insertStatusAndAverage(id, "异常", average)
                        fuelStatusLiveData.postValue("异常")
                    }else {
                        database.deviceDao().insertStatusAndAverage(id, "正常", average)
                        fuelStatusLiveData.postValue("正常")
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
                    for (i in 0 until fuelConsumeList.size){
                        refuelDataList.add(Refuel(i,id,refuelList[i]))
                    }
                    database.refuelDataDao().insertRefuelData(refuelDataList)
                    tipLiveData.postValue("process")
                }
            }
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
            tipLiveData.postValue("rev")
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
        tipLiveData.postValue("fail")
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
                    fuelStatusLiveData.postValue(device.status)
                    averageFuelConsumeLiveData.postValue(device?.average)
                }
            }
        }
    }

}
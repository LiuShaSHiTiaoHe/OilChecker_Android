package com.example.oilchecker.fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import com.example.oilchecker.base.MainApplication
import com.example.oilchecker.databinding.BleDeviceFragmentBinding
import com.example.oilchecker.databinding.FragmentDeviceInfoBinding
import com.example.oilchecker.util.Contants
import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.RxBleDevice
import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import java.util.*
import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.text.Editable
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.example.oilchecker.R
import com.example.oilchecker.data.entity.Device
import com.polidea.rxandroidble2.NotificationSetupMode
import com.polidea.rxandroidble2.internal.RxBleLog
import com.polidea.rxandroidble2.internal.logger.LoggerUtil
import com.polidea.rxandroidble2.samplekotlin.util.*
import com.polidea.rxandroidble2.samplekotlin.util.isConnected
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.internal.managers.FragmentComponentManager
import io.reactivex.functions.Function
import io.reactivex.observers.DisposableObserver
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import net.akaish.ikey.hkb.IKeyHexKeyboard
import java.lang.StringBuilder
import kotlin.concurrent.schedule

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [DeviceInfoFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
@AndroidEntryPoint
class DeviceInfoFragment : Fragment(), View.OnClickListener{
    // TODO: Rename and change types of parameters
    private val TAG = "DeviceInfoFragment"
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var viewModel: BleDeviceViewModel
    private lateinit var deviceFragmentBinding: FragmentDeviceInfoBinding
    private val args: DeviceInfoFragmentArgs by navArgs()
    private lateinit var characteristicUuid: UUID

    private lateinit var bleDevice: RxBleDevice

    private lateinit var connectionObservable: Observable<RxBleConnection>
    private val disconnectTriggerSubject = PublishSubject.create<Unit>()

    private val connectionDisposable = CompositeDisposable()

    private val bytesToWrite = ByteArray(1024) // a kilobyte array
    private lateinit var mConnection: RxBleConnection
    private lateinit var macAddress: String
    private lateinit var hexKeyboard: IKeyHexKeyboard


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentDeviceInfoBinding.bind(view)
        deviceFragmentBinding = binding
        deviceFragmentBinding.llBack.setOnClickListener(this)
        deviceFragmentBinding.btnSave.setOnClickListener(this)
        deviceFragmentBinding.ivBack.setOnClickListener(this)
        viewModel = ViewModelProvider(this).get(BleDeviceViewModel::class.java)

        Log.i(TAG, "onViewCreated: ${args.mac}")
        characteristicUuid = Contants.NOTIFY_UUID

        macAddress = args.mac
        bleDevice = MainApplication.rxBleClient.getBleDevice(macAddress!!)
        Timer().schedule(1000){
            doConnect()
        }
        viewModel.tipLiveData.observe(viewLifecycleOwner, {
            Log.i(TAG, "onViewCreated: tipLiveData -->$it")
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        })
        viewModel.deviceLiveData.observe(viewLifecycleOwner, {
            deviceFragmentBinding.etCarNum.text = Editable.Factory.getInstance().newEditable(it)
            deviceFragmentBinding.btnSave.text = getString(R.string.Edit)
            allowEdit(false)
        })


    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_device_info, container, false)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment DeviceInfoFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            DeviceInfoFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                    Log.i(TAG, "newInstance: $param1   --> $param2")

                }
            }
    }

    fun doConnect(){

        if (bleDevice.isConnected) {
            triggerDisconnect()
        } else {
            connectionObservable = bleDevice.establishConnection(false)
            connectionObservable
                //.flatMapSingle { it.discoverServices() }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    mConnection = it
                   // test()
                    onConnectionReceived(it)
                },{
                    onConnectionFailure(it)
                })
                .let { connectionDisposable.add(it) }
        }

    }

    private fun onConnectionFailure(throwable: Throwable) {
        Log.i(TAG, "onConnectionFailure: $throwable")
        Toast.makeText(context, getString(R.string.connect_fail), Toast.LENGTH_SHORT).show()
        deviceFragmentBinding.btnSave.findNavController().navigateUp()
    }

    private fun onConnectionReceived(connection: RxBleConnection) {
        Toast.makeText(context, getString(R.string.connect_successful), Toast.LENGTH_SHORT).show()

        Log.i(TAG, "onConnectionReceived: --->")
        if(bleDevice.isConnected) {
            Log.i(TAG, "doConnect: isConnected-->")
            mConnection.setupNotification(Contants.NOTIFY_UUID,NotificationSetupMode.QUICK_SETUP)
                .doOnNext{
                   // connection.writeCharacteristic(Contants.WRITE_UUID, inputBytes)
                    getDeviceInfo()
                    val activity = FragmentComponentManager.findActivity(view?.context) as Activity
                    activity.runOnUiThread {
                        Toast.makeText(context, getString(R.string.notify_set_up), Toast.LENGTH_SHORT).show()
                    }
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
        val result = bytes.toHex()
        if (result.length > 14){
            if (result.substring(12,14) == "86"){
                val identify = result.substring(14,18)
                val length = result.substring(18,22).toInt(16).toString()
                val width = result.substring(22,26).toInt(16).toString()
                val height = result.substring(26,30).toInt(16).toString()
                val compare = result.substring(30,34).toInt(16).toString()

                deviceFragmentBinding.etIdentify.text = Editable.Factory.getInstance().newEditable(identify)
                deviceFragmentBinding.etLength.text = Editable.Factory.getInstance().newEditable(length)
                deviceFragmentBinding.etWidth.text = Editable.Factory.getInstance().newEditable(width)
                deviceFragmentBinding.etHeight.text = Editable.Factory.getInstance().newEditable(height)
                deviceFragmentBinding.etCompare.text = Editable.Factory.getInstance().newEditable(compare)
                viewModel.getDeviceById(identify)
                Toast.makeText(context, R.string.request_successfully, Toast.LENGTH_SHORT).show()
            }else if (result.substring(12,14) == "84"){
                if (result.substring(14,16) == "00"){
                    Log.i(TAG, "onNotificationReceived: set successful")
                    //add mac address
                    saveInfo()
                    triggerDisconnect()
                    Toast.makeText(context, R.string.set_dev_successfully, Toast.LENGTH_SHORT).show()
                    val direction = DeviceInfoFragmentDirections.actionDeviceInfoFragmentToHomeFragment()
                    deviceFragmentBinding.btnSave.findNavController().navigate(direction)
                }else{
                    Log.i(TAG, "onNotificationReceived: set fail")
                    Toast.makeText(context, R.string.set_dev_fail, Toast.LENGTH_SHORT).show()
                }

            }

        }
    }


    private fun onNotificationSetupFailure(throwable: Throwable){

        Log.i(TAG, "onNotificationSetupFailure: $throwable")
        Toast.makeText(context, throwable.message, Toast.LENGTH_SHORT).show()

    }

    private  fun  allowEdit(allow: Boolean){
        deviceFragmentBinding.etCarNum.setEnabled(allow)
        deviceFragmentBinding.etIdentify.setEnabled(allow)
        deviceFragmentBinding.etLength.setEnabled(allow)
        deviceFragmentBinding.etWidth.setEnabled(allow)
        deviceFragmentBinding.etHeight.setEnabled(allow)
        deviceFragmentBinding.etCompare.setEnabled(allow)
    }

    private fun saveInfo(){
        //add mac address
        val identify = deviceFragmentBinding.etIdentify.text.toString()
        val length = deviceFragmentBinding.etLength.text.toString()
        val width = deviceFragmentBinding.etWidth.text.toString()
        val height = deviceFragmentBinding.etHeight.text.toString()
        val compare = deviceFragmentBinding.etCompare.text.toString()

        Log.i(TAG, "onClick: save identify $identify   --->${"001A".toInt(16)}")
        Log.i(TAG, "onClick: Save Identify $identify   --->${identify.toInt(16)}")
        lifecycleScope.launch{
            val id = identify.toInt(16)
            var device: Device = Device(id ,identify, deviceFragmentBinding.etCarNum.text.toString(),length, width, height, compare, "","",macAddress)
            viewModel.addNewDevice(device)
        }
    }

    private fun triggerDisconnect(){
        connectionDisposable?.dispose()
        disconnectTriggerSubject.onNext(Unit)
    }

    override fun onPause() {
        super.onPause()
       // triggerDisconnect()
        connectionDisposable.clear()
    }

    override fun onClick(v: View?) {
        when(v?.id){
            R.id.btn_save -> {
                if (deviceFragmentBinding.btnSave.text == getString(R.string.Edit)){
                    AlertDialog.Builder(context).setTitle(getString(R.string.Tips)).setMessage(getString(R.string.ChangeDeviceInfoTips))
                        .setPositiveButton(getString(R.string.Confirm),DialogInterface.OnClickListener{ _, _ ->
                            allowEdit(true)
                            deviceFragmentBinding.btnSave.text = getString(R.string.save)
                        })
                        .setNegativeButton(getString(R.string.Cancle),null)
                        .create()
                        .show()

                }else{
                    lifecycleScope.launch {
                        //add mac address
                        val carNum = deviceFragmentBinding.etCarNum.text.toString()
                        val identify = deviceFragmentBinding.etIdentify.text.toString()
                        val length = deviceFragmentBinding.etLength.text.toString()
                        val width = deviceFragmentBinding.etWidth.text.toString()
                        val height = deviceFragmentBinding.etHeight.text.toString()
                        val compare = deviceFragmentBinding.etCompare.text.toString()
//                        val compare = "FFFF"

                        if (carNum.isEmpty() || identify.isEmpty() || length.isEmpty() || width.isEmpty() || height.isEmpty() || compare.isEmpty()){
                            Toast.makeText(context, getString(R.string.enter_complete_data), Toast.LENGTH_SHORT).show()
                        }else {
                            //todo save data to db in notify success
                            //todo already connect
                            setDeviceInfo(carNum,identify,length, width, height, compare)
                        }
                    }
                }

            }
            R.id.ll_back -> {
                v.findNavController().navigateUp()
            }
            R.id.iv_back -> {
                v.findNavController().navigateUp()
            }

        }
    }

    private fun getDeviceInfo() {

        var dataLen = 1.toString(16)   //数据长度 1字节
        val len = (256 - 1).toString(16) //数据长补数 1字节
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

        mConnection.writeCharacteristic(Contants.WRITE_UUID,inputBytes.hex2byte())
            .map { Log.i(TAG, "getDeviceInfo: write --> ${it.toHex()}") }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ onWriteSuccess() }, { onWriteFailure(it) })
            .let { connectionDisposable.add(it) }

        /*if (bleDevice.isConnected) {
            Log.i(TAG, "getDeviceInfo: isConnected --> write")
            connectionObservable
                .firstOrError()
                .flatMap { it.writeCharacteristic(Contants.WRITE_UUID, inputBytes) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ onWriteSuccess() }, { onWriteFailure(it) })
                .let { connectionDisposable.add(it) }
        }else {
            Log.i(TAG, "getDeviceInfo: disConnected --> write")

            mConnection.writeCharacteristic(Contants.WRITE_UUID,inputBytes)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ onWriteSuccess() }, { onWriteFailure(it) })
                .let { connectionDisposable.add(it) }
        }*/
    }

    private fun setDeviceInfo(carNum: String, identify: String, length: String, width: String, height: String, compare: String) {

        Log.i(TAG, "setDeviceInfo: $carNum $identify $length $width $height $compare")
        Log.i(TAG, "setDeviceInfo: length -> ${length.toString().toInt().toString(16)}")
        var dataLen = 10.toString(16)   //数据长度 1字节
        val len = (256 - 10).toString(16) //数据长补数 1字节
        Log.i(TAG, "onClick:$dataLen ->  $len")

        if (dataLen.length == 1){
            dataLen = "0$dataLen"
        }

        val data = StringBuilder()
        data.append(dataLen)
        data.append(len)
        data.append("00")
        data.append("00")
        data.append("01")
        data.append("83")
        data.append(identify) //device id
        data.append(length.toDoubleByte()) //lenght
        data.append(width.toDoubleByte())  //width
        data.append(height.toDoubleByte())  //height
        data.append(compare.toDoubleByte())
//        data.append(compare)

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
        result.append(data)
        result.append(sumLen)
        result.append("03")

        val write = result.toString()
        Log.i(TAG, "-->onClick: $write")

        val inputBytes: ByteArray = write.toByteArray()

        Log.i(TAG, "setDeviceInfo: connect state -> ${bleDevice.isConnected}")

        if(bleDevice.isConnected) {
            mConnection.writeCharacteristic(Contants.WRITE_UUID,inputBytes.hex2byte())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ onWriteSuccess() }, { onWriteFailure(it) })
                .let { connectionDisposable.add(it) }
        }else {
            deviceFragmentBinding.btnSave.findNavController().navigateUp()
        }
    }

    private fun onWriteSuccess(){
        Log.i(TAG, "onWriteSuccess: ")
    }

    private fun onWriteFailure(throwable: Throwable){
        Log.i(TAG, "onWriteFailure: ${throwable.message}")
        
    }

    private var co: Disposable = CompositeDisposable()

    private fun test(){


        val write = "0201ff00000185116b03"
        co = mConnection.setupNotification(Contants.test)
                .flatMap<ByteArray>(Function<Observable<ByteArray>, ObservableSource<out ByteArray>> { ob: Observable<ByteArray>? ->
                    Observable.merge(
                        mConnection.writeCharacteristic(
                            Contants.test,
                            write.toByteArray().hex2byte()
                        ).toObservable(),
                        ob // observing log data notifications
                    )
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableObserver<ByteArray?>() {
                    override fun onNext(bytes: ByteArray) {
                        Log.i(TAG, "  -onNext--->>>>$bytes --->${bytes.toHex()}")

                        Log.i(TAG, "  -onNext--->>>>" + bytes.toHexString())
                    }

                    override fun onError(e: Throwable) {
                        Log.i(TAG, "onError: update $e")
                        e.printStackTrace()
                    }

                    override fun onComplete() {
                        Log.i(TAG, "onComplete: ???!! ")
                    }
                })
        connectionDisposable.add(co)
    }


}
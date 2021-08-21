package com.example.oilchecker.fragment

import android.app.Activity
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.findNavController
import com.example.oilchecker.MainActivity
import com.example.oilchecker.R
import com.example.oilchecker.ScanResultsAdapter
import com.example.oilchecker.base.MainApplication
import com.example.oilchecker.databinding.BleDeviceFragmentBinding
import com.example.oilchecker.util.Contants
import com.polidea.rxandroidble2.exceptions.BleScanException
import com.polidea.rxandroidble2.samplekotlin.util.isLocationPermissionGranted
import com.polidea.rxandroidble2.samplekotlin.util.requestLocationPermission
import com.polidea.rxandroidble2.scan.ScanFilter
import com.polidea.rxandroidble2.scan.ScanResult
import com.polidea.rxandroidble2.scan.ScanSettings
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.internal.managers.FragmentComponentManager
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import java.lang.Thread.sleep
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.schedule

@AndroidEntryPoint
class BleDeviceFragment : Fragment(), View.OnClickListener {
    private val TAG = "BleDeviceFragment"
    private val rxBleClient = MainApplication.rxBleClient
    private var scanDisposable: Disposable?= null
    private var hasClickedScan = false
    private var isScanning: Boolean= scanDisposable != null
    private val resultsAdapter = ScanResultsAdapter {
        scanDisposable?.dispose()
        scanDisposable = null
        val mac = it.bleDevice.macAddress
        Log.i(TAG, "scanResult: onclick --> ${it.bleDevice.macAddress} ")
        val direction = BleDeviceFragmentDirections.actionBleDeviceFragmentToDeviceInfoFragment(mac)
        bleDeviceFragmentBinding.scanResults.findNavController().navigate(direction)
    }

    companion object {
        fun newInstance() = BleDeviceFragment()
    }

    private lateinit var viewModel: BleDeviceViewModel
    private lateinit var bleDeviceFragmentBinding: BleDeviceFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.ble_device_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = BleDeviceFragmentBinding.bind(view)
        bleDeviceFragmentBinding = binding
        bleDeviceFragmentBinding.llBack.setOnClickListener(this)
        bleDeviceFragmentBinding.ivBack.setOnClickListener(this)

    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(BleDeviceViewModel::class.java)
        // TODO: Use the ViewModel
        configureResultList()

    }

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "onResume: ")
        scan()

    }

    override fun onPause() {
        super.onPause()
        Log.i(TAG, "onPause: ")
        scanDisposable?.dispose()
    }

    private fun configureResultList() {

        with(bleDeviceFragmentBinding.scanResults) {
            setHasFixedSize(true)
            itemAnimator = null
            adapter = resultsAdapter
        }
    }

    override fun onClick(v: View?) {
        when(v?.id){
            R.id.ll_back -> {
                val direction = BleDeviceFragmentDirections.actionBleDeviceFragmentToSettingFragment()
                v.findNavController().navigate(direction)
            }
        }
    }

    private fun scan(){
        if(isScanning){
            scanDisposable?.dispose()
            scanDisposable = null
        }else {
            if (rxBleClient.isScanRuntimePermissionGranted){
                scanBleDevices()
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally { dispose() }
                    .subscribe({ resultsAdapter.addScanResult(it) }, { onScanFailure(it)})
                    .let { scanDisposable = it }
            }else {
                hasClickedScan = true
                val activity = FragmentComponentManager.findActivity(view?.context) as Activity
                activity.requestLocationPermission(rxBleClient)
            }
        }
    }

    private fun scanBleDevices(): Observable<ScanResult> {
        return rxBleClient.scanBleDevices(
            ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .build(),
            ScanFilter.Builder()
                .build()
        )
    }


    private fun onScanFailure(throwable: Throwable) {
        Log.i(TAG, "scan fail")
        scanDisposable?.dispose()
        if (throwable is BleScanException) {
            handleBleScanException(throwable)
        }
        viewModel.recordMalfuntion(requireContext().getString(R.string.connect_fail))
    }

    private fun handleBleScanException(bleScanException: BleScanException) {
        val text: String
        text = when (bleScanException.reason) {
            BleScanException.BLUETOOTH_NOT_AVAILABLE -> getString(R.string.ble_not_available) //"Bluetooth is not available"
            BleScanException.BLUETOOTH_DISABLED -> getString(R.string.open_ble)
            BleScanException.LOCATION_PERMISSION_MISSING ->                 //text = "On Android 6.0 location permission is required. Implement Runtime Permissions";
               // "定位权限未打开"
                getString(R.string.location_permission_missing)
            BleScanException.LOCATION_SERVICES_DISABLED ->                 //text = "Location services needs to be enabled on Android 6.0";
                getString(R.string.location_disabled) //"请打开定位服务"
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
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    }

    private fun secondsTill(retryDateSuggestion: Date): Long {
        return TimeUnit.MILLISECONDS.toSeconds(retryDateSuggestion.time - System.currentTimeMillis())
    }

    private fun dispose() {

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (isLocationPermissionGranted(requestCode, grantResults) && hasClickedScan) {
            hasClickedScan = false
            scanBleDevices()
        }
    }


}
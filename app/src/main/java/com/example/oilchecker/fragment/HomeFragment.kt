package com.example.oilchecker.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.example.oilchecker.R
import com.example.oilchecker.databinding.HomeFragmentBinding
import com.example.oilchecker.util.CustomMarkerView
import com.example.oilchecker.util.clickWithTrigger
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*

@AndroidEntryPoint
class HomeFragment : Fragment(), View.OnClickListener{
    private val TAG = "HomeFragment"

    companion object {
        fun newInstance() = HomeFragment()
    }
    private lateinit var binding: HomeFragmentBinding
    private lateinit var viewModel: HomeViewModel
    private var currentDevice: String = ""
    private var type: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.home_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = HomeFragmentBinding.bind(view)
        handleToast()
        binding.tvCarNum.setOnClickListener(this)

        binding.llSync.clickWithTrigger {
            val mac = HomeViewModel.getMac()
            binding.progressBar.visibility = View.VISIBLE
            lifecycleScope.launch {
                if (mac != null && mac.isNotEmpty()) {
                    //get fuel history data
                    viewModel.doConnect(mac)
                } else {
                    binding.progressBar.visibility = View.INVISIBLE
                    Toast.makeText(context, R.string.add_device, Toast.LENGTH_SHORT).show()
                }
            }
        }

        val segmentIndex = HomeViewModel.getSegmentIndex()
        type = segmentIndex.toString()

        binding.segmented {
            initialCheckedIndex = segmentIndex
            onSegmentChecked { segment ->
                val segmentedTitleString = segment.text.toString()
                when(segmentedTitleString){
                    getString(R.string.day) -> type = "0"
                    getString(R.string.week) -> type = "1"
                    getString(R.string.month) -> type = "2"
                    getString(R.string.year) -> type = "3"
                }
                HomeViewModel.setSegmentIndex(type.toInt())
            }
            onSegmentRechecked { segment -> Log.i(TAG, "onViewCreated: rechecked") }
            onSegmentUnchecked { segment -> Log.i(TAG, "onViewCreated: unchecked")}
        }
        viewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

        viewModel.fuelLiveData.observe(viewLifecycleOwner, Observer {
            Log.i(TAG, "createChart: list ${it.size}")
            Log.i(TAG, "createChart: list type ----->  ${type}")
            val size = it.size
        })
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        // TODO: Use the ViewModel

    }

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "onResume:")
        currentDevice = HomeViewModel.getDevice().toString()
        if(currentDevice.isNotEmpty()){
            binding.tvCarNum.text = currentDevice
            createChart()
            //fragmentHomeFragmentBinding.tvCarNum.setTextColor(resources.getColor(R.color.black))
        }
    }

    override fun onStop() {
        super.onStop()
        binding.progressBar.visibility = View.INVISIBLE
    }
    override fun onClick(v: View?) {
        when(v?.id){
            R.id.tv_car_num -> {
                val direction = if(currentDevice.isNotEmpty()){
                    HomeFragmentDirections.actionHomeFragmentToSelectCarFragment()
                }else {
                    HomeFragmentDirections.actionHomeFragmentToBleDeviceFragment()
                }
                v.findNavController().navigate(direction)
            }

        }
    }

    fun createChart(){
        viewModel.getFuelData()
    }



    fun handleToast(){
        viewModel.tipLiveData.observe(viewLifecycleOwner, {
            when(it) {
                "request" -> {
                    Toast.makeText(context, R.string.request_successfully, Toast.LENGTH_SHORT).show()
                }
                "requestFuelData" -> {
                    binding.progressBar.visibility = View.VISIBLE
                    Toast.makeText(context, R.string.updating_data, Toast.LENGTH_SHORT).show()
                }
                "rev" -> {
                    binding.progressBar.visibility = View.VISIBLE
                    Toast.makeText(context, R.string.parsing_data, Toast.LENGTH_SHORT).show()
                }
                "process" -> {
                    binding.progressBar.visibility = View.INVISIBLE
                    Toast.makeText(context, R.string.processing_completed, Toast.LENGTH_SHORT).show()
                }
                "fail" -> {
                    binding.progressBar.visibility = View.INVISIBLE
                    Toast.makeText(context, R.string.sync_fail, Toast.LENGTH_SHORT).show()
                }
                "connectionfail" -> {
                    binding.progressBar.visibility = View.INVISIBLE
                    Toast.makeText(context, R.string.connect_fail, Toast.LENGTH_SHORT).show()
                }
                "writeDatafail" -> {
                    binding.progressBar.visibility = View.INVISIBLE
                    Toast.makeText(context, R.string.sync_fail, Toast.LENGTH_SHORT).show()
                }
                "disconnect" -> {
                    binding.progressBar.visibility = View.INVISIBLE
                    Toast.makeText(context, R.string.disconnect, Toast.LENGTH_SHORT).show()
                }
            }
        })
    }
}
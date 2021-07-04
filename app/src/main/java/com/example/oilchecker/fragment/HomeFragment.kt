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
    private val lineEntry = ArrayList<Entry>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.home_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = HomeFragmentBinding.bind(view)
        binding.tvCarNum.setOnClickListener(this)

        var customMarkerView =
            CustomMarkerView(
                context,
                R.layout.custom_maker_view_layout
            )
        customMarkerView.chartView = binding.lineChart
        binding.lineChart.marker = customMarkerView

        binding.llSync.clickWithTrigger {
            val mac = HomeViewModel.getMac()
            binding.progressBar.visibility = View.VISIBLE
            lifecycleScope.launch {
                if (mac != null && mac.isNotEmpty()) {
                    //get fuel history data
                    Log.i(TAG, "onClick: -->connect $mac")
                    viewModel.doConnect(mac)
                } else {
                    binding.progressBar.visibility = View.INVISIBLE
                    Toast.makeText(context, R.string.add_device, Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.tvRealStatus.text = getString(R.string.unknown)
        val segmentIndex = HomeViewModel.getSegmentIndex()
        if (segmentIndex == 0){
            type = getString(R.string.day)
        }else{
            type = getString(R.string.week)
        }
        binding.segmented {
            initialCheckedIndex = segmentIndex
            onSegmentChecked { segment ->
                type = segment.text.toString()
                if (type == getString(R.string.day)){
                    HomeViewModel.setSegmentIndex(0)
                }else{
                    HomeViewModel.setSegmentIndex(1)
                }
                createChart()
                Log.i(TAG, "onViewCreated: checked ${segment.text} $initialCheckedIndex") }
            onSegmentRechecked { segment -> Log.i(TAG, "onViewCreated: rechecked") }
            onSegmentUnchecked { segment -> Log.i(TAG, "onViewCreated: unchecked")}
        }
        viewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

        viewModel.averageFuelConsumeLiveData.observe(viewLifecycleOwner, {
            if (it.isEmpty()){
                binding.tvAverage.text = "0.0 L"
            }else {
                binding.tvAverage.text = "$it L"
            }
        })

        viewModel.fuelStatusLiveData.observe(viewLifecycleOwner, {
            val txt: String
            if(it.equals("异常")){
                txt = getString(R.string.exception)
                binding.tvRealStatus.setTextColor(resources.getColor(R.color.red))
            }else if(it.equals("正常")){
                txt = getString(R.string.normal)
                binding.tvRealStatus.setTextColor(resources.getColor(R.color.theme))
            }else{
                txt = getString(R.string.unknown)
                binding.tvRealStatus.setTextColor(resources.getColor(R.color.theme))
                binding.tvAverage.text = "0.0 L"
            }
            binding.tvRealStatus.text = txt
        })

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
        viewModel.fuelLiveData.observe(viewLifecycleOwner, Observer {
            lineEntry.clear()
            Log.i(TAG, "createChart: list ${it.size}")
            Log.i(TAG, "createChart: list type ----->  ${type}")
            val size = it.size
            if (size > 0){
                binding.placeholder1.visibility = View.GONE
                var index = 0
                var fuelDatasList = ArrayList<String>()

                if (type == getString(R.string.day)){
                    if (size >=  30) {
                        index = size - 30
                        for (i in index until size){
                            fuelDatasList.add(it[i])
                        }
                    }else{
                        for (i in 0 until size){
                            fuelDatasList.add(it[i])
                        }
                    }
                }else {
                    if (size >= 60) {
                        index = size - 60
                        for (i in index until size){
                            fuelDatasList.add(it[i])
                        }
                    }else{
                        for (i in 0 until size){
                            fuelDatasList.add(it[i])
                        }
                    }
                }
                for (i in 0 until fuelDatasList.size - 1){
                    if (i < fuelDatasList.size - 2) {
                        val data = fuelDatasList[i]
                        val nextData = fuelDatasList[i+1]
                        val thesholds = HomeViewModel.getThreshold()
                        if (data.toFloat() - nextData.toFloat() > thesholds.toFloat()){
                            Log.i(TAG,"Data ${data}, nextData ${nextData}")
                            lineEntry.add(Entry(i.toFloat(),data.toFloat(), getResources().getDrawable(R.drawable.icon_warning)))
                        }else{
                            lineEntry.add(Entry(i.toFloat(), fuelDatasList[i].toFloat()))
                        }
                    }else{
                        lineEntry.add(Entry(i.toFloat(), fuelDatasList[i].toFloat()))
                    }
                }

                val linedataset = LineDataSet(lineEntry, getString(R.string.fuel))
                linedataset.color = resources.getColor(R.color.theme)
                linedataset.setDrawCircles(true)
                linedataset.setCircleColor(resources.getColor(R.color.theme))
                linedataset.setDrawFilled(true)
                linedataset.fillColor = resources.getColor(R.color.theme)
                linedataset.fillAlpha = 80
                linedataset.valueTextSize = 0f
//                linedataset.setDrawValues(true)


                val data = LineData(linedataset)
                binding.lineChart.description.isEnabled = false
                binding.lineChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
                binding.lineChart.axisRight.isEnabled = false
                binding.lineChart.data = data
                binding.lineChart.setBackgroundColor(resources.getColor(R.color.white))
                binding.lineChart.animateXY(30, 30)

            }

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
//            R.id.iv_sync, R.id.tv_sync ->{
//                val mac = HomeViewModel.getMac()
//                fragmentHomeFragmentBinding.progressBar.visibility = View.VISIBLE
//                lifecycleScope.launch {
//                    if (mac != null && mac.isNotEmpty()) {
//                        //get fuel history data
//                        Log.i(TAG, "onClick: -->connect $mac")
//                        viewModel.doConnect(mac)
//                    } else {
//                        Toast.makeText(context, R.string.add_device, Toast.LENGTH_SHORT).show()
//                    }
//                }
//            }
        }
    }

    fun createChart(){
        viewModel.getFuelData()
    }

    override fun onStop() {
        super.onStop()
        binding.progressBar.visibility = View.INVISIBLE
    }
}
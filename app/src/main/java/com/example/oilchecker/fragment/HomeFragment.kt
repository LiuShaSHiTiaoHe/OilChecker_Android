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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.oilchecker.R
import com.example.oilchecker.adapter.HomeViewDataListAdapter
import com.example.oilchecker.data.entity.FuelChange
import com.example.oilchecker.databinding.HomeFragmentBinding
import com.example.oilchecker.util.*
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import dagger.hilt.android.AndroidEntryPoint
import khronos.*
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
        viewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

        binding.tvCarNum.setOnClickListener(this)
        binding.leftButton.setOnClickListener(this)
        binding.rightButton.setOnClickListener(this)
        binding.llSync.clickWithTrigger {
            val mac = UserPreference.getMac()
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

        val listAdapter = HomeViewDataListAdapter()
        binding.rvFuel.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = listAdapter
            itemAnimator = null
        }

        handleToast()
        val segmentIndex = UserPreference.getSegmentIndex()
        type = segmentIndex.toString()

        binding.segmented {
            initialCheckedIndex = segmentIndex
            onSegmentChecked { segment ->
                val segmentedTitleString = segment.text.toString()
                UserPreference.setDateOffset(0)
                when(segmentedTitleString){
                    getString(R.string.day) -> type = "0"
                    getString(R.string.week) -> type = "1"
                    getString(R.string.month) -> type = "2"
                    getString(R.string.year) -> type = "3"
                }
                UserPreference.setSegmentIndex(type.toInt())
                updateTimeRang()
                viewModel.getFuelData()
            }
            onSegmentRechecked {}
            onSegmentUnchecked {}
        }


        viewModel.fuelLiveData.observe(viewLifecycleOwner, Observer {
            Log.i(TAG, "fuelLiveData: list ${it.size}")
            Log.i(TAG, "fuelLiveData: list type ----->  ${type}")
            val size = it.size


        })

        viewModel.fuelChangedLiveData.observe(viewLifecycleOwner, Observer {
            Log.i(TAG, "fuelChangedLiveData: Size ${it.size}")
            var refuelArray = ArrayList<FuelChange>()
            var consumptionArray = ArrayList<FuelChange>()
            var totalRefuel: Double = 0.0
            var totalConsumption: Double = 0.0

            for (item in it){
                if (item.type == FuelChangedType.REFUEL.type){
                    refuelArray.add(item)
                    totalConsumption += item.fuelData!!
                }else{
                    consumptionArray.add(item)
                    totalRefuel += item.fuelData!!
                }
            }

            binding.ssRefuelrecord.text = refuelArray.size.toString() + " 次"
            binding.ssTotalrefuel.text = totalRefuel.toString() + " L"
            binding.ssRefuelrecord.setTextColor(requireContext().getColor(R.color.theme))
            binding.ssTotalrefuel.setTextColor(requireContext().getColor(R.color.theme))

            binding.ssUnusualrecordcount.text = consumptionArray.size.toString() + " 次"
            binding.ssUnusualrecord.text = totalConsumption.toString() + " L"
            binding.ssUnusualrecordcount.setTextColor(requireContext().getColor(R.color.red))
            binding.ssUnusualrecord.setTextColor(requireContext().getColor(R.color.red))
            listAdapter.apply {
                listAdapter.addFuelChanges(it)
                listAdapter.notifyDataSetChanged()
            }
        })
        updateTimeRang()
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        // TODO: Use the ViewModel

    }

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "onResume:")
        currentDevice = UserPreference.getDevice().toString()
        if(currentDevice.isNotEmpty()){
            binding.tvCarNum.text = currentDevice
            viewModel.getFuelData()
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
            R.id.leftButton -> {
                val offset = UserPreference.getDateOffset()
                UserPreference.setDateOffset(offset + 1)
                updateTimeRang()
                viewModel.getFuelData()
            }

            R.id.rightButton -> {
                val offset = UserPreference.getDateOffset()
                if (offset == 0) {
                    return
                }else{
                    UserPreference.setDateOffset(offset - 1)
                    updateTimeRang()
                    viewModel.getFuelData()
                }
            }

        }
    }

    fun updateTimeRang(){
        val timeRange = viewModel.getDisplayTimeRange()
        binding.centerTime.text = timeRange[0] + "----" + timeRange[1]
    }

    fun handleToast(){
        viewModel.tipLiveData.observe(viewLifecycleOwner, {
            when(it) {
                "request" -> {
                    Toast.makeText(context, R.string.request_successfully, Toast.LENGTH_SHORT).show()
                }
                ToastTips.R_FuelData -> {
                    binding.progressBar.visibility = View.VISIBLE
                    Toast.makeText(context, R.string.updating_data, Toast.LENGTH_SHORT).show()
                }
                ToastTips.R_ReceivedFuelData -> {
                    binding.progressBar.visibility = View.VISIBLE
                    Toast.makeText(context, R.string.parsing_data, Toast.LENGTH_SHORT).show()
                }
                ToastTips.S_ProcessFuelDataComplete -> {
                    binding.progressBar.visibility = View.INVISIBLE
                    Toast.makeText(context, R.string.processing_completed, Toast.LENGTH_SHORT).show()
                    viewModel.getFuelData()
                }
                "fail" -> {
                    binding.progressBar.visibility = View.INVISIBLE
                    Toast.makeText(context, R.string.sync_fail, Toast.LENGTH_SHORT).show()
                }
                ToastTips.B_ConnectFailed -> {
                    binding.progressBar.visibility = View.INVISIBLE
                    Toast.makeText(context, R.string.connect_fail, Toast.LENGTH_SHORT).show()
                }
                ToastTips.B_SendDataFailed -> {
                    binding.progressBar.visibility = View.INVISIBLE
                    Toast.makeText(context, R.string.sync_fail, Toast.LENGTH_SHORT).show()
                }
                ToastTips.B_Disconnect -> {
                    binding.progressBar.visibility = View.INVISIBLE
                    Toast.makeText(context, R.string.disconnect, Toast.LENGTH_SHORT).show()
                }
            }
        })
    }
}
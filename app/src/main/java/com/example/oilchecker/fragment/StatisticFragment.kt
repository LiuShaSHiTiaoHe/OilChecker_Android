package com.example.oilchecker.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.oilchecker.R
import com.example.oilchecker.data.entity.*
import com.example.oilchecker.databinding.StatisticFragmentBinding
import com.example.oilchecker.util.UserPreference
import com.example.oilchecker.util.toDateStr
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import dagger.hilt.android.AndroidEntryPoint
import khronos.*


@AndroidEntryPoint
class StatisticFragment : Fragment(), View.OnClickListener{
    private val TAG = "StatisticFragment"

    companion object {
        fun newInstance() = StatisticFragment()
    }

    private lateinit var binding: StatisticFragmentBinding
    private lateinit var homeViewModel: HomeViewModel
    private var type: String = ""
    private var currentDevice: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.statistic_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = StatisticFragmentBinding.bind(view)
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

        val segmentIndex = UserPreference.getSegmentIndex()
        type = segmentIndex.toString()

        binding.leftButton.setOnClickListener(this)
        binding.rightButton.setOnClickListener(this)
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
                homeViewModel.getChartStyleFuelData()
            }
            onSegmentRechecked {}
            onSegmentUnchecked {}
        }
        updateTimeRang()

        homeViewModel.fuelChartLiveData.observe(viewLifecycleOwner, Observer {
            if (it.size > 0){
                initFuelChart(it)
                binding.chartFuel.isGone = false
                binding.placeholderFuelRecord.isGone = true
            }
        })

        homeViewModel.consumptionChartLiveData.observe(viewLifecycleOwner, Observer {
            if (it.size > 0){
                initConsumptionChart(it)
                binding.chartConsumption.isGone = false
                binding.placeholderConsumption.isGone = true
            }
        })

        homeViewModel.refuelChartLiveData.observe(viewLifecycleOwner, Observer {
            if (it.size > 0){
                initRefuelChart(it)
                binding.chartRefuel.isGone = false
                binding.placeholderRefuelRecord.isGone = true
            }
        })

        currentDevice = UserPreference.getDevice().toString()
        if(currentDevice.isNotEmpty()){
            homeViewModel.getChartStyleFuelData()
        }
    }

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "onResume:")

    }

    fun updateTimeRang(){
        val displayTimeString = homeViewModel.getDisplayTimeRange()
        binding.centerTime.text = displayTimeString
    }

    fun initFuelChart(dataList: ArrayList<ChartDateModel>){
        val lineEntry = ArrayList<BarEntry>()
        lineEntry.clear()
        val chart = binding.chartFuel
        if (dataList.size > 0){
            binding.placeholderFuelRecord.visibility = View.INVISIBLE
            chart.visibility = View.VISIBLE
        }
        for (i in dataList.indices){
            val value = dataList[i].fuelData.toFloat()
            val timeInterval = dataList[i].timeInterval.toFloat()
            if (value != null) {
                lineEntry.add(BarEntry(timeInterval.toFloat(), value.toFloat()))
            }
        }

        chart.xAxis.run {
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            if (dataList.size > 12){
                setLabelCount(12, false)
            }else{
                setLabelCount(dataList.size, false)
            }
        }

        val linedataset = BarDataSet(lineEntry, requireContext().getString(R.string.fuel_record) + " (L)")
        linedataset.color = requireContext().getColor(R.color.red)
        val data = BarData(linedataset)
        chart.data = data
        chart.axisLeft.axisMinimum = 0.0F
        chart.description.isEnabled = false
        chart.axisRight.isEnabled = false
        chart.isDragEnabled = false
        chart.setScaleEnabled(false)
        chart.isDoubleTapToZoomEnabled = false
        chart.isHighlightPerTapEnabled = false
        chart.setBackgroundColor(requireContext().getColor(R.color.white))
        chart.animateXY(30, 30)
        chart.invalidate()

    }

    fun initConsumptionChart(dataList: ArrayList<ChartDateModel>){
        val lineEntry = ArrayList<BarEntry>()
        lineEntry.clear()
        val chart = binding.chartConsumption
        if (dataList.size > 0){
            binding.placeholderConsumption.visibility = View.INVISIBLE
            chart.visibility = View.VISIBLE
        }
        for (i in dataList.indices){
            val value = dataList[i].fuelData
            val timeInterval = dataList[i].timeInterval
            if (value != null) {
                lineEntry.add(BarEntry(timeInterval.toFloat(), value.toFloat()))
            }
        }
        chart.xAxis.run {
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            if (dataList.size > 12){
                setLabelCount(12, false)
            }else{
                setLabelCount(dataList.size, false)
            }
        }
        val linedataset = BarDataSet(lineEntry, resources.getString(R.string.consumption_record) + " (L)")
        linedataset.color = requireContext().getColor(R.color.red)
        val data = BarData(linedataset)
        chart.data = data
        chart.axisLeft.axisMinimum = 0.0F
        chart.description.isEnabled = false
        chart.axisRight.isEnabled = false
        chart.isDragEnabled = false
        chart.setScaleEnabled(false)
        chart.isDoubleTapToZoomEnabled = false
        chart.isHighlightPerTapEnabled = false
        chart.setBackgroundColor(requireContext().getColor(R.color.white))
        chart.invalidate()
    }

    fun initRefuelChart(dataList: ArrayList<ChartDateModel>){
        val lineEntry = ArrayList<BarEntry>()
        lineEntry.clear()
        val chart = binding.chartRefuel
        if (dataList.size > 0){
            binding.placeholderRefuelRecord.visibility = View.INVISIBLE
            chart.visibility = View.VISIBLE
        }
        for (i in dataList.indices){
            val value = dataList[i].fuelData
            val timeInterval = dataList[i].timeInterval
            if (value != null) {
                lineEntry.add(BarEntry(timeInterval.toFloat(), value.toFloat()))
            }
        }

        chart.xAxis.run {
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            if (dataList.size > 12){
                setLabelCount(12, false)
            }else{
                setLabelCount(dataList.size, false)
            }
        }
        val linedataset = BarDataSet(lineEntry, resources.getString(R.string.refuel_record) + " (L)")
        linedataset.color = requireContext().getColor(R.color.theme)

        val data = BarData(linedataset)
        chart.data = data
        chart.axisLeft.axisMinimum = 0.0F
        chart.description.isEnabled = false
        chart.axisRight.isEnabled = false
        chart.isDragEnabled = false
        chart.setScaleEnabled(false)
        chart.isDoubleTapToZoomEnabled = false
        chart.isHighlightPerTapEnabled = false
        chart.setBackgroundColor(requireContext().getColor(R.color.white))
        chart.invalidate()

    }




    override fun onClick(v: View?) {
        when(v?.id){
            R.id.leftButton -> {
                val offset = UserPreference.getDateOffset()
                UserPreference.setDateOffset(offset + 1)
                updateTimeRang()
                homeViewModel.getChartStyleFuelData()
            }

            R.id.rightButton -> {
                val offset = UserPreference.getDateOffset()
                if (offset == 0) {
                    return
                }else{
                    UserPreference.setDateOffset(offset - 1)
                    updateTimeRang()
                    homeViewModel.getChartStyleFuelData()
                }
            }

        }
    }

}
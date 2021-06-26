package com.example.oilchecker.fragment

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import com.example.oilchecker.R
import com.example.oilchecker.data.entity.FuelConsume
import com.example.oilchecker.data.entity.Refuel
import com.example.oilchecker.databinding.StatisticFragmentBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.*
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class StatisticFragment : Fragment(), View.OnClickListener{
    private val TAG = "StatisticFragment"

    companion object {
        fun newInstance() = StatisticFragment()
    }

    private lateinit var viewModel: StatisticViewModel
    private lateinit var statisticFragmentBinding: StatisticFragmentBinding
    private lateinit var homeViewModel: HomeViewModel
    val lineEntry = ArrayList<BarEntry>()

    private var fuelConsumeList = ArrayList<FuelConsume>()
    private var refuelList = ArrayList<Refuel>()



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.statistic_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = StatisticFragmentBinding.bind(view)
        statisticFragmentBinding = binding

        statisticFragmentBinding.ivFuel.setOnClickListener(this)
        statisticFragmentBinding.ivRefuel.setOnClickListener(this)

    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(StatisticViewModel::class.java)
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

        // TODO: Use the ViewModel

        createFuelChart()
        createRefuelChart()
    }

    fun createFuelChart(){
        val lineEntry = ArrayList<BarEntry>()
       /* lineEntry.add(BarEntry(10f, 10f))
        lineEntry.add(BarEntry(12f, 20f))
        lineEntry.add(BarEntry(20f, 30f))
        lineEntry.add(BarEntry(25f, 5f))
        lineEntry.add(BarEntry(30f, 10f))*/

        val id = HomeViewModel.getIdentify()
        if (id != null) {
            viewModel.getFuelConsume(id)
            viewModel.getRefuelData(id)
        }

      /*  val linedataset = BarDataSet(lineEntry, "consume")
        linedataset.color = resources.getColor(R.color.red)

        val data = BarData(linedataset)
        statisticFragmentBinding.chartFuel.data = data
        statisticFragmentBinding.chartFuel.setBackgroundColor(resources.getColor(R.color.white))
        statisticFragmentBinding.chartFuel.animateXY(30, 30)
*/
        viewModel.fuelConsumeLiveData.observe(viewLifecycleOwner, Observer {
            lineEntry.clear()
            Log.i(TAG, "createChart: list ${it.size}")
            val size = it.size
            if (size > 0){
                statisticFragmentBinding.placeholder1.visibility = View.INVISIBLE
                statisticFragmentBinding.chartFuel.visibility = View.VISIBLE
                for (i in it.indices){
                    val value = it[i].capacity?.toFloat()
                    Log.i(TAG, "createChart: $value")
                    if (value != null) {
                        lineEntry.add(BarEntry(i.toFloat(), value.toFloat()))
                    }
                }
                val linedataset = BarDataSet(lineEntry, "consume")
                linedataset.color = resources.getColor(R.color.red)

                val data = BarData(linedataset)
                statisticFragmentBinding.chartFuel.data = data

                statisticFragmentBinding.chartFuel.description.isEnabled = false
                statisticFragmentBinding.chartFuel.xAxis.position = XAxis.XAxisPosition.BOTTOM
                statisticFragmentBinding.chartFuel.axisRight.isEnabled = false
                statisticFragmentBinding.chartFuel.setBackgroundColor(resources.getColor(R.color.white))
//                statisticFragmentBinding.chartFuel.animateXY(30, 30)
                statisticFragmentBinding.chartFuel.zoomAndCenterAnimated((size/15).toFloat(),
                    1F,lineEntry[size-1].x,lineEntry[size-1].y,YAxis.AxisDependency.LEFT, 0.1.toLong()
                )
                fuelConsumeList = it as ArrayList<FuelConsume>

            }


        })
    }

    fun createRefuelChart(){
        val lineEntry = ArrayList<BarEntry>()
       /* lineEntry.add(BarEntry(6f, 10f))
        lineEntry.add(BarEntry(12f, 20f))
        lineEntry.add(BarEntry(13f, 4f))
        lineEntry.add(BarEntry(25f, 15f))
        lineEntry.add(BarEntry(30f, 10f))*/

        /*val linedataset = BarDataSet(lineEntry, "refuel")
        linedataset.color = resources.getColor(R.color.theme)

        val data = BarData(linedataset)
        statisticFragmentBinding.chartRefuel.data = data
        statisticFragmentBinding.chartRefuel.setBackgroundColor(resources.getColor(R.color.white))
        statisticFragmentBinding.chartRefuel.animateXY(30, 30)
*/
        viewModel.refuelLiveData.observe(viewLifecycleOwner, Observer {
            lineEntry.clear()
            Log.i(TAG, "createChart: list ${it.size}")
            val size = it.size
            if (size > 0){
                statisticFragmentBinding.placeholder2.visibility = View.INVISIBLE
                statisticFragmentBinding.chartRefuel.visibility = View.VISIBLE
                for (i in it.indices){
                    val value = it[i].capacity?.toFloat()
                    Log.i(TAG, "createChart: $value")
                    if (value != null) {
                        lineEntry.add(BarEntry(i.toFloat(), value.toFloat()))
                    }
                }
                val linedataset = BarDataSet(lineEntry, "refuel")
                linedataset.color = resources.getColor(R.color.theme)

                val data = BarData(linedataset)
                statisticFragmentBinding.chartRefuel.data = data

                statisticFragmentBinding.chartRefuel.description.isEnabled = false
                statisticFragmentBinding.chartRefuel.xAxis.position = XAxis.XAxisPosition.BOTTOM
                statisticFragmentBinding.chartRefuel.axisRight.isEnabled = false
                statisticFragmentBinding.chartRefuel.setBackgroundColor(resources.getColor(R.color.white))
//                statisticFragmentBinding.chartRefuel.animateXY(30, 30)
                statisticFragmentBinding.chartRefuel.zoomAndCenterAnimated((size/15).toFloat(),
                    1F,lineEntry[size-1].x,lineEntry[size-1].y,YAxis.AxisDependency.LEFT, 0.1.toLong()
                )
                refuelList = it as ArrayList<Refuel>


            }

        })
    }

    override fun onClick(v: View?) {
        when(v?.id){
            R.id.iv_fuel -> {
                val direction = StatisticFragmentDirections.actionStatisFragmentToFuelConsumeRecordFragment()
                v.findNavController().navigate(direction)

            }
            R.id.iv_refuel -> {
                val direction = StatisticFragmentDirections.actionStatisFragmentToRefuelRecordFragment()
                v.findNavController().navigate(direction)

            }
        }
    }

}
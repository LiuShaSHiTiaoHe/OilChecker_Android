package com.example.oilchecker.fragment

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDirections
import androidx.navigation.findNavController
import com.example.oilchecker.R
import com.example.oilchecker.databinding.HomeFragmentBinding
import com.example.oilchecker.util.SpUtils
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment(), View.OnClickListener{
    private val TAG = "HomeFragment"

    companion object {
        fun newInstance() = HomeFragment()
    }
    private lateinit var fragmentHomeFragmentBinding: HomeFragmentBinding
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
        val binding = HomeFragmentBinding.bind(view)
        fragmentHomeFragmentBinding = binding
        fragmentHomeFragmentBinding.tvCarNum.setOnClickListener(this)
        fragmentHomeFragmentBinding.llSync.setOnClickListener(this)
        fragmentHomeFragmentBinding.ivSync.setOnClickListener(this)
        fragmentHomeFragmentBinding.tvSync.setOnClickListener(this)
       // fragmentHomeFragmentBinding.tvAverageOil.setOnClickListener(this)
        fragmentHomeFragmentBinding.tvRealStatus.text = getString(R.string.unknown)
        type = getString(R.string.day)



        fragmentHomeFragmentBinding.segmented {
            initialCheckedIndex = 0
            onSegmentChecked { segment ->
                type = segment.text.toString()
                createChart()
                Log.i(TAG, "onViewCreated: checked ${segment.text} $initialCheckedIndex") }
            onSegmentRechecked { segment -> Log.i(TAG, "onViewCreated: rechecked") }
            onSegmentUnchecked { segment -> Log.i(TAG, "onViewCreated: unchecked")}
        }
        viewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        Log.i(TAG, "onViewCreated: ${HomeViewModel.getAverageOil()}  --> ${HomeViewModel.getStatus()}")

        viewModel.averageFuelConsumeLiveData.observe(viewLifecycleOwner, {
            if (it.isEmpty()){
                fragmentHomeFragmentBinding.tvAverage.text = "0.0 L"
            }else {
                fragmentHomeFragmentBinding.tvAverage.text = "$it L"
            }
        })

        viewModel.fuelStatusLiveData.observe(viewLifecycleOwner, {
            val txt: String
            if(it.equals("异常")){
                txt = getString(R.string.exception)
                fragmentHomeFragmentBinding.tvRealStatus.setTextColor(resources.getColor(R.color.red))
            }else if(it.equals("正常")){
                txt = getString(R.string.normal)
                fragmentHomeFragmentBinding.tvRealStatus.setTextColor(resources.getColor(R.color.theme))
            }else{
                txt = getString(R.string.unknown)
                fragmentHomeFragmentBinding.tvRealStatus.setTextColor(resources.getColor(R.color.theme))
                fragmentHomeFragmentBinding.tvAverage.text = "0.0 L"


            }

            fragmentHomeFragmentBinding.tvRealStatus.text = txt
        })

        viewModel.tipLiveData.observe(viewLifecycleOwner, {
            when(it) {
                "request" -> {
                  //  fragmentHomeFragmentBinding.progressBar.visibility = View.VISIBLE
                    Toast.makeText(context, R.string.request_successfully, Toast.LENGTH_SHORT).show()
                }
                "rev" -> {
                 //   fragmentHomeFragmentBinding.progressBar.visibility = View.VISIBLE
                    Toast.makeText(context, R.string.parsing_data, Toast.LENGTH_SHORT).show()
                }
                "process" -> {
                    fragmentHomeFragmentBinding.progressBar.visibility = View.INVISIBLE
                    Toast.makeText(context, R.string.processing_completed, Toast.LENGTH_SHORT).show()
//                    AlertDialog.Builder(context).setMessage(getString(R.string.processing_completed)).create().show()
                }
                "fail" -> {
                    fragmentHomeFragmentBinding.progressBar.visibility = View.INVISIBLE
                    Toast.makeText(context, R.string.sync_fail, Toast.LENGTH_SHORT).show()
                }
                "disconnect" -> {
                    fragmentHomeFragmentBinding.progressBar.visibility = View.INVISIBLE
                    Toast.makeText(context, R.string.disconnect, Toast.LENGTH_SHORT).show()

                }
            }
        })

        createChart()
    }
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        // TODO: Use the ViewModel

    }

    override fun onResume() {
        super.onResume()
        currentDevice = HomeViewModel.getDevice().toString()
        if(currentDevice.isNotEmpty()){
            fragmentHomeFragmentBinding.tvCarNum.text = currentDevice
            //fragmentHomeFragmentBinding.tvCarNum.setTextColor(resources.getColor(R.color.black))

        }

    }

    override fun onClick(v: View?) {
        when(v?.id){
            R.id.tv_car_num -> {
                //for test when without ble device
               /* val mac = "23:33:33:12:12:13"
                val direction = HomeFragmentDirections.actionHomeFragmentToDeviceInfoFragment(mac)
                v.findNavController().navigate(direction)*/

                val direction = if(currentDevice.isNotEmpty()){
                    HomeFragmentDirections.actionHomeFragmentToSelectCarFragment()
                }else {
                    HomeFragmentDirections.actionHomeFragmentToBleDeviceFragment()
                }
                v.findNavController().navigate(direction)
            }
            R.id.iv_sync, R.id.tv_sync ->{
                val mac = HomeViewModel.getMac()
                fragmentHomeFragmentBinding.progressBar.visibility = View.VISIBLE

                Log.i(TAG, "onClick: ${"001A".toInt(16)}  sync-->--->${HomeViewModel.getIdentify()}")
                lifecycleScope.launch {
                    if (mac != null && mac.isNotEmpty()) {
                        //get fuel history data
                        Log.i(TAG, "onClick: -->connect $mac")
                        viewModel.doConnect(mac)
                    } else {
                        Toast.makeText(context, R.string.add_device, Toast.LENGTH_SHORT).show()
                    }
                }

            }
            R.id.tv_average_oil ->{
                val mac = HomeViewModel.getMac()

                Log.i(TAG, "onClick: ${"001A".toInt(16)}  sync-->--->${HomeViewModel.getIdentify()}")
                lifecycleScope.launch {
                    //get fuel history data
                    Log.i(TAG, "onClick: -->connect ")
                    //02FF018585008201FDFFD800FDFFD700D800FDFFE400E400FDFFA1009C009C0097009300930093009300930093009300930001000100010001000300060006000600FDFF0100010002000200020002000200020002000100010001000100010001000200020002000200020002000200020002000200FDFF020002000200020002000200020002000200020002000200020002000200FDFF01000100010001000100FDFF0100FDFF0600060006000600FDFF060006000600FDFF060006000600060006000600060006000600FDFF06000600FDFF1200FDFF120012001200FDFF12001200FDFF1200120012001200120012001200FDFF120012001300FDFF1600FDFF4300FDFFCD0302D7298585008202120012001200FDFF3300FDFFFEFF1200120012001200120012001200120012001200120012001200120012001200130016001600FDFFFEFFFDFFFEFFFDFFFEFF15001500150015001500FDFF12001200120012001200120012001200FEFF12001200FEFF12001200120012001200120012001200120012001200120012001200120012001200FDFFFEFFFDFF1200FEFFFEFFFEFFFEFFFEFF12001200FEFFFDFFFEFFFEFFFEFFFEFFFEFFFEFFFEFFFEFFFEFFFEFFFEFFFEFFFDFFFEFFFEFFFEFFFEFFFDFFFEFFFEFFFDFFFEFFFEFFFDFFFEFFFEFFFDFF58030202FE85850082FF028303
                    val data ="02FF0185850082FFFDFFD800FDFFD700D800FDFFE400E400FDFFA1009C009C0097009300930093009300930093009300930001000100010001000300060006000600FDFF0100010002000200020002000200020002000100010001000100010001000200020002000200020002000200020002000200FDFF020002000200020002000200020002000200020002000200020002000200FDFF01000100010001000100FDFF0100FDFF0600060006000600FDFF060006000600FDFF060006000600060006000600060006000600FDFF06000600FDFF1200FDFF120012001200FDFF12001200FDFF1200120012001200120012001200FDFF120012001300FDFF1600FDFF4300FDFFCD03"
                           // "02D7298585008202120012001200FDFF3300FDFFFEFF1200120012001200120012001200120012001200120012001200120012001200130016001600FDFFFEFFFDFFFEFFFDFFFEFF15001500150015001500FDFF12001200120012001200120012001200FEFF12001200FEFF12001200120012001200120012001200120012001200120012001200120012001200FDFFFEFFFDFF1200FEFFFEFFFEFFFEFFFEFF12001200FEFFFDFFFEFFFEFFFEFFFEFFFEFFFEFFFEFFFEFFFEFFFEFFFEFFFEFFFDFFFEFFFEFFFEFFFEFFFDFFFEFFFEFFFDFFFEFFFEFFFDFFFEFFFEFFFDFF58030202FE85850082FF028303"
                    viewModel.receiveFuelData(data)
                }

            }
        }
    }

    fun createChart(){

        val lineEntry = ArrayList<Entry>()
        viewModel.getFuelData()

        viewModel.fuelLiveData.observe(viewLifecycleOwner, Observer {
            lineEntry.clear()
            Log.i(TAG, "createChart: list ${it.size}")
            val size = it.size
            if (size > 0){
                fragmentHomeFragmentBinding.placeholder1.visibility = View.GONE
                var index = 0
                var fuelDatasList: ArrayList<String> = ArrayList()
                if (type == getString(R.string.day)){
                    if (size >=  30) {
                        index = size - 30
                        for (i in index until size){
                            fuelDatasList.add(it[i])
                        }
                    }else{
                        fuelDatasList.addAll(it)
                    }

                }else {
                    if (size >= 60) {
                        index = size - 60
                        for (i in index until size){
                            fuelDatasList.add(it[i])
                        }
                    }else{
                        fuelDatasList.addAll(it)
                    }
                }
                for (i in 0 until fuelDatasList.size - 1){
                    if (i < fuelDatasList.size - 2) {
                        val data = fuelDatasList[i]
                        val nextData = fuelDatasList[i+1]
                        if (data.toFloat() - nextData.toFloat() > 5){
                            Log.i(TAG,"Data ${data}, nextData ${nextData}")
                            lineEntry.add(Entry(i.toFloat(),data.toFloat(), getResources().getDrawable(R.drawable.icon_warning)))
                        }else{
                            lineEntry.add(Entry(i.toFloat(), fuelDatasList[i].toFloat()))
                        }
                    }else{
                        lineEntry.add(Entry(i.toFloat(), fuelDatasList[i].toFloat()))
                    }
                }
//                if (index < 0){
//                    index = 0
//                }
//                for (i in index until size){
//                    Log.i(TAG, "createChart: ${it[i].toFloat()}")
//                    lineEntry.add(Entry(i.toFloat(), it[i].toFloat()))
//                }
                val linedataset = LineDataSet(lineEntry, getString(R.string.fuel))
                linedataset.color = resources.getColor(R.color.theme)
                linedataset.setDrawCircles(true)
                linedataset.setCircleColor(resources.getColor(R.color.theme))
                linedataset.setDrawFilled(true)
                linedataset.fillColor = resources.getColor(R.color.theme)
                linedataset.fillAlpha = 80
                linedataset.valueTextSize = 0f

                val data = LineData(linedataset)
                fragmentHomeFragmentBinding.lineChart.description.isEnabled = false
                fragmentHomeFragmentBinding.lineChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
                fragmentHomeFragmentBinding.lineChart.axisRight.isEnabled = false
                fragmentHomeFragmentBinding.lineChart.data = data

                fragmentHomeFragmentBinding.lineChart.setBackgroundColor(resources.getColor(R.color.white))
                fragmentHomeFragmentBinding.lineChart.animateXY(30, 30)
            }

        })
    }

    override fun onStop() {
        super.onStop()
        fragmentHomeFragmentBinding.progressBar.visibility = View.INVISIBLE
    }
}
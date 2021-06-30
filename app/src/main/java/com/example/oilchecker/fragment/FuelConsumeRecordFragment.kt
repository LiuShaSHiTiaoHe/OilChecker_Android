package com.example.oilchecker.fragment

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.oilchecker.R
import com.example.oilchecker.adapter.DeviceListAdapter
import com.example.oilchecker.adapter.FuelListAdapter
import com.example.oilchecker.data.entity.FuelConsume
import com.example.oilchecker.databinding.FuelConsumeRecordFragmentBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
@AndroidEntryPoint
class FuelConsumeRecordFragment : Fragment(), View.OnClickListener {

    companion object {
        fun newInstance() = FuelConsumeRecordFragment()
    }

    private lateinit var viewModel: FuelConsumeRecordViewModel
    private lateinit var statisticViewModel: StatisticViewModel

    private lateinit var fuelConsumeRecordFragmentBinding: FuelConsumeRecordFragmentBinding

    private var type: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fuel_consume_record_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FuelConsumeRecordFragmentBinding.bind(view)
        type = getString(R.string.week)
        binding.llBack.setOnClickListener(this)
        binding.ivBack.setOnClickListener(this)
        binding.segmented {
            initialCheckedIndex = 0
            onSegmentChecked { segment ->
                type = segment.text.toString()
                val id = HomeViewModel.getIdentify()
                if (id != null) {
                    statisticViewModel.getFuelConsume(id)
                }

            }
            onSegmentRechecked { segment ->  }
            onSegmentUnchecked { segment ->  }
        }
        val listAdapter = FuelListAdapter()
        binding.rvFuel.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = listAdapter
            itemAnimator = null
        }

        viewModel = ViewModelProvider(this).get(FuelConsumeRecordViewModel::class.java)
        // TODO: Use the ViewModel
        statisticViewModel = ViewModelProvider(this).get(StatisticViewModel::class.java)

        lifecycleScope.launch {
            val id = HomeViewModel.getIdentify()
            if (id != null) {
                statisticViewModel.getFuelConsume(id)
            }

            statisticViewModel.fuelConsumeLiveData.observe(viewLifecycleOwner, Observer {
                it.let {
                    Log.i("TAG", "onViewCreated: list  ${it.size}")
                    val size = it.size
                    var list = arrayListOf<FuelConsume>().toList()
                    when(type){
                        getString(R.string.week) -> {
                            if (size >= 21){
                                list = it.subList(size-21, size-1)
                            }else {
                                list = it
                            }
                        }
                        getString(R.string.month) -> {
                            if (size >= 41) {
                                list = it.subList(size - 41, size - 1)
                            }else {
                                list = it
                            }
                        }
                        getString(R.string.year) -> {
                            list = it
                        }
                    }
                    listAdapter.apply {
                        listAdapter.addFuels(list)
                        listAdapter.notifyDataSetChanged()
                    }

                }
            })
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

    }

    override fun onClick(v: View?) {
        when(v?.id){
            R.id.ll_back -> {
                v.findNavController().navigateUp()
            }
        }
    }

}
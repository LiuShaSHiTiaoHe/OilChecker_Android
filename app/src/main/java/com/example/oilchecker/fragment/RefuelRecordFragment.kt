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
import com.example.oilchecker.adapter.FuelListAdapter
import com.example.oilchecker.adapter.RefuelListAdapter
import com.example.oilchecker.data.entity.FuelConsume
import com.example.oilchecker.data.entity.Refuel
import com.example.oilchecker.databinding.RefuelRecordFragmentBinding
import com.example.oilchecker.util.UserPreference
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
@AndroidEntryPoint
class RefuelRecordFragment : Fragment(), View.OnClickListener{
    private val TAG = "RefuelRecordFragment"

    companion object {
        fun newInstance() = RefuelRecordFragment()
    }

    private lateinit var viewModel: RefuelRecordViewModel
    private lateinit var refuelRecordFragmentBinding: RefuelRecordFragmentBinding
    private lateinit var statisticViewModel: StatisticViewModel
    private var type: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.refuel_record_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = RefuelRecordFragmentBinding.bind(view)
        refuelRecordFragmentBinding = binding
        type = getString(R.string.week)
        refuelRecordFragmentBinding.llBack.setOnClickListener(this)
        refuelRecordFragmentBinding.ivBack.setOnClickListener(this)
        refuelRecordFragmentBinding.segmented {
            initialCheckedIndex = 0
            onSegmentChecked { segment ->
                type = segment.text.toString()
                val id = UserPreference.getIdentify()
                if (id != null) {
                    statisticViewModel.getRefuelData(id)
                }
            }
            onSegmentRechecked { segment ->  }
            onSegmentUnchecked { segment ->  }
        }

        val listAdapter = RefuelListAdapter()
        binding.rvFuel.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = listAdapter
            itemAnimator = null
        }

        viewModel = ViewModelProvider(this).get(RefuelRecordViewModel::class.java)
        // TODO: Use the ViewModel
        statisticViewModel = ViewModelProvider(this).get(StatisticViewModel::class.java)

        Log.i(TAG, "onViewCreated: -->")
        lifecycleScope.launch {
            val id = UserPreference.getIdentify()
            if (id != null) {
                statisticViewModel.getRefuelData(id)
            }

            statisticViewModel.refuelLiveData.observe(viewLifecycleOwner, Observer {
                it.let {
                    Log.i(TAG, "onViewCreated: list  ${it.size}")
                    val size = it.size
                    var list = arrayListOf<Refuel>().toList()
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
                            if (size >= 61){
                                list = it.subList(size-61, size-1)
                            }else {
                                list = it
                            }
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
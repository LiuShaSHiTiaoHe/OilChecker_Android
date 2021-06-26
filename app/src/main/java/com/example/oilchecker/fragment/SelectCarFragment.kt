package com.example.oilchecker.fragment

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.oilchecker.R
import com.example.oilchecker.adapter.DeviceListAdapter
import com.example.oilchecker.databinding.SelectCarFragmentBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SelectCarFragment : Fragment(), View.OnClickListener{
    private val TAG = "SelectCarFragment"

    companion object {
        fun newInstance() = SelectCarFragment()
    }

    private val viewModel: SelectCarViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.select_car_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = SelectCarFragmentBinding.bind(view)
        binding.llBack.setOnClickListener(this)
        binding.ivBack.setOnClickListener(this)
        val listAdapter = DeviceListAdapter()
        binding.rvDevice.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = listAdapter
            itemAnimator = null
        }


        Log.i(TAG, "onViewCreated:-->")

        lifecycleScope.launch {

            viewModel.searchAllDevice().observe(viewLifecycleOwner, Observer {
                it.let {
                    listAdapter.apply {
                        listAdapter.addDevice(it)
                        listAdapter.notifyDataSetChanged()
                    }

                }
            })
        }
    }

    override fun onClick(v: View?) {
        when(v?.id){
            R.id.ll_back -> {
                /*val direction = SelectCarFragmentDirections.actionSelectCarFragmentToHomeFragment()
                v.findNavController().navigate(direction)*/
                v.findNavController().navigateUp()
            }
        }
    }


}
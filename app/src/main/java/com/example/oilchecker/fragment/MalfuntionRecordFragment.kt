package com.example.oilchecker.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isGone
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.oilchecker.R
import com.example.oilchecker.adapter.MalfuntionalListAdapter
import com.example.oilchecker.databinding.FragmentMalfuntionRecordBinding
import com.example.oilchecker.util.UserPreference
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MalfuntionRecordFragment : Fragment(), View.OnClickListener  {
    private lateinit var binding: FragmentMalfuntionRecordBinding
    private var currentDevice: String = ""
    private lateinit var viewModel: MalfuntionRecordViewModel

    var listAdapter = MalfuntionalListAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_malfuntion_record, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentMalfuntionRecordBinding.bind(view)
        binding.ivBack.setOnClickListener(this)
        binding.llBack.setOnClickListener(this)
        binding.tvName.setOnClickListener(this)

        binding.rvFuel.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = listAdapter
            itemAnimator = null
        }
        binding.settingInfoLlPhoto.isGone = true
        binding.emptyTips.isGone = false
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(MalfuntionRecordViewModel::class.java)
        // TODO: Use the ViewModel
        currentDevice = UserPreference.getDevice().toString()
        if(currentDevice.isNotEmpty()){
            loadMalfuntionalData()
        }

    }

    fun loadMalfuntionalData(){
        lifecycleScope.launch {
            viewModel.getAllMalfuntionRecords().observe(viewLifecycleOwner, Observer {
                it.let {
                    if (it.size > 0){
                        binding.settingInfoLlPhoto.isGone = false
                        binding.emptyTips.isGone = true
                        listAdapter.apply {
                            listAdapter.addFuelChanges(it)
                            listAdapter.notifyDataSetChanged()
                        }
                    }

                }
            })
        }
    }


    override fun onClick(view: View?) {
        when(view?.id){
            R.id.iv_back,R.id.ll_back,R.id.tv_name ->{
                view.findNavController().navigateUp()
            }

        }
    }

    companion object {
        fun newInstance() = MalfuntionRecordFragment()
    }

}
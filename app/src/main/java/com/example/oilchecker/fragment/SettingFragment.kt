package com.example.oilchecker.fragment

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.navigation.findNavController
import com.example.oilchecker.R
import com.example.oilchecker.databinding.SettingFragmentBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingFragment : Fragment(), View.OnClickListener {

    companion object {
        fun newInstance() = SettingFragment()
    }

    private lateinit var viewModel: SettingViewModel
    private lateinit var settingFragmentBinding: SettingFragmentBinding
    private var currentDevice: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.setting_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = SettingFragmentBinding.bind(view)
        settingFragmentBinding = binding
        settingFragmentBinding.llAdd.setOnClickListener(this)
        settingFragmentBinding.llDevices.setOnClickListener(this)
        settingFragmentBinding.llList.setOnClickListener(this)
        settingFragmentBinding.llSearch.setOnClickListener(this)

        currentDevice = HomeViewModel.getDevice().toString()
        val height = dp2px(context, 1)
        val top = dp2px(context, 15)
        val margin = dp2px(context, 15)

        if(currentDevice.isNotEmpty()){
            val tvLineLayoutParams: ConstraintLayout.LayoutParams = ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height)
            tvLineLayoutParams.topToBottom = R.id.ll_devices
            tvLineLayoutParams.topMargin = top
            tvLineLayoutParams.marginStart = margin
            tvLineLayoutParams.marginEnd = margin
            settingFragmentBinding.tvLine.layoutParams = tvLineLayoutParams
            settingFragmentBinding.llAdd.visibility = View.INVISIBLE
            settingFragmentBinding.llDevices.visibility = View.VISIBLE
        }else {
            val tvLineLayoutParams: ConstraintLayout.LayoutParams = ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,height)
            tvLineLayoutParams.topToBottom = R.id.ll_add
            tvLineLayoutParams.topMargin = top
            tvLineLayoutParams.marginStart = margin
            tvLineLayoutParams.marginEnd = margin
            settingFragmentBinding.tvLine.layoutParams = tvLineLayoutParams
            settingFragmentBinding.llAdd.visibility = View.VISIBLE
            settingFragmentBinding.llDevices.visibility = View.INVISIBLE
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(SettingViewModel::class.java)
        // TODO: Use the ViewModel
    }

    override fun onClick(v: View?) {
        when(v?.id){
            R.id.ll_add -> {
                val direction = SettingFragmentDirections.actionSettingFragmentToBleDeviceFragment()
                v.findNavController().navigate(direction)
            }
            R.id.ll_devices -> {
                val direction = SettingFragmentDirections.actionSettingFragmentToSelectCarFragment()
                v.findNavController().navigate(direction)
            }
            R.id.ll_list -> {
                //fault

            }
            R.id.ll_search -> {
                val direction = SettingFragmentDirections.actionSettingFragmentToBleDeviceFragment()
                v.findNavController().navigate(direction)
            }
        }
    }

    fun dp2px(context: Context?, dp:Int):Int=(dp * requireContext().resources.displayMetrics.density).toInt()

    fun px2dp(context: Context, px:Int):Int =(px / context.resources.displayMetrics.density).toInt()


}
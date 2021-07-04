package com.example.oilchecker.fragment

import android.os.Bundle
import android.text.Editable
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.findNavController
import com.example.oilchecker.R
import com.example.oilchecker.databinding.FragmentSettingAppParametersBinding
import com.example.oilchecker.databinding.SettingFragmentBinding
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class SettingAppParametersFragment : Fragment(), View.OnClickListener {

    private lateinit var binding: FragmentSettingAppParametersBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_setting_app_parameters, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentSettingAppParametersBinding.bind(view)
        binding.btnSettingSave.setOnClickListener(this)
        binding.ivBack.setOnClickListener(this)
        binding.llBack.setOnClickListener(this)
        binding.tvName.setOnClickListener(this)

        val thresholds = HomeViewModel.getThreshold()
        binding.etThreshold.text = Editable.Factory.getInstance().newEditable(thresholds.toString())

    }

    companion object {
        fun newInstance() = SettingAppParametersFragment()
    }

    override fun onClick(view: View?) {
        when(view?.id){
            R.id.btn_setting_save -> {
                val thresholds = binding.etThreshold.text.toString()
                if (thresholds.isEmpty()){
                    Toast.makeText(context, getString(R.string.inputErrorData), Toast.LENGTH_SHORT).show()
                }else{
                    HomeViewModel.setThresholdValue(thresholds.toDouble())
                    Toast.makeText(context, getString(R.string.setAppParamatersSuccess), Toast.LENGTH_SHORT).show()
                    view.findNavController().navigateUp()
                }
            }
            R.id.iv_back,R.id.ll_back,R.id.tv_name ->{
                view.findNavController().navigateUp()
            }

        }
    }
}
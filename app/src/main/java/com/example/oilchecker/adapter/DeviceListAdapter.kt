package com.example.oilchecker.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.oilchecker.data.entity.Device
import com.example.oilchecker.databinding.ItemDeviceBinding
import com.example.oilchecker.fragment.BleDeviceFragmentDirections
import com.example.oilchecker.fragment.HomeViewModel
import com.example.oilchecker.fragment.SelectCarFragmentDirections

class DeviceListAdapter: RecyclerView.Adapter<DeviceListAdapter.ViewHolder>(){
    private var devices: ArrayList<Device> = ArrayList()

    inner class ViewHolder(private val binding: ItemDeviceBinding): RecyclerView.ViewHolder(binding.root){
        init {
            binding.root.setOnClickListener{
                binding.tvName.text?.let { text ->
                    //navigate to back ,set current device
                    HomeViewModel.setDevice(text.toString())
                    HomeViewModel.setMac(binding.tvMac.text.toString())
                    HomeViewModel.setIdentify(binding.tvIdentify.text.toString())
                    val direction = SelectCarFragmentDirections.actionSelectCarFragmentToHomeFragment()
                    it.findNavController().navigate(direction)
                }
            }
        }
        fun bind(data: Device){
            with(binding) {
                tvName.text = data.num + "(BT-${data.deviceId})"
                tvMac.text = data.mac
                tvIdentify.text = data.deviceId
                Log.i("SelectCarFragment", "bind: ${data.num}")
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Log.i("SelectCarFragment", "onBindViewHolder:$position -->  ")
        holder.bind(devices[position])

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        Log.i("SelectCarFragment", "onCreateViewHolder: ")
        val binding = ItemDeviceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = devices.size

    fun addDevice(device: List<Device>){
        this.devices.apply {
            addAll(device)
        }
    }







}
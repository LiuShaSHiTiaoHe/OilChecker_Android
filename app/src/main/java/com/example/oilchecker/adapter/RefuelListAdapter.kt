package com.example.oilchecker.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.oilchecker.R
import com.example.oilchecker.data.entity.Device
import com.example.oilchecker.data.entity.FuelConsume
import com.example.oilchecker.data.entity.Refuel
import com.example.oilchecker.databinding.ItemDeviceBinding
import com.example.oilchecker.databinding.ItemFuelRecordBinding
import com.example.oilchecker.fragment.BleDeviceFragmentDirections
import com.example.oilchecker.fragment.HomeViewModel
import com.example.oilchecker.fragment.SelectCarFragmentDirections
import kotlin.coroutines.coroutineContext

class RefuelListAdapter: RecyclerView.Adapter<RefuelListAdapter.ViewHolder>(){
    private var fuels: ArrayList<Refuel> = ArrayList()
    private var context: Context? = null

    inner class ViewHolder(private val binding: ItemFuelRecordBinding): RecyclerView.ViewHolder(binding.root){
        fun bind(data: Refuel){
            with(binding) {
                tvName.text = context?.getString(R.string.refuel_record)
                tvId.text = data.deviceId
                tvNum.text = data.id.toString()
                tvVolume.text = data.capacity
                Log.i("SelectCarFragment", "bind: ${data.capacity}")
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Log.i("SelectCarFragment", "onBindViewHolder:$position -->  ")
        holder.bind(fuels[position])

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        Log.i("SelectCarFragment", "onCreateViewHolder: ")
        context = parent.context
        val binding = ItemFuelRecordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = fuels.size

    fun addFuels(fuels: List<Refuel>){
        this.fuels.clear()
        this.fuels.apply {
            addAll(fuels)
        }
    }







}
package com.example.oilchecker.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.oilchecker.R
import com.example.oilchecker.data.entity.FuelChange
import com.example.oilchecker.databinding.ItemHomeRecordLayoutBinding
import com.example.oilchecker.util.FuelChangedType
import com.example.oilchecker.util.UserPreference
import com.example.oilchecker.util.toDateStr


class HomeViewDataListAdapter: RecyclerView.Adapter<HomeViewDataListAdapter.ViewHolder>(){
    private var fuelChangedDatas: ArrayList<FuelChange> = ArrayList()
    private var context: Context? = null

    inner class ViewHolder(private val binding: ItemHomeRecordLayoutBinding): RecyclerView.ViewHolder(binding.root){
        fun bind(data: FuelChange){
            with(binding) {
                tvTime.text = data.recordTimeInterval!!.toDateStr("yyyy/MM/dd HH:mm")
                val type = data.type
                if (type == FuelChangedType.REFUEL.type){
                    tvAction.text = context?.getString(R.string.fuelchanged_refuel)
                    tvAction.setTextColor(context!!.resources.getColor(R.color.theme))
                    tvData.setTextColor(context!!.resources.getColor(R.color.theme))
                }else{
                    tvAction.text = context?.getString(R.string.fuelchanged_unusual)
                    tvAction.setTextColor(context!!.resources.getColor(R.color.red))
                    tvData.setTextColor(context!!.resources.getColor(R.color.red))
                }
//                tvData.text = data.fuelData.toString() + "L"
                tvData.text = String.format("%.1f",data.fuelData) + "L"
            }

        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Log.i("SelectCarFragment", "onBindViewHolder:$position -->  ")
        holder.bind(fuelChangedDatas[position])

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        Log.i("SelectCarFragment", "onCreateViewHolder: ")
        context = parent.context
        val binding = ItemHomeRecordLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = fuelChangedDatas.size

    fun addFuelChanges(fuels: List<FuelChange>){
        this.fuelChangedDatas.clear()
        this.fuelChangedDatas.apply {
            addAll(fuels)
        }
    }







}
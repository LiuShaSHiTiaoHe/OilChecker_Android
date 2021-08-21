package com.example.oilchecker.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import com.example.oilchecker.data.entity.MalfunctionModel
import com.example.oilchecker.databinding.ItemMalfuntionBinding
import com.example.oilchecker.fragment.MalfuntionRecordViewModel
import com.example.oilchecker.util.toDateStr

class MalfuntionalListAdapter: RecyclerView.Adapter<MalfuntionalListAdapter.ViewHolder>(){
    private var malfuntionalDatas: ArrayList<MalfunctionModel> = ArrayList()
    private var context: Context? = null

    inner class ViewHolder(private val binding: ItemMalfuntionBinding): RecyclerView.ViewHolder(binding.root){
        fun bind(data: MalfunctionModel){
            with(binding) {
                tvTime.text = data.recordTimeInterval!!.toDateStr("yyyy/MM/dd HH:mm")
                tvError.text = data.errorDes
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Log.i("SelectCarFragment", "onBindViewHolder:$position -->  ")
        holder.bind(malfuntionalDatas[position])

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        Log.i("SelectCarFragment", "onCreateViewHolder: ")
        context = parent.context
        val binding = ItemMalfuntionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = malfuntionalDatas.size

    fun addFuelChanges(malfuntionals: List<MalfunctionModel>){
        this.malfuntionalDatas.clear()
        this.malfuntionalDatas.apply {
            addAll(malfuntionals)
        }
    }
}
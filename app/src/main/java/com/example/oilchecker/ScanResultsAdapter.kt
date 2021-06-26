package com.example.oilchecker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.polidea.rxandroidble2.scan.ScanResult

internal class ScanResultsAdapter(
    private val onClickListener: (ScanResult) -> Unit
) : RecyclerView.Adapter<ScanResultsAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val device : TextView = itemView.findViewById(R.id.tv_name)
        val rssi : TextView = itemView.findViewById(R.id.tv_rssi)
        val view = itemView
    }

    private val data = mutableListOf<ScanResult>()
    fun addScanResult(bleScanResult: ScanResult) {
        data.withIndex()
            .firstOrNull { it.value.bleDevice == bleScanResult.bleDevice }
            ?.let {
                data[it.index] = bleScanResult
                notifyItemChanged(it.index)
            }
            ?: kotlin.run {
                with(data) {
                    if (bleScanResult.bleDevice.name?.contains("BT-") == true){
                        if (contains(bleScanResult)){

                        }else{
                            add(bleScanResult)
                            sortBy { it.bleDevice.macAddress }
                            notifyDataSetChanged()
                        }
                    }
                }
//                notifyDataSetChanged()
            }
    }

    fun clearScanResults() {
        data.clear()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return LayoutInflater.from(parent.context)
            .inflate(R.layout.item_scan_device, parent, false)
            .let { ViewHolder(it) }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(data[position]) {
//            holder.device.text = String.format("%s (%s)", bleDevice.macAddress, bleDevice.name)
            holder.device.text = String.format("%s", bleDevice.name)
            // holder.rssi.text = String.format("RSSI: %d", rssi)
//            holder.device.setOnClickListener{onClickListener(this)}
//            holder.itemView.setOnClickListener{ onClickListener(this)}
            holder.view.setOnClickListener{onClickListener(this)}
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }
}
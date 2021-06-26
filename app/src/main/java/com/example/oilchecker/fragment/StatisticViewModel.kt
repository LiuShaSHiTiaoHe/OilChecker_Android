package com.example.oilchecker.fragment

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.oilchecker.data.AppDatabase
import com.example.oilchecker.data.entity.FuelConsume
import com.example.oilchecker.data.entity.Refuel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StatisticViewModel @Inject constructor(
    private val database: AppDatabase
): ViewModel() {
    // TODO: Implement the ViewModel
    var fuelConsumeLiveData = MutableLiveData<List<FuelConsume>>()
    var refuelLiveData = MutableLiveData<List<Refuel>>()

    fun getFuelConsume(id: String){
        viewModelScope.launch {
            async(Dispatchers.IO) {
                val list = database.fuelConsuemDataDao().getFuelConsumeData(id)
                fuelConsumeLiveData.postValue(list)

            }
        }
    }

    fun getRefuelData(id: String){
        viewModelScope.launch {
            async(Dispatchers.IO) {
                val list = database.refuelDataDao().getRefuelData(id)
                Log.i("TAG", "getRefuelData: ${list.size}")
                refuelLiveData.postValue(list)

            }
        }
    }
}
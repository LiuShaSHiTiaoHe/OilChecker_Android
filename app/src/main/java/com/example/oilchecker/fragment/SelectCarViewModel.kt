package com.example.oilchecker.fragment

import android.bluetooth.BluetoothClass
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.example.oilchecker.data.AppDatabase
import com.example.oilchecker.data.entity.Device
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

@HiltViewModel
class SelectCarViewModel @Inject constructor(
    private val database: AppDatabase
) : ViewModel() {
    // TODO: Implement the ViewModel
    private val devices = MutableLiveData<List<Device>>()


    suspend fun searchAllDevice(): LiveData<List<Device>>{
        coroutineScope {
            async(Dispatchers.IO) {

                val list = database.deviceDao().getAllDevice()
                devices.postValue(list)
            }
        }
        return devices

    }
}
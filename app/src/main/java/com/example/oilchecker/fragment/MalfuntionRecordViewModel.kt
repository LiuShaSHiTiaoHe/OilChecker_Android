package com.example.oilchecker.fragment

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.oilchecker.data.AppDatabase
import com.example.oilchecker.data.entity.MalfunctionModel
import com.example.oilchecker.util.UserPreference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject


@HiltViewModel
class MalfuntionRecordViewModel @Inject constructor(
    private val database: AppDatabase
):  ViewModel() {
    private val records = MutableLiveData<List<MalfunctionModel>>()

    suspend fun getAllMalfuntionRecords(): LiveData<List<MalfunctionModel>>{
        coroutineScope {
            async(Dispatchers.IO) {
                val id = UserPreference.getIdentify()
                val list = database.malfunctionDao().getMalfunctionData(id!!)
                records.postValue(list)
            }
        }

        return records
    }


}
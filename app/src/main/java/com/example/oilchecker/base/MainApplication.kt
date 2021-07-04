package com.example.oilchecker.base

import android.app.Application
import com.polidea.rxandroidble2.RxBleClient
import com.tencent.mmkv.MMKV
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MainApplication : Application(){
    companion object {
        lateinit var rxBleClient: RxBleClient
        private set

        private var instances: MainApplication? = null
        fun getInstance(): MainApplication {
            if (instances == null) {
                synchronized(MainApplication::class.java) {
                    if (instances == null) {
                        instances = MainApplication()
                    }
                }
            }
            return instances!!
        }
    }

    override fun onCreate() {
        super.onCreate()
      //  rxBleClient = RxBleClient.create(this)
        rxBleClient = RxBleClient.create(this)
        MMKV.initialize(this)
        instances = this

    }
}
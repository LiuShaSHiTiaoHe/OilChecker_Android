package com.example.oilchecker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.oilchecker.data.entity.*

@Database(entities = [Device::class, Fuel::class, FuelConsume::class ,Refuel::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deviceDao(): DeviceDao
    abstract fun fuelDataDao(): FuelDataDao
    abstract fun fuelConsuemDataDao(): FuelConsumeDao
    abstract fun refuelDataDao(): RefuelDao


    companion object{
        @Volatile
        private var instance: AppDatabase? = null
        fun get(context: Context): AppDatabase {
            if (instance == null){
                instance = Room.databaseBuilder(context.applicationContext,
                AppDatabase::class.java,"AppDatabase")
                    .addCallback(object : RoomDatabase.Callback(){
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                        }
                    }).build()
            }
            return instance!!
        }
    }
}
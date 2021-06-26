package com.example.oilchecker.di

import android.content.Context
import com.example.oilchecker.data.AppDatabase
import com.example.oilchecker.data.entity.DeviceDao
import com.example.oilchecker.data.entity.FuelConsumeDao
import com.example.oilchecker.data.entity.FuelDataDao
import com.example.oilchecker.data.entity.RefuelDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class DatabaseModel {
    @Singleton
    @Provides
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.get(context)
    }

    @Provides
    fun provideDeviceDao(appDatabase: AppDatabase): DeviceDao {
        return appDatabase.deviceDao()
    }

    @Provides
    fun provideFuelDataDao(appDatabase: AppDatabase): FuelDataDao {
        return appDatabase.fuelDataDao()
    }

    @Provides
    fun provideFuelConsumeDataDao(appDatabase: AppDatabase): FuelConsumeDao {
        return appDatabase.fuelConsuemDataDao()
    }

    @Provides
    fun provideRefuelDataDao(appDatabase: AppDatabase): RefuelDao {
        return appDatabase.refuelDataDao()
    }
}
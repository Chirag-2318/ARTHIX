package com.chirag.arthix.di

import com.chirag.arthix.report.phrasing.OnDeviceMediaPipeEngine
import com.chirag.arthix.report.phrasing.ReportPhrasingEngine
import com.google.gson.Gson
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ReportProvidesModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ReportBindsModule {

    @Binds
    @Singleton
    abstract fun bindReportPhrasingEngine(
        impl: OnDeviceMediaPipeEngine
    ): ReportPhrasingEngine
}

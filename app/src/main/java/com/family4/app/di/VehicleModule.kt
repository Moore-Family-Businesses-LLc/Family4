package com.family4.app.di

import com.family4.app.vehicle.bluelink.BlueLinkApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

/**
 * Provides the BlueLink Retrofit instance.
 * Other vehicle deps (BlueLinkAuthManager, ObdBluetoothManager, ObdPidDecoder,
 * BlueLinkRepository) use @Inject constructor() + @Singleton — Hilt handles them
 * automatically without explicit @Provides.
 */
@Module
@InstallIn(SingletonComponent::class)
object VehicleModule {

    private const val BLUELINK_BASE_URL = "https://prd.ca-ccapi.hyundai.com:8080"

    @Provides
    @Singleton
    fun provideBlueLinkOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
            )
            .build()

    @Provides
    @Singleton
    fun provideBlueLinkApi(okHttpClient: OkHttpClient): BlueLinkApi =
        Retrofit.Builder()
            .baseUrl(BLUELINK_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BlueLinkApi::class.java)
}

package com.smartmove.androidfilelogger

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import timber.log.Timber

class ApiServiceGenerator(baseUrl: String) {

    private val logger = HttpLoggingInterceptor.Logger {
            message -> Timber.i(message)
    }

    private val loggingInterceptor = HttpLoggingInterceptor(logger)
        .setLevel(HttpLoggingInterceptor.Level.BASIC)

    private val httpClient = OkHttpClient.Builder().addInterceptor(loggingInterceptor).build()

    private val retrofit = Retrofit.Builder()
        .client(httpClient)
        .baseUrl(baseUrl)
        .build()

    fun <S> createService(serviceClass: Class<S>): S {
        return retrofit.create(serviceClass)
    }
}

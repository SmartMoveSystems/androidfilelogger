package com.smartmove.androidfilelogger

import okhttp3.OkHttpClient
import retrofit2.Retrofit

class ApiServiceGenerator {

    private val retrofit = Retrofit.Builder()
        .client(OkHttpClient.Builder().build())
        .build()

    fun <S> createService(serviceClass: Class<S>): S {
        return retrofit.create(serviceClass)
    }
}

package com.smartmove.androidfilelogger

import okhttp3.OkHttpClient
import retrofit2.Retrofit

class ApiServiceGenerator(baseUrl: String) {

    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(OkHttpClient.Builder().build())
        .build()

    fun <S> createService(serviceClass: Class<S>): S {
        return retrofit.create(serviceClass)
    }
}

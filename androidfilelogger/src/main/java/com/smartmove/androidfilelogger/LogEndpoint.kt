package com.smartmove.androidfilelogger

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Url
import retrofit2.http.Part
import retrofit2.http.PartMap

interface LogEndpoint {
    @Multipart
    @POST
    fun sendLogs(
        @Url url: String,
        @Part file: MultipartBody?
    ): Call<Void>

    @Multipart
    @POST
    fun sendLogs(
        @Url url: String,
        @Part file: MultipartBody?,
        @PartMap additionalParts: @JvmSuppressWildcards Map<String, RequestBody>
    ): Call<Void>
}

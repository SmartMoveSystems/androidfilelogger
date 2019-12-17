package com.smartmove.androidfilelogger

import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface LogEndpoint {
    @Multipart
    @POST("")
    fun sendLogs(
        @Part("subject") subject: RequestBody,
        @Part("body") messageBody: RequestBody,
        @Part("type") type: RequestBody,
        @Part("file\"; filename=\"logs.zip\" ") file: RequestBody?
    ): Call<Void>
}

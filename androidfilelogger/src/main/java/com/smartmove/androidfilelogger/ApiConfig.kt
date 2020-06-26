package com.smartmove.androidfilelogger

data class ApiConfig @JvmOverloads constructor(
    val url: String,
    val stringParts: Map<String, String>? = null
)

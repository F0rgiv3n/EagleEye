package com.eagleeye.data

data class GeoPoint(
    val ip: String,
    val lat: Float,
    val lon: Float,
    val country: String,
    val countryCode: String,
    val city: String,
    val isp: String,
    val isHome: Boolean = false
)

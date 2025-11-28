package com.example.helloworldapp

data class WeatherResponse(
    val status: String,
    val count: String,
    val info: String,
    val infocode: String,
    val lives: List<LiveWeather>?
)

data class LiveWeather(
    val province: String,
    val city: String,
    val adcode: String,
    val weather: String,
    val temperature: String,
    val winddirection: String,
    val windpower: String,
    val humidity: String,
    val reporttime: String
)
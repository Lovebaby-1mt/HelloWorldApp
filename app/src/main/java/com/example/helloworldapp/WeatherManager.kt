package com.example.helloworldapp

import android.os.Handler
import android.os.Looper
import com.google.gson.Gson
import okhttp3.*
import java.io.IOException

/**
 * 天气管理器
 * 负责天气的网络请求、数据解析和回调分发
 */
class WeatherManager private constructor() {

    private val client = OkHttpClient()
    private val gson = Gson()
    private val mainHandler = Handler(Looper.getMainLooper())

    // 回调接口
    interface WeatherCallback {
        fun onSuccess(weather: LiveWeather)
        fun onError(msg: String)
    }

    companion object {
        // 单例模式
        val instance: WeatherManager by lazy { WeatherManager() }

        private const val API_KEY = "空"
        private const val CITY_CODE = "330114"
        private const val WEATHER_URL =
            "https://restapi.amap.com/v3/weather/weatherInfo?city=$CITY_CODE&extensions=base&key=$API_KEY"
    }

    fun fetchWeather(callback: WeatherCallback) {
        val request = Request.Builder().url(WEATHER_URL).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mainHandler.post { callback.onError(e.message ?: "Network Error") }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (it.isSuccessful) {
                        val jsonStr = it.body?.string()
                        if (jsonStr != null) {
                            try {
                                val weatherResponse =
                                    gson.fromJson(jsonStr, WeatherResponse::class.java)
                                val lives = weatherResponse.lives?.firstOrNull()
                                if (lives != null) {
                                    mainHandler.post { callback.onSuccess(lives) }
                                } else {
                                    mainHandler.post { callback.onError("No weather data") }
                                }
                            } catch (e: Exception) {
                                mainHandler.post { callback.onError("Parse Error") }
                            }
                        }
                    } else {
                        mainHandler.post { callback.onError("Server Error: ${it.code}") }
                    }
                }
            }
        })
    }
}
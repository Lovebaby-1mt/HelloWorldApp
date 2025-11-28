package com.example.helloworldapp

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import okhttp3.*
import java.io.IOException

/**
 * 天气详情 Activity
 * 功能：
 * 1. 展示特定城市的实时天气和未来预报
 * 2. 支持城市切换（顶部 Tab）
 * 3. 根据天气状况和时间（昼夜）动态调整背景和图标
 */
class WeatherActivity : AppCompatActivity() {

    private val client = OkHttpClient()
    private val gson = Gson()

    // 支持切换的城市列表 (城市名 -> 城市代码)
    private val cityMap = mapOf(
        "杭州" to "330100",
        "北京" to "110101",
        "上海" to "310000",
        "广州" to "440100",
        "深圳" to "440300",
        "成都" to "510100"
    )

    // 当前选中的城市代码，默认为杭州
    private var currentCityCode = "330100"
    
    private lateinit var forecastAdapter: ForecastAdapter
    private val castsList = mutableListOf<CastInfo>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weather)

        // 返回按钮事件
        findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            finish()
        }

        initCityTabs()
        initRecyclerView()

        // 初始加载默认城市天气
        fetchWeatherForecast(currentCityCode)
    }

    /**
     * 初始化预报列表 RecyclerView
     */
    private fun initRecyclerView() {
        val rvForecast = findViewById<RecyclerView>(R.id.rv_forecast)
        forecastAdapter = ForecastAdapter(castsList)
        rvForecast.layoutManager = LinearLayoutManager(this)
        rvForecast.adapter = forecastAdapter
    }

    /**
     * 初始化城市切换 Tab
     * 动态向 LinearLayout 添加 TextView 作为 Tab
     */
    private fun initCityTabs() {
        val cityContainer = findViewById<LinearLayout>(R.id.ll_city_container)
        cityContainer.removeAllViews()

        for ((cityName, cityCode) in cityMap) {
            val textView = TextView(this)
            textView.text = cityName
            textView.setTextColor(Color.WHITE)
            textView.textSize = 14f
            textView.setPadding(32, 16, 32, 16)
            textView.gravity = Gravity.CENTER
            
            // 半透明背景
            textView.setBackgroundColor(Color.parseColor("#33FFFFFF"))
            
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 16, 0)
            textView.layoutParams = params

            // 点击切换城市
            textView.setOnClickListener {
                if (currentCityCode != cityCode) {
                    currentCityCode = cityCode
                    fetchWeatherForecast(cityCode)
                    Toast.makeText(this, "切换到 $cityName", Toast.LENGTH_SHORT).show()
                }
            }

            cityContainer.addView(textView)
        }
    }

    /**
     * 获取天气预报数据
     * 使用 AMAP API (extensions=all 获取预报信息)
     */
    private fun fetchWeatherForecast(cityCode: String) {
        val url = "https://restapi.amap.com/v3/weather/weatherInfo?city=$cityCode&extensions=all&key=1acb9f2e4e024e47a6b0f776eb5c689f"

        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@WeatherActivity, "预报获取失败", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val jsonStr = response.body?.string()
                    if (jsonStr != null) {
                        try {
                            val forecastResponse = gson.fromJson(jsonStr, ForecastResponse::class.java)
                            runOnUiThread {
                                updateUI(forecastResponse)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        })
    }

    /**
     * 更新界面显示
     * 包括：头部当前天气详情、背景图、图标、未来预报列表
     */
    private fun updateUI(data: ForecastResponse) {
        val forecast = data.forecasts?.firstOrNull() ?: return
        
        findViewById<TextView>(R.id.tv_detail_city).text = forecast.city
        
        // 获取当天的预报作为“当前”状态展示
        val today = forecast.casts?.firstOrNull()
        if (today != null) {
            findViewById<TextView>(R.id.tv_detail_temp).text = "${today.daytemp}°"
            findViewById<TextView>(R.id.tv_detail_weather).text = today.dayweather

            // 填充详细数据：风力和温差
            findViewById<TextView>(R.id.tv_detail_wind).text = "${today.daywind}风 ${today.daypower}级"
            findViewById<TextView>(R.id.tv_detail_range).text = "${today.nighttemp}° / ${today.daytemp}°"

            val rootLayout = findViewById<LinearLayout>(R.id.root_layout)
            val weather = today.dayweather
            val ivDetailIcon = findViewById<ImageView>(R.id.iv_detail_icon)
            
            // 根据 reporttime 判断当前时间是否为夜晚 (18:00 - 6:00)
            var isNight = false
            try {
                // 示例格式: "2025-05-20 16:23:35"
                val parts = forecast.reporttime.split(" ")
                if (parts.size > 1) {
                    val timeStr = parts[1]
                    val hour = timeStr.split(":")[0].toInt()
                    if (hour >= 18 || hour < 6) {
                        isNight = true
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 动态设置背景颜色
            val bgRes = when {
                weather.contains("雨") || weather.contains("雪") -> R.drawable.bg_rainy
                weather.contains("阴") || weather.contains("云") -> R.drawable.bg_cloudy
                else -> R.drawable.bg_sunny
            }
            rootLayout.setBackgroundResource(bgRes)

            // 动态设置天气图标 (支持昼夜区分)
             when {
                weather.contains("雨") -> ivDetailIcon.setImageResource(R.drawable.w_moderaterain)
                weather.contains("雪") -> ivDetailIcon.setImageResource(R.drawable.w_snow)
                weather.contains("云") -> ivDetailIcon.setImageResource(if (isNight) R.drawable.w_cloudynight else R.drawable.w_cloudylight)
                weather.contains("阴") -> ivDetailIcon.setImageResource(R.drawable.w_overcastsky)
                weather.contains("雾") -> ivDetailIcon.setImageResource(R.drawable.w_fog)
                weather.contains("晴") -> ivDetailIcon.setImageResource(if (isNight) R.drawable.w_sunny_night else R.drawable.w_sunnylight)
                else -> ivDetailIcon.setImageResource(if (isNight && weather.contains("晴")) R.drawable.w_sunny_night else R.drawable.w_sunnylight)
            }
        }

        // 刷新预报列表数据
        castsList.clear()
        forecast.casts?.let { castsList.addAll(it) }
        forecastAdapter.notifyDataSetChanged()
    }
}

/**
 * 天气预报列表适配器
 */
class ForecastAdapter(private val forecastList: List<CastInfo>) : RecyclerView.Adapter<ForecastAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tv_date)
        val tvWeather: TextView = view.findViewById(R.id.tv_weather_text)
        val tvTemp: TextView = view.findViewById(R.id.tv_temp_range)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_forecast, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val cast = forecastList[position]
        
        val weekText = when(cast.week) {
            "1" -> "周一"
            "2" -> "周二"
            "3" -> "周三"
            "4" -> "周四"
            "5" -> "周五"
            "6" -> "周六"
            "7" -> "周日"
            else -> "周${cast.week}"
        }

        holder.tvDate.text = "${cast.date}\n$weekText"
        holder.tvWeather.text = "${cast.dayweather}\n${cast.daywind}风 ${cast.daypower}级"
        holder.tvTemp.text = "${cast.daytemp}° / ${cast.nighttemp}°"
    }

    override fun getItemCount() = forecastList.size
}

// --- 数据模型定义 ---

data class ForecastResponse(
    val status: String,
    val forecasts: List<ForecastInfo>?
)

data class ForecastInfo(
    val city: String,
    val reporttime: String, // 发布时间
    val casts: List<CastInfo>?
)

data class CastInfo(
    val date: String,
    val week: String,
    val dayweather: String,
    val nightweather: String,
    val daytemp: String,
    val nighttemp: String,
    val daywind: String,
    val nightwind: String,
    val daypower: String,
    val nightpower: String
)
package com.example.helloworldapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import okhttp3.*
import java.io.IOException
import java.util.Random

/**
 * 首页 Activity
 * 集成了插件式 Feed 卡片系统
 */
class HomeActivity : AppCompatActivity() {

    private val client = OkHttpClient()
    private val gson = Gson()
    
    private lateinit var feedAdapter: FeedAdapter
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var fabBackTop: FloatingActionButton
    
    // Feed 数据源 (使用 BaseFeedData 接口)
    private var feedDataList = mutableListOf<Feedable>()
    
    // 分页控制变量
    private var currentPage = 1
    private var isLoading = false
    private val pageSize = 10
    private var isLastPage = false

    // 模拟数据池
    private val contentPool = listOf(
        "1111111111111111",
        "22222222222222",
        "33333333333333",
        "44444444444444444444",
        "555555555555555555555"
    )
    private val bannerResources = listOf(
        R.drawable.picture01, 
        R.drawable.picture02,
        R.drawable.picture03,
        R.drawable.picture04
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // 1. 注册所有卡片样式 (TextCard 已移除)
        registerCardStyles()

        initViews()
        initData()
    }

    private fun registerCardStyles() {
        // 注册图文卡片
        CardStyleRegistry.register("image_card", ImageCardProcessor {
            Toast.makeText(this, "点击了图文卡片，准备进入详情页", Toast.LENGTH_SHORT).show()
        })

        // 注册文章卡片
        CardStyleRegistry.register("article_card", ArticleCardProcessor())

        // 注册视频卡片
        CardStyleRegistry.register("video_card", VideoCardProcessor())

        // 注册广告卡片
        CardStyleRegistry.register("ad_card", AdCardProcessor())
    }

    private fun initViews() {
        swipeRefreshLayout = findViewById(R.id.swipe_refresh)
        fabBackTop = findViewById(R.id.fab_back_top)

        setupFeedList()
        setupBottomNavigation()
        
        swipeRefreshLayout.setOnRefreshListener {
            fetchWeatherData()
            loadFeedData(isRefresh = true)
        }

        fabBackTop.setOnClickListener {
            val rvFeed = findViewById<RecyclerView>(R.id.rv_feed)
            val layoutManager = rvFeed.layoutManager as LinearLayoutManager
            val currentPos = layoutManager.findFirstVisibleItemPosition()

            if (currentPos > 20) {
                rvFeed.scrollToPosition(10)
                rvFeed.post { rvFeed.smoothScrollToPosition(0) }
            } else {
                rvFeed.smoothScrollToPosition(0)
            }
            Toast.makeText(this, "已回到顶部", Toast.LENGTH_SHORT).show()
        }

        findViewById<ImageView>(R.id.search_button).setOnClickListener {
            Toast.makeText(this, "搜索功能开发中...", Toast.LENGTH_SHORT).show()
        }

        findViewById<ImageView>(R.id.switch_button).setOnClickListener {
            Toast.makeText(this, "切换功能开发中...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initData() {
        fetchWeatherData()
        loadFeedData(isRefresh = true)
    }

    private fun setupFeedList() {
        val rvFeed = findViewById<RecyclerView>(R.id.rv_feed)
        val layoutManager = LinearLayoutManager(this)
        rvFeed.layoutManager = layoutManager
        
        // FeedAdapter 的构造参数 onHeaderClick 仅用于 Header (天气卡片)
        feedAdapter = FeedAdapter(feedDataList) {
            startActivity(Intent(this, WeatherActivity::class.java))
        }
        rvFeed.adapter = feedAdapter

        rvFeed.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                if (firstVisibleItemPosition > 5) {
                    if (fabBackTop.visibility != View.VISIBLE) fabBackTop.show()
                } else {
                    if (fabBackTop.visibility == View.VISIBLE) fabBackTop.hide()
                }

                if (!isLoading && !isLastPage) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                        && firstVisibleItemPosition >= 0
                        && totalItemCount >= pageSize) {
                        loadFeedData(isRefresh = false)
                    }
                }
            }
        })
    }

    private fun setupBottomNavigation() {
        findViewById<LinearLayout>(R.id.tab_home).setOnClickListener {
            Toast.makeText(this, "已经在首页", Toast.LENGTH_SHORT).show()
        }
        findViewById<LinearLayout>(R.id.tab_feed).setOnClickListener {
            Toast.makeText(this, "发现功能开发中...", Toast.LENGTH_SHORT).show()
        }
        findViewById<LinearLayout>(R.id.tab_mine).setOnClickListener {
            val intent = Intent(this, UserInfoActivity::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }
    }

    private fun loadFeedData(isRefresh: Boolean) {
        if (isLoading) return
        isLoading = true

        if (!isRefresh) {
            feedAdapter.isFooterVisible = true
            // 通知 Adapter 插入了一行新数据（即 Footer）
            feedAdapter.notifyItemInserted(feedAdapter.itemCount - 1)
            // 让列表稍微滚一点，露出这个 Footer
            findViewById<RecyclerView>(R.id.rv_feed).smoothScrollToPosition(feedAdapter.itemCount - 1)
        }

        if (isRefresh) {
            currentPage = 1
            isLastPage = false
        }

        Handler(Looper.getMainLooper()).postDelayed({
            val newItems = mutableListOf<Feedable>()
            val random = Random()

            if (currentPage > 100) {
                // End of data
            } else {
                for (i in 0 until pageSize) {
                    // 随机生成卡片类型
                    val cardType = random.nextInt(100)
                    val item: Feedable = when {
                        // 40% 概率：图文卡片
                        cardType < 40 -> { 
                            val imageData = ImageCardData(
                                author = "用户 ${random.nextInt(1000)}",
                                time = "${random.nextInt(24)}小时前",
                                content = contentPool.random(),
                                imageSource = bannerResources.random()
                            )
                            object : Feedable {
                                override val styleId = "image_card"
                                override val data = gson.toJsonTree(imageData)
                            }
                        }
                        // 30% 概率：文章卡片
                        cardType < 70 -> { 
                            val articleData = ArticleCardData(
                                title = "标题标题标题标题",
                                summary = "内容内容内内容内容",
                                time = "${random.nextInt(24)}小时前",
                                author = "作者作者作者",
                            )
                            object : Feedable {
                                override val styleId = "article_card"
                                override val data = gson.toJsonTree(articleData)
                            }
                        }
                        // 20% 概率：视频卡片
                        cardType < 90 -> { 
                            val videoData = VideoCardData(
                                title = "Android 14 新特性上手体验",
                                duration = String.format("%02d:%02d", random.nextInt(10), random.nextInt(60)),
                                coverImage = bannerResources.random(),
                                author = "用户 ${random.nextInt(1000)}"
                            )
                            object : Feedable {
                                override val styleId = "video_card"
                                override val data = gson.toJsonTree(videoData)
                            }
                        }
                        // 10% 概率：广告卡片
                        else -> { 
                            val adData = AdCardData(
                                title = "夏季促销盛典",
                                desc = "全场五折起，限时抢购！",
                                coverImage = bannerResources.random(),
                                buttonText = "立即查看"
                            )
                            object : Feedable {
                                override val styleId = "ad_card"
                                override val data = gson.toJsonTree(adData)
                            }
                        }
                    }
                    newItems.add(item)
                }
            }

            if (feedAdapter.isFooterVisible) {
                val footerPos = feedAdapter.itemCount - 1
                feedAdapter.isFooterVisible = false
                // 通知 Adapter 这一行被移除了
                feedAdapter.notifyItemRemoved(footerPos)
            }

            if (isRefresh) {
                feedDataList.clear()
                feedAdapter.notifyDataSetChanged()
            }

            if (newItems.isEmpty()) {
                isLastPage = true
                Toast.makeText(this, "没有更多数据了", Toast.LENGTH_SHORT).show()
            } else {
                val startPos = feedDataList.size + 1 
                feedDataList.addAll(newItems)
                if (isRefresh) {
                    feedAdapter.notifyDataSetChanged()
                } else {
                    feedAdapter.notifyItemRangeInserted(startPos, newItems.size)
                    Toast.makeText(this, "加载了 ${newItems.size} 条更多数据", Toast.LENGTH_SHORT).show()
                }
                currentPage++
            }

            isLoading = false
            swipeRefreshLayout.isRefreshing = false
            if (isRefresh) {
                Toast.makeText(this, "刷新成功", Toast.LENGTH_SHORT).show()
            }

        }, 1000)
    }

    private fun fetchWeatherData() {
        val url = "https://restapi.amap.com/v3/weather/weatherInfo?city=330114&extensions=base&key=1acb9f2e4e024e47a6b0f776eb5c689f"
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    response.body?.string()?.let { jsonStr ->
                        try {
                            val weatherResponse = gson.fromJson(jsonStr, WeatherResponse::class.java)
                            weatherResponse.lives?.firstOrNull()?.let { lives ->
                                runOnUiThread {
                                    feedAdapter.updateHeader(lives)
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        })
    }
}

/**
 * 重构后的 FeedAdapter
 * 基于插件式卡片系统，动态处理不同类型的卡片
 */
class FeedAdapter(
    private val feedList: List<Feedable>,
    private val onHeaderClick: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        // 0 留给 Header，其他正整数留给 styleId 的 hash
        const val TYPE_HEADER = 0
        const val TYPE_FOOTER = 999
    }
    var isFooterVisible = false

    private var weatherData: LiveWeather? = null

    fun updateHeader(data: LiveWeather) {
        weatherData = data
        notifyItemChanged(0)
    }

    override fun getItemViewType(position: Int): Int {
        if (position == 0) return TYPE_HEADER

        if (isFooterVisible && position == itemCount - 1) {
            return TYPE_FOOTER
        }

        // 获取数据项
        val item = feedList[position - 1]
        // 根据 styleId 获取对应的 ViewType (int)
        return CardStyleRegistry.styleIdToViewType(item.styleId)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_home_header, parent, false)
            return HeaderViewHolder(view)
        }

        if (viewType == TYPE_FOOTER) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_footer_loading, parent, false)
            return FooterViewHolder(view)
        }

        // 根据 ViewType 查找对应的 styleId
        val styleId = CardStyleRegistry.viewTypeToStyleId(viewType)
        if (styleId != null) {
            val processor = CardStyleRegistry.getProcessor(styleId)
            if (processor != null) {
                return processor.createViewHolder(parent)
            }
        }

        // 兜底逻辑
        val emptyView = View(parent.context)
        return object : RecyclerView.ViewHolder(emptyView) {}
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val viewType = getItemViewType(position)

        if (viewType == TYPE_HEADER) {
            (holder as HeaderViewHolder).bind(weatherData, onHeaderClick)
        } else if (viewType == TYPE_FOOTER) {
            // Footer 不需要绑定数据，直接跳过
            // 如果 Footer 有需要更新的状态（比如显示“加载失败”），可以在这里处理
        } else {
            // 只有普通的 Feed Item 才执行这里的逻辑
            // 注意：position - 1 是因为第 0 项是 Header
            if (position - 1 < feedList.size) {
                val item = feedList[position - 1]
                val processor = CardStyleRegistry.getProcessor(item.styleId)
                processor?.bindViewHolder(holder, item)
            }
        }
    }

    override fun getItemCount(): Int {
        var count = feedList.size + 1 // Header + List
        if (isFooterVisible) {
            count += 1
        }
        return count
    }

    class FooterViewHolder(view: View) : RecyclerView.ViewHolder(view)

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvCity: TextView = view.findViewById(R.id.tv_city)
        private val tvTemp: TextView = view.findViewById(R.id.tv_temperature)
        private val tvWeather: TextView = view.findViewById(R.id.tv_weather)
        private val ivIcon: ImageView = view.findViewById(R.id.iv_weather_icon)
        private val clBg: ConstraintLayout = view.findViewById(R.id.cl_weather_bg)
        private val cardView: CardView = view.findViewById(R.id.cv_weather_widget)

        fun bind(data: LiveWeather?, onHeaderClick: () -> Unit) {
            cardView.setOnClickListener { onHeaderClick() }
            if (data == null) return

            tvCity.text = data.city
            tvTemp.text = "${data.temperature}°C"
            tvWeather.text = "${data.weather} | 风向:${data.winddirection}"

            var isNight = false
            try {
                val parts = data.reporttime.split(" ")
                if (parts.size > 1) {
                    val hour = parts[1].split(":")[0].toInt()
                    if (hour >= 18 || hour < 6) isNight = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            updateWeatherStyle(data.weather, isNight)
        }

        private fun updateWeatherStyle(weather: String, isNight: Boolean) {
            val bgRes = when {
                weather.contains("雨") -> R.drawable.bg_rainy
                weather.contains("云") || weather.contains("阴") -> R.drawable.bg_cloudy
                else -> R.drawable.bg_sunny
            }
            clBg.setBackgroundResource(bgRes)

            val iconRes = when {
                weather.contains("雨") -> R.drawable.w_moderaterain
                weather.contains("雪") -> R.drawable.w_snow
                weather.contains("云") -> if (isNight) R.drawable.w_cloudynight else R.drawable.w_cloudylight
                weather.contains("阴") -> R.drawable.w_overcastsky
                weather.contains("雾") -> R.drawable.w_fog
                weather.contains("晴") -> if (isNight) R.drawable.w_sunny_night else R.drawable.w_sunnylight
                else -> if (isNight) R.drawable.w_sunny_night else R.drawable.w_sunnylight
            }
            ivIcon.setImageResource(iconRes)
        }
    }
}
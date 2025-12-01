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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import okhttp3.*
import java.io.IOException
import java.util.Random

/**
 * 首页 Activity
 * 包含功能：
 * 1. 顶部天气卡片展示（作为 RecyclerView 的 Header）
 * 2. Feed 流列表展示（模拟数据，支持分页）
 * 3. 下拉刷新与无限加载更多
 * 4. 底部导航栏跳转
 * 5. 长按删除卡片
 */
class HomeActivity : AppCompatActivity() {

    private val client = OkHttpClient()
    private val gson = Gson()
    
    private lateinit var feedAdapter: FeedAdapter
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var fabBackTop: FloatingActionButton
    
    // 列表数据源
    private var feedDataList = mutableListOf<FeedItem>()
    
    // 分页控制变量
    private var currentPage = 1
    private var isLoading = false
    private val pageSize = 10
    private var isLastPage = false

    // 模拟数据池（文案、图片）
    private val contentPool = listOf(
        "今天天气真不错，适合出去散步！",
        "刚刚拍了一张照片，大家觉得怎么样？",
        "工作终于完成了，开心！",
        "这里风景很好，推荐大家来玩。",
        "生活不止眼前的苟且，还有诗和远方。",
        "打卡网红店，味道一般般。",
        "又是元气满满的一天，加油！",
        "晚上吃什么呢？好纠结。"
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

        initViews()
        initData()
    }

    /**
     * 初始化视图组件及事件监听
     */
    private fun initViews() {
        swipeRefreshLayout = findViewById(R.id.swipe_refresh)
        fabBackTop = findViewById(R.id.fab_back_top)

        setupFeedList()
        setupBottomNavigation()
        
        // 配置下拉刷新监听
        swipeRefreshLayout.setOnRefreshListener {
            fetchWeatherData()
            loadFeedData(isRefresh = true)
        }

        // 配置回到顶部按钮点击事件
        fabBackTop.setOnClickListener {
            val rvFeed = findViewById<RecyclerView>(R.id.rv_feed)
            val layoutManager = rvFeed.layoutManager as LinearLayoutManager
            val currentPos = layoutManager.findFirstVisibleItemPosition()

            // 如果列表滑动距离较远 (>20)，则先跳转到较近位置再平滑滚动，提升体验
            if (currentPos > 20) {
                rvFeed.scrollToPosition(10)
                rvFeed.post { rvFeed.smoothScrollToPosition(0) }
            } else {
                rvFeed.smoothScrollToPosition(0)
            }
            Toast.makeText(this, "已回到顶部", Toast.LENGTH_SHORT).show()
        }

        // 顶部 Toolbar 按钮点击事件
        findViewById<ImageView>(R.id.search_button).setOnClickListener {
            Toast.makeText(this, "点击了搜索", Toast.LENGTH_SHORT).show()
        }

        findViewById<ImageView>(R.id.switch_button).setOnClickListener {
            Toast.makeText(this, "已切换双列模式", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 初始化数据加载
     */
    private fun initData() {
        fetchWeatherData() // 获取天气数据
        loadFeedData(isRefresh = true) // 获取第一页 Feed 数据
    }

    /**
     * 配置 Feed 列表 (RecyclerView)
     */
    private fun setupFeedList() {
        val rvFeed = findViewById<RecyclerView>(R.id.rv_feed)
        val layoutManager = LinearLayoutManager(this)
        rvFeed.layoutManager = layoutManager
        
        // 初始化 Adapter，传入数据源、头部点击回调、以及长按删除回调
        feedAdapter = FeedAdapter(
            feedList = feedDataList,
            onHeaderClick = {
                startActivity(Intent(this, WeatherActivity::class.java))
            },
            onItemLongClick = { position ->
                showDeleteConfirmDialog(position)
            }
        )
        rvFeed.adapter = feedAdapter

        // 添加滚动监听，实现“加载更多”及“回到顶部按钮”的显示控制
        rvFeed.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                // 控制 FAB 显示/隐藏：下滑超过 5 个 Item 显示
                if (firstVisibleItemPosition > 5) {
                    if (fabBackTop.visibility != View.VISIBLE) fabBackTop.show()
                } else {
                    if (fabBackTop.visibility == View.VISIBLE) fabBackTop.hide()
                }

                // 触底自动加载更多逻辑
                if (!isLoading && !isLastPage) {
                    // 当可见区域 + 起始位置 >= 总条数，说明滑动到了底部
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                        && firstVisibleItemPosition >= 0
                        && totalItemCount >= pageSize) {
                        loadFeedData(isRefresh = false)
                    }
                }
            }
        })
    }

    /**
     * 显示删除确认对话框
     */
    private fun showDeleteConfirmDialog(adapterPosition: Int) {
        AlertDialog.Builder(this)
            .setTitle("删除提示")
            .setMessage("确定要删除这条内容吗？")
            .setPositiveButton("删除") { _, _ ->
                // Adapter位置需要减去 Header 数量 (1) 才是数据源索引
                val dataIndex = adapterPosition - 1
                if (dataIndex >= 0 && dataIndex < feedDataList.size) {
                    feedDataList.removeAt(dataIndex)
                    feedAdapter.notifyItemRemoved(adapterPosition)
                    // 刷新后续 Item 的位置索引，防止连续删除时位置错乱
                    feedAdapter.notifyItemRangeChanged(adapterPosition, feedDataList.size - dataIndex)
                    Toast.makeText(this, "删除成功", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
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

    /**
     * 模拟加载 Feed 数据
     * @param isRefresh true 为下拉刷新（重置第一页），false 为加载更多（追加下一页）
     */
    private fun loadFeedData(isRefresh: Boolean) {
        if (isLoading) return
        isLoading = true

        if (isRefresh) {
            currentPage = 1
            isLastPage = false
        }

        // 模拟网络请求延迟 1秒
        Handler(Looper.getMainLooper()).postDelayed({
            val newItems = mutableListOf<FeedItem>()
            val random = Random()

            // 模拟最多加载 100 页数据
            if (currentPage > 100) {
                // 数据加载完毕
            } else {
                // 随机生成一页数据
                val startIndex = (currentPage - 1) * pageSize
                for (i in 0 until pageSize) {
                    val userIndex = startIndex + i + 1
                    newItems.add(
                        FeedItem(
                            author = String.format("user%02d", userIndex),
                            time = "${random.nextInt(24)}小时前",
                            content = contentPool.random(),
                            imageSource = bannerResources.random()
                        )
                    )
                }
            }

            // 更新 UI 数据
            if (isRefresh) {
                feedDataList.clear()
                feedAdapter.notifyDataSetChanged()
            }

            if (newItems.isEmpty()) {
                isLastPage = true
                Toast.makeText(this, "没有更多数据了", Toast.LENGTH_SHORT).show()
            } else {
                val startPos = feedDataList.size + 1 // +1 是因为有 Header
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

    /**
     * 请求天气 API 获取实时天气数据
     */
    private fun fetchWeatherData() {
        val url = "https://restapi.amap.com/v3/weather/weatherInfo?city=330114&extensions=base&key=1acb9f2e4e024e47a6b0f776eb5c689f"
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // 网络请求失败处理
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    response.body?.string()?.let { jsonStr ->
                        try {
                            val weatherResponse = gson.fromJson(jsonStr, WeatherResponse::class.java)
                            weatherResponse.lives?.firstOrNull()?.let { lives ->
                                runOnUiThread {
                                    // 更新 Adapter 中的 Header 数据
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
 * Feed 列表项数据模型
 * @param imageSource 支持 Int (资源ID) 或 String (网络URL)
 */
data class FeedItem(
    val author: String,
    val time: String,
    val content: String,
    val imageSource: Any 
)

/**
 * Feed 列表适配器
 * 支持多布局类型：Header (天气卡片) + Item (Feed内容)
 */
class FeedAdapter(
    private val feedList: List<FeedItem>,
    private val onHeaderClick: () -> Unit,
    private val onItemLongClick: (Int) -> Unit // 新增：长按回调
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_ITEM = 1
    }

    private var weatherData: LiveWeather? = null

    fun updateHeader(data: LiveWeather) {
        weatherData = data
        notifyItemChanged(0) // 仅刷新 Header 位置
    }

    override fun getItemViewType(position: Int): Int {
        // 第一个位置显示 Header
        return if (position == 0) TYPE_HEADER else TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            HeaderViewHolder(inflater.inflate(R.layout.item_home_header, parent, false))
        } else {
            ItemViewHolder(inflater.inflate(R.layout.item_feed_card, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == TYPE_HEADER) {
            (holder as HeaderViewHolder).bind(weatherData, onHeaderClick)
        } else {
            // Feed 数据索引需要 -1 (减去 Header 占位)
            (holder as ItemViewHolder).bind(feedList[position - 1], onItemLongClick)
        }
    }

    override fun getItemCount() = feedList.size + 1

    /**
     * 头部 ViewHolder (天气卡片)
     */
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

            // 判断当前时间是否为夜晚 (18:00 - 6:00)
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

        /**
         * 根据天气状况和时间更新背景及图标
         */
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

    /**
     * 列表项 ViewHolder (Feed 卡片)
     */
    class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvAuthor: TextView = view.findViewById(R.id.tv_author)
        private val tvTime: TextView = view.findViewById(R.id.tv_time)
        private val tvContent: TextView = view.findViewById(R.id.tv_content)
        private val ivContentImage: ImageView = view.findViewById(R.id.iv_content_image)
        
        private val btnLike: LinearLayout = view.findViewById(R.id.ll_like)
        private val btnComment: LinearLayout = view.findViewById(R.id.ll_comment)
        private val btnShare: LinearLayout = view.findViewById(R.id.ll_share)

        fun bind(item: FeedItem, onItemLongClick: (Int) -> Unit) {
            tvAuthor.text = item.author
            tvTime.text = item.time
            tvContent.text = item.content
            
            // 使用 Glide 加载图片
            Glide.with(itemView.context)
                .load(item.imageSource)
                .placeholder(R.drawable.ic_menu_recent_history)
                .error(R.drawable.bg_cloudy)
                .into(ivContentImage)
                
            // 按钮点击事件
            btnLike.setOnClickListener {
                Toast.makeText(itemView.context, "点击点赞", Toast.LENGTH_SHORT).show()
            }
            
            btnComment.setOnClickListener {
                Toast.makeText(itemView.context, "点击评论", Toast.LENGTH_SHORT).show()
            }
            
            btnShare.setOnClickListener {
                Toast.makeText(itemView.context, "点击分享", Toast.LENGTH_SHORT).show()
            }

            // 长按删除监听
            itemView.setOnLongClickListener {
                onItemLongClick(adapterPosition)
                true // 消费长按事件
            }
        }
    }
}
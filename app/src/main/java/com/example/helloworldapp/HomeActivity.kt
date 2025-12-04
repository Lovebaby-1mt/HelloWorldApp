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
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import okhttp3.*
import java.io.IOException
import java.util.Random

/**
 * 首页 Activity (新版)
 * 集成功能：
 * 1. 插件式多类型 Feed 流 (图文、文章、视频、广告)
 * 2. 瀑布流(双列混排) / 列表流(单列) 全局切换
 * 3. 服务端控制卡片排版 (layoutType)
 * 4. 下拉刷新 & 上拉加载更多 (带底部 Footer)
 * 5. 顶部天气 & 长按删除
 */
class HomeActivity : AppCompatActivity() {

    private val client = OkHttpClient()
    private val gson = Gson()

    private lateinit var rvFeed: RecyclerView
    private lateinit var feedAdapter: FeedAdapter
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var fabBackTop: FloatingActionButton
    private lateinit var switchBtn: ImageView

    // 布局管理器
    private lateinit var staggeredLayoutManager: StaggeredGridLayoutManager // 瀑布流 (支持混排)
    private lateinit var linearLayoutManager: LinearLayoutManager       // 纯单列

    // 状态控制：false=单列, true=双列混排
    private var isGridMode = false

    // 数据源 (使用 Feedable 接口支持多类型)
    private var feedDataList = mutableListOf<Feedable>()

    // 分页控制
    private var currentPage = 1
    private var isLoading = false
    private val pageSize = 10
    private var isLastPage = false

    // 模拟资源
    private val contentPool = listOf(
        "11111111111111111111111",
        "2222222222222222222222222222",
        "3333333333333333333",
        "44444444444444444",
        "5555555555555555555555555"
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

        // 1. 注册所有卡片样式 (重要！必须先注册)
        registerCardStyles()

        initViews()
        initData()
    }

    /**
     * 注册卡片类型
     */
    /**
     * 注册卡片类型 (分别注册单列和双列版本)
     */
    private fun registerCardStyles() {
        // --- 1. 图文卡片 ---
        // 单列样式 (默认)
        CardStyleRegistry.register("image_card_list", ImageCardProcessor(
            layoutResId = R.layout.item_card_image,
            onImageCardClick = { Toast.makeText(this, "点击单列图文", Toast.LENGTH_SHORT).show() }
        ))
        // 双列样式 (Grid)
        CardStyleRegistry.register("image_card_grid", ImageCardProcessor(
            layoutResId = R.layout.item_card_image_grid,
            onImageCardClick = { Toast.makeText(this, "点击双列图文", Toast.LENGTH_SHORT).show() }
        ))

        // --- 2. 文章卡片 ---
        // 单列样式
        CardStyleRegistry.register("article_card_list", ArticleCardProcessor(R.layout.item_card_article))
        // 双列样式
        CardStyleRegistry.register("article_card_grid", ArticleCardProcessor(R.layout.item_card_article_grid))

        // --- 3. 视频卡片 ---
        // 单列样式
        CardStyleRegistry.register("video_card_list", VideoCardProcessor(R.layout.item_card_video))
        // 双列样式
        CardStyleRegistry.register("video_card_grid", VideoCardProcessor(R.layout.item_card_video_grid))

        // --- 4. 广告卡片 ---
        // 单列样式
        CardStyleRegistry.register("ad_card_list", AdCardProcessor(R.layout.item_card_ad))
        // 双列样式
        CardStyleRegistry.register("ad_card_grid", AdCardProcessor(R.layout.item_card_ad_grid))
    }

    private fun initViews() {
        rvFeed = findViewById(R.id.rv_feed)
        swipeRefreshLayout = findViewById(R.id.swipe_refresh)
        fabBackTop = findViewById(R.id.fab_back_top)
        switchBtn = findViewById(R.id.switch_button)

        setupFeedList()
        setupBottomNavigation()

        // 下拉刷新
        swipeRefreshLayout.setOnRefreshListener {
            fetchWeatherData()
            loadFeedData(isRefresh = true)
        }

        // 回到顶部 FAB
        fabBackTop.setOnClickListener {
            val firstPos = if (isGridMode) {
                val positions = IntArray(2)
                staggeredLayoutManager.findFirstVisibleItemPositions(positions)
                positions[0]
            } else {
                linearLayoutManager.findFirstVisibleItemPosition()
            }

            if (firstPos > 20) {
                rvFeed.scrollToPosition(10)
                rvFeed.post { rvFeed.smoothScrollToPosition(0) }
            } else {
                rvFeed.smoothScrollToPosition(0)
            }
        }

        // 搜索按钮
        findViewById<ImageView>(R.id.search_button).setOnClickListener {
            Toast.makeText(this, "搜索功能开发中...", Toast.LENGTH_SHORT).show()
        }

        // 切换布局按钮
        switchBtn.setOnClickListener {
            toggleLayoutMode()
        }
    }

    private fun initData() {
        fetchWeatherData()
        loadFeedData(isRefresh = true)
    }

    /**
     * 初始化 RecyclerView
     */
    private fun setupFeedList() {
        // 1. 初始化两种布局管理器
        staggeredLayoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        staggeredLayoutManager.gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_NONE // 防止跳动

        linearLayoutManager = LinearLayoutManager(this)

        // 2. 根据当前模式设置 LayoutManager
        rvFeed.layoutManager = if (isGridMode) staggeredLayoutManager else linearLayoutManager

        // 3. 初始化 Adapter
        feedAdapter = FeedAdapter(
            feedList = feedDataList,
            onHeaderClick = {
                startActivity(Intent(this, WeatherActivity::class.java))
            },
            // 传递长按删除回调
            onItemLongClick = { adapterPosition ->
                showDeleteConfirmDialog(adapterPosition)
            }
        )
        // 同步模式状态
        feedAdapter.isGridMode = isGridMode
        rvFeed.adapter = feedAdapter

        // 4. 滚动监听 (控制加载更多和 FAB)
        rvFeed.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                // 获取可见性信息
                val layoutManager = recyclerView.layoutManager
                var firstVisibleItemPosition = 0
                var visibleItemCount = 0
                var totalItemCount = 0

                if (layoutManager is StaggeredGridLayoutManager) {
                    val positions = IntArray(2)
                    layoutManager.findFirstVisibleItemPositions(positions)
                    firstVisibleItemPosition = positions[0]
                    visibleItemCount = layoutManager.childCount
                    totalItemCount = layoutManager.itemCount
                } else if (layoutManager is LinearLayoutManager) {
                    firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                    visibleItemCount = layoutManager.childCount
                    totalItemCount = layoutManager.itemCount
                }

                // FAB 显示控制
                if (firstVisibleItemPosition > 5) {
                    if (fabBackTop.visibility != View.VISIBLE) fabBackTop.show()
                } else {
                    if (fabBackTop.visibility == View.VISIBLE) fabBackTop.hide()
                }

                // 触底加载更多
                if (!isLoading && !isLastPage) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                        && firstVisibleItemPosition >= 0
                        && totalItemCount >= pageSize
                    ) {
                        loadFeedData(isRefresh = false)
                    }
                }
            }
        })
    }

    /**
     * 切换单列/双列布局模式
     */
    private fun toggleLayoutMode() {
        isGridMode = !isGridMode
        feedAdapter.isGridMode = isGridMode

        if (isGridMode) {
            rvFeed.layoutManager = staggeredLayoutManager
            // 假设你有 ic_header_list 图标，如果没有，请用 switch 图标暂代
            switchBtn.setImageResource(R.drawable.ic_header_switch)
            Toast.makeText(this, "切换为双列混排", Toast.LENGTH_SHORT).show()
        } else {
            rvFeed.layoutManager = linearLayoutManager
            switchBtn.setImageResource(R.drawable.ic_header_switch)
            Toast.makeText(this, "切换为单列列表", Toast.LENGTH_SHORT).show()
        }

        // 必须刷新 Adapter 以应用新的布局参数
        feedAdapter.notifyDataSetChanged()
    }

    /**
     * 删除确认对话框
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
                    // 刷新后续 Item 的位置索引
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
            startActivity(Intent(this, UserInfoActivity::class.java))
            overridePendingTransition(0, 0)
        }
    }

    /**
     * 加载数据 (模拟生成多种类型卡片)
     */
    /**
     * 加载数据 (模拟生成多种类型卡片，并根据 layoutType 分配 StyleID)
     */
    /**
     * 加载数据 (新版：严格根据全局模式分配样式)
     */
    private fun loadFeedData(isRefresh: Boolean) {
        if (isLoading) return
        isLoading = true

        if (!isRefresh) {            feedAdapter.isFooterVisible = true
            feedAdapter.notifyItemInserted(feedAdapter.itemCount - 1)
            rvFeed.smoothScrollToPosition(feedAdapter.itemCount - 1)
        }

        if (isRefresh) {
            currentPage = 1
            isLastPage = false
        }

        Handler(Looper.getMainLooper()).postDelayed({
            val newItems = mutableListOf<Feedable>()
            val random = Random()

            if (currentPage > 100) {
                // 到底了
            } else {
                repeat(pageSize) {
                    val cardType = random.nextInt(100)

                    // 【核心修改】不再随机，而是根据全局开关决定样式
                    // 如果是 Grid 模式，layoutType=1, style 后缀是 _grid
                    // 如果是 List 模式，layoutType=2, style 后缀是 _list
                    val currentLayoutType = if (isGridMode) 1 else 2
                    val suffix = if (isGridMode) "_grid" else "_list"

                    val item: Feedable = when {
                        // 40% 图文
                        cardType < 40 -> {
                            val imageData = ImageCardData(
                                author = "User ${random.nextInt(1000)}",
                                time = "${random.nextInt(24)}小时前",
                                content = contentPool.random(),
                                imageSource = bannerResources.random(),
                                layoutType = currentLayoutType // 传入当前模式的类型
                            )
                            object : Feedable {
                                override val styleId = "image_card$suffix"
                                override val data = gson.toJsonTree(imageData)
                            }
                        }
                        // 30% 文章
                        cardType < 70 -> {
                            val articleData = ArticleCardData(
                                title = "文章标题${random.nextInt(1000)}",
                                summary = "摘要摘要摘要摘要摘要",
                                time = "${random.nextInt(24)}小时前",
                                author = "User ${random.nextInt(1000)}",
                                layoutType = currentLayoutType
                            )
                            object : Feedable {
                                override val styleId = "article_card$suffix"
                                override val data = gson.toJsonTree(articleData)
                            }
                        }
                        // 20% 视频
                        cardType < 90 -> {
                            val videoData = VideoCardData(
                                title = "视频${random.nextInt(1000)}",
                                duration = String.format("%02d:%02d", random.nextInt(10), random.nextInt(60)),
                                coverImage = bannerResources.random(),
                                author = "User ${random.nextInt(1000)}",
                                time = "${random.nextInt(24)}小时前",
                                layoutType = currentLayoutType
                            )
                            object : Feedable {
                                override val styleId = "video_card$suffix"
                                override val data = gson.toJsonTree(videoData)
                            }
                        }
                        // 10% 广告
                        else -> {
                            val adData = AdCardData(
                                title = "广告主标题",
                                desc = "广告副标题",
                                coverImage = bannerResources.random(),
                                buttonText = "感兴趣",
                                layoutType = currentLayoutType
                            )
                            object : Feedable {
                                override val styleId = "ad_card$suffix"
                                override val data = gson.toJsonTree(adData)
                            }
                        }
                    }
                    newItems.add(item)
                }
            }

            // 数据回来，移除 Footer
            if (feedAdapter.isFooterVisible) {
                val footerPos = feedAdapter.itemCount - 1
                feedAdapter.isFooterVisible = false
                feedAdapter.notifyItemRemoved(footerPos)
            }

            if (isRefresh) {
                feedDataList.clear()
                feedDataList.addAll(newItems)
                feedAdapter.notifyDataSetChanged()
                Toast.makeText(this, "刷新成功", Toast.LENGTH_SHORT).show()
            } else {
                if (newItems.isEmpty()) {
                    isLastPage = true
                    Toast.makeText(this, "没有更多数据了", Toast.LENGTH_SHORT).show()
                } else {
                    val startPos = feedDataList.size + 1
                    feedDataList.addAll(newItems)
                    feedAdapter.notifyItemRangeInserted(startPos, newItems.size)
                }
            }

            currentPage++
            isLoading = false
            swipeRefreshLayout.isRefreshing = false

        }, 1000)
    }


    private fun fetchWeatherData() {
        // 请使用你自己的 Key
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
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                }
            }
        })
    }
}

/**
 * 适配器：支持 Header + Footer + 多类型卡片 + 瀑布流混排 + 长按删除
 */
class FeedAdapter(
    private val feedList: List<Feedable>,
    private val onHeaderClick: () -> Unit,
    private val onItemLongClick: (Int) -> Unit // 长按回调
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        // 0 留给 Header，其他正整数留给 styleId 的 hash
        const val TYPE_HEADER = 0
        const val TYPE_FOOTER = 999
    }
    var isFooterVisible = false

    var isGridMode = false // 模式开关
    var isFooterVisible = false
    private var weatherData: LiveWeather? = null

    fun updateHeader(data: LiveWeather) {
        weatherData = data
        notifyItemChanged(0)
    }

    override fun getItemViewType(position: Int): Int {
        if (position == 0) return TYPE_HEADER
        if (isFooterVisible && position == itemCount - 1) return TYPE_FOOTER

        val item = feedList[position - 1]
        var finalStyleId = item.styleId

        // 【关键】如果全局处于【单列模式】，强制使用 List 样式的卡片
        if (!isGridMode) {
            if (finalStyleId.endsWith("_grid")) {
                finalStyleId = finalStyleId.replace("_grid", "_list")
            }
        } else {
            // 【关键】如果全局处于【双列模式】，强制使用 Grid 样式的卡片
            if (finalStyleId.endsWith("_list")) {
                finalStyleId = finalStyleId.replace("_list", "_grid")
            }
        }

        return CardStyleRegistry.styleIdToViewType(finalStyleId)
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

        // 使用正确的 styleId 获取处理器
        val styleId = CardStyleRegistry.viewTypeToStyleId(viewType)
        if (styleId != null) {
            val processor = CardStyleRegistry.getProcessor(styleId)
            if (processor != null) {
                return processor.createViewHolder(parent)
            }
        }
        // 兜底，防止崩溃
        return object : RecyclerView.ViewHolder(View(parent.context)) {}
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        // 根据 position 获取当前应该是什么 ViewType
        val viewType = getItemViewType(position)

        if (viewType == TYPE_HEADER) {
            (holder as HeaderViewHolder).bind(weatherData, onHeaderClick)
        } else if (viewType == TYPE_FOOTER) {

        } else {
            // 确保 position 是有效的，防止越界
            if (position - 1 < feedList.size) {
                val item = feedList[position - 1]

                val styleId = CardStyleRegistry.viewTypeToStyleId(viewType)

                if (styleId != null) {
                    val processor = CardStyleRegistry.getProcessor(styleId)
                    processor?.bindViewHolder(holder, item, onItemLongClick)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        var count = feedList.size + 1
        if (isFooterVisible) count += 1
        return count
    }

    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        super.onViewAttachedToWindow(holder)

        val lp = holder.itemView.layoutParams
        if (lp is StaggeredGridLayoutManager.LayoutParams) {
            val position = holder.bindingAdapterPosition
            if (position == RecyclerView.NO_POSITION) return

            val viewType = getItemViewType(position)

            if (viewType == TYPE_HEADER || viewType == TYPE_FOOTER) {
                lp.isFullSpan = true
                return
            }

            lp.isFullSpan = !isGridMode
        }
    }

    // Footer ViewHolder
    class FooterViewHolder(view: View) : RecyclerView.ViewHolder(view)

    // Header ViewHolder
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

            // 简单的天气图标逻辑
            val isRainy = data.weather.contains("雨")
            val isCloudy = data.weather.contains("云") || data.weather.contains("阴")

            if (isRainy) {
                clBg.setBackgroundResource(R.drawable.bg_rainy)
                ivIcon.setImageResource(R.drawable.w_moderaterain)
            } else if (isCloudy) {
                clBg.setBackgroundResource(R.drawable.bg_cloudy)
                ivIcon.setImageResource(R.drawable.w_cloudylight)
            } else {
                clBg.setBackgroundResource(R.drawable.bg_sunny)
                ivIcon.setImageResource(R.drawable.w_sunnylight)
            }
        }
    }
}

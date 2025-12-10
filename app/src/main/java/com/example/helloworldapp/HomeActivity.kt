package com.example.helloworldapp

import android.content.Intent
import android.os.Bundle
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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.*
import java.io.IOException
import java.util.Random

class HomeActivity : AppCompatActivity() {

    companion object {
        private const val PAGE_SIZE = 20
        private const val MAX_PAGES = 5
    }

    private val client by lazy { OkHttpClient() }
    private val gson by lazy { Gson() }
    private val random = Random()

    private lateinit var rvFeed: RecyclerView
    private lateinit var feedAdapter: FeedAdapter
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var fabBackTop: FloatingActionButton
    private lateinit var switchBtn: ImageView

    private lateinit var staggeredLayoutManager: StaggeredGridLayoutManager
    private lateinit var linearLayoutManager: LinearLayoutManager

    private var isGridMode = false
    private var currentPage = 1
    private var isLoading = false
    private var isLastPage = false

    private var feedDataList = mutableListOf<Feedable>()
    private var exposureManager: FeedExposureManager? = null

    private val scrollPositions = IntArray(2)

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

        registerCardStyles()
        initViews()
        initData()
    }

    override fun onResume() {
        super.onResume()
        exposureManager?.onResume() // 恢复曝光检测
    }

    override fun onPause() {
        super.onPause()
        exposureManager?.onPause() // 暂停曝光检测
    }

    override fun onDestroy() {
        super.onDestroy()
        exposureManager?.detach()
    }

    private fun registerCardStyles() {
        CardStyleRegistry.register("image_card_list", ImageCardProcessor(R.layout.item_card_image) {
            showToast("点击单列图文")
        })
        CardStyleRegistry.register("image_card_grid", ImageCardProcessor(R.layout.item_card_image_grid) {
            showToast("点击双列图文")
        })
        CardStyleRegistry.register("article_card_list", ArticleCardProcessor(R.layout.item_card_article))
        CardStyleRegistry.register("article_card_grid", ArticleCardProcessor(R.layout.item_card_article_grid))
        CardStyleRegistry.register("video_card_list", VideoCardProcessor(R.layout.item_card_video))
        CardStyleRegistry.register("video_card_grid", VideoCardProcessor(R.layout.item_card_video_grid))
        CardStyleRegistry.register("ad_card_list", AdCardProcessor(R.layout.item_card_ad))
        CardStyleRegistry.register("ad_card_grid", AdCardProcessor(R.layout.item_card_ad_grid))
    }

    private fun initViews() {
        rvFeed = findViewById(R.id.rv_feed)
        swipeRefreshLayout = findViewById(R.id.swipe_refresh)
        fabBackTop = findViewById(R.id.fab_back_top)
        switchBtn = findViewById(R.id.switch_button)

        setupFeedList()
        setupBottomNavigation()

        swipeRefreshLayout.setOnRefreshListener {
            loadFeedData(isRefresh = true)
            fetchWeatherData() // 刷新时也更新天气
        }

        fabBackTop.setOnClickListener {
            rvFeed.stopScroll()
            val firstPos = getFirstVisiblePosition()
            if (firstPos > 20) {
                rvFeed.scrollToPosition(10)
            }
            rvFeed.smoothScrollToPosition(0)
        }

        findViewById<ImageView>(R.id.search_button).setOnClickListener {
            showToast("搜索功能开发中...")
        }

        switchBtn.setOnClickListener {
            toggleLayoutMode()
        }
    }

    private fun initData() {
        fetchWeatherData()
        loadFeedData(isRefresh = true)
    }

    private fun setupFeedList() {
        staggeredLayoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL).apply {
            gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_NONE
        }
        linearLayoutManager = LinearLayoutManager(this)

        rvFeed.layoutManager = if (isGridMode) staggeredLayoutManager else linearLayoutManager
        feedAdapter = FeedAdapter(
            feedList = feedDataList,
            onHeaderClick = { startActivity(Intent(this, WeatherActivity::class.java)) },
            onItemLongClick = { pos -> showDeleteConfirmDialog(pos) }
        )
        feedAdapter.isGridMode = isGridMode
        rvFeed.adapter = feedAdapter

        rvFeed.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                handleScrollEvent()
            }
        })

        initExposureManager()
    }

    private fun handleScrollEvent() {
        val firstVisiblePos = getFirstVisiblePosition()
        val totalItemCount = rvFeed.layoutManager?.itemCount ?: 0
        val visibleItemCount = rvFeed.layoutManager?.childCount ?: 0

        if (firstVisiblePos > 5) {
            if (fabBackTop.visibility != View.VISIBLE) fabBackTop.show()
        } else {
            if (fabBackTop.visibility == View.VISIBLE) fabBackTop.hide()
        }

        if (!isLoading && !isLastPage) {
            if (visibleItemCount > 0 &&
                (firstVisiblePos + visibleItemCount) >= totalItemCount &&
                totalItemCount >= PAGE_SIZE
            ) {
                loadFeedData(isRefresh = false)
            }
        }
    }

    private fun getFirstVisiblePosition(): Int {
        return if (isGridMode) {
            staggeredLayoutManager.findFirstVisibleItemPositions(scrollPositions)
            scrollPositions[0]
        } else {
            linearLayoutManager.findFirstVisibleItemPosition()
        }
    }

    private fun initExposureManager() {
        exposureManager?.detach()
        exposureManager = FeedExposureManager(rvFeed, feedDataList, object : OnExposureListener {
            override fun onStateChanged(position: Int, styleId: String, oldState: ExposureState, newState: ExposureState, timestamp: Long) {
                val eventName = when (newState) {
                    ExposureState.VISIBLE -> "开始露出"
                    ExposureState.OVER_50 -> "露出超50%"
                    ExposureState.FULL_VISIBLE -> "完整展示"
                    ExposureState.INVISIBLE -> "消失不可见"
                }
                ExposureDataHolder.addLog(position, styleId, eventName, timestamp)
            }
        })
    }

    private fun loadFeedData(isRefresh: Boolean) {
        if (isLoading) return
        isLoading = true

        if (!isRefresh) {
            feedAdapter.isFooterVisible = true
        } else {
            currentPage = 1
            isLastPage = false
        }

        lifecycleScope.launch {
            delay(1000)

            if (currentPage > MAX_PAGES) {
                isLastPage = true
                showToast("没有更多数据了")
                finishLoading(isRefresh, emptyList())
            } else {
                val newItems = generateMockData()
                finishLoading(isRefresh, newItems)
                currentPage++
            }
        }
    }

    private fun finishLoading(isRefresh: Boolean, newItems: List<Feedable>) {
        isLoading = false
        swipeRefreshLayout.isRefreshing = false

        if (feedAdapter.isFooterVisible) {
            feedAdapter.isFooterVisible = false
        }

        if (isRefresh) {
            feedDataList.clear()
            feedDataList.addAll(newItems)
            feedAdapter.notifyDataSetChanged()
            showToast("刷新成功")
        } else {
            if (newItems.isNotEmpty()) {
                val startPos = feedDataList.size + 1
                feedDataList.addAll(newItems)
                feedAdapter.notifyItemRangeInserted(startPos, newItems.size)
            }
        }
    }

    private fun generateMockData(): List<Feedable> {
        val items = mutableListOf<Feedable>()
        val currentLayoutType = if (isGridMode) 1 else 2
        val suffix = if (isGridMode) "_grid" else "_list"

        repeat(PAGE_SIZE) {
            val cardType = random.nextInt(100)

            val item: Feedable = when {
                cardType < 40 -> createItem("image_card$suffix", ImageCardData(
                    author = "User ${random.nextInt(1000)}",
                    time = "${random.nextInt(12) + 1}小时前",
                    content = contentPool.random(),
                    imageSource = bannerResources.random(),
                    layoutType = currentLayoutType
                ))
                cardType < 70 -> createItem("article_card$suffix", ArticleCardData(
                    title = "文章${random.nextInt(1000)}",
                    summary = "摘要摘要摘要摘要摘要",
                    time = "${random.nextInt(24)}小时前",
                    author = "User ${random.nextInt(1000)}",
                    layoutType = currentLayoutType
                ))
                cardType < 90 -> createItem("video_card$suffix", VideoCardData(
                    title = "视频${random.nextInt(1000)}",
                    duration = String.format("%02d:%02d", random.nextInt(10), random.nextInt(60)),
                    coverImage = bannerResources.random(),
                    author = "User ${random.nextInt(1000)}",
                    time = "${random.nextInt(24)}小时前",
                    layoutType = currentLayoutType
                ))
                else -> createItem("ad_card$suffix", AdCardData(
                    title = "广告主标题",
                    desc = "广告副标题",
                    coverImage = bannerResources.random(),
                    buttonText = "感兴趣",
                    layoutType = currentLayoutType
                ))
            }
            items.add(item)
        }
        return items
    }

    private fun createItem(styleId: String, dataObj: Any): Feedable {
        return object : Feedable {
            override val styleId = styleId
            override val data = gson.toJsonTree(dataObj)
        }
    }

    private fun toggleLayoutMode() {
        // 切换模式标记
        isGridMode = !isGridMode
        feedAdapter.isGridMode = isGridMode

        // 切换 LayoutManager
        rvFeed.layoutManager = if (isGridMode) staggeredLayoutManager else linearLayoutManager

        // 重新设置 Adapter 是强制 RecyclerView 刷新布局、解决宽度问题的最可靠方法
        rvFeed.adapter = feedAdapter

        // 切换后滚动到顶部，提供一致的用户体验
        rvFeed.scrollToPosition(0)

        // 更新切换按钮图标（不再更换图标，始终使用同一个）
        switchBtn.setImageResource(R.drawable.ic_header_switch)
        showToast(if (isGridMode) "切换为双列模式" else "切换为单列模式")
    }

    private fun showDeleteConfirmDialog(adapterPosition: Int) {
        AlertDialog.Builder(this)
            .setTitle("删除提示")
            .setMessage("确定要删除这条内容吗？")
            .setPositiveButton("删除") { _, _ ->
                val dataIndex = adapterPosition - 1
                if (dataIndex in feedDataList.indices) {
                    feedDataList.removeAt(dataIndex)
                    feedAdapter.notifyItemRemoved(adapterPosition)
                    feedAdapter.notifyItemRangeChanged(adapterPosition, feedDataList.size - dataIndex + 1)
                    showToast("删除成功")
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun setupBottomNavigation() {
        findViewById<LinearLayout>(R.id.tab_home).setOnClickListener {
            showToast("已经在首页")
        }
        findViewById<LinearLayout>(R.id.tab_feed).setOnClickListener {
            startActivity(Intent(this, ExposureDataActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.tab_mine).setOnClickListener {
            startActivity(Intent(this, UserInfoActivity::class.java))
            overridePendingTransition(0, 0)
        }
    }

    // 使用 WeatherManager 获取真实天气
    private fun fetchWeatherData() {
        WeatherManager.instance.fetchWeather(object : WeatherManager.WeatherCallback {
            override fun onSuccess(weather: LiveWeather) {
                // 回调已在主线程，直接更新 UI
                feedAdapter.updateHeader(weather)
            }

            override fun onError(msg: String) {
                showToast("天气获取失败: $msg")
            }
        })
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}

/**
 * FeedAdapter for the plugin-style card system
 */
class FeedAdapter(
    private val feedList: List<Feedable>,
    private val onHeaderClick: () -> Unit,
    private val onItemLongClick: (Int) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var isGridMode: Boolean = false
    var isFooterVisible: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                if (value) {
                    notifyItemInserted(itemCount) // itemCount is now list.size + header + footer
                } else {
                    notifyItemRemoved(itemCount) // itemCount was list.size + header + footer, now it's one less
                }
            }
        }

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_FOOTER = -1
    }

    private var weatherData: LiveWeather? = null

    fun updateHeader(data: LiveWeather) {
        weatherData = data
        notifyItemChanged(0)
    }

    override fun getItemCount(): Int {
        return 1 + feedList.size + if (isFooterVisible) 1 else 0
    }

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            0 -> TYPE_HEADER
            itemCount - 1 -> if (isFooterVisible) TYPE_FOOTER else getFeedItemViewType(position)
            else -> getFeedItemViewType(position)
        }
    }

    private fun getFeedItemViewType(position: Int): Int {
        val item = feedList[position - 1]
        return CardStyleRegistry.styleIdToViewType(item.styleId)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_home_header, parent, false)
            )
            TYPE_FOOTER -> FooterViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_footer_loading, parent, false)
            )
            else -> {
                val styleId = CardStyleRegistry.viewTypeToStyleId(viewType)
                    ?: return createFallbackViewHolder(parent)
                val processor = CardStyleRegistry.getProcessor(styleId)
                    ?: return createFallbackViewHolder(parent)
                processor.createViewHolder(parent)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val layoutParams = holder.itemView.layoutParams
        if (layoutParams is StaggeredGridLayoutManager.LayoutParams) {
            layoutParams.isFullSpan = (getItemViewType(position) == TYPE_HEADER || getItemViewType(position) == TYPE_FOOTER)
        }

        when (holder.itemViewType) {
            TYPE_HEADER -> (holder as? HeaderViewHolder)?.bind(weatherData, onHeaderClick)
            TYPE_FOOTER -> { /* No binding needed for footer */ }
            else -> {
                val item = feedList.getOrNull(position - 1) ?: return
                val processor = CardStyleRegistry.getProcessor(item.styleId)
                processor?.bindViewHolder(holder, item)

                holder.itemView.setOnLongClickListener {
                    onItemLongClick(position)
                    true
                }
            }
        }
    }

    private fun createFallbackViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val emptyView = View(parent.context)
        emptyView.layoutParams = ViewGroup.LayoutParams(0, 0) // Collapse the view
        return object : RecyclerView.ViewHolder(emptyView) {}
    }

    // --- Inner ViewHolders ---

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

    class FooterViewHolder(view: View) : RecyclerView.ViewHolder(view)
}
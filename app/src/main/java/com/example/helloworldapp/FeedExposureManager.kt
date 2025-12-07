package com.example.helloworldapp

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * 曝光状态枚举
 */
enum class ExposureState {
    INVISIBLE,      // 完全不可见
    VISIBLE,        // 露出 ( > 0%)
    OVER_50,        // 露出超过 50%
    FULL_VISIBLE    // 完整露出 (100%)
}

/**
 * 曝光事件回调接口
 */
interface OnExposureListener {
    /**
     * @param position Adapter中的位置
     * @param styleId 卡片类型 ID
     * @param oldState 旧状态
     * @param newState 新状态
     * @param timestamp 事件发生时间戳 (新增)
     */
    fun onStateChanged(
        position: Int,
        styleId: String,
        oldState: ExposureState,
        newState: ExposureState,
        timestamp: Long // 新增参数
    )
}

/**
 * 曝光管理器
 * 负责监听 RecyclerView 滚动，计算 Item 可见性，并触发状态变更
 */
class FeedExposureManager(
    private val recyclerView: RecyclerView,
    private val dataList: List<Feedable>, // 需要数据源来获取卡片类型
    private val listener: OnExposureListener
) : RecyclerView.OnScrollListener() {

    // 记录每个位置当前的曝光状态
    private val stateMap = mutableMapOf<Int, ExposureState>()

    init {
        // 初始化时监听
        recyclerView.addOnScrollListener(this)
    }

    // 页面停止滚动或手动触发检测时调用
    fun onScrolled(dx: Int, dy: Int) {
        checkVisibility()
    }

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)
        checkVisibility()
    }

    /**
     * 核心方法：检测所有可见 Item 的状态
     */
    private fun checkVisibility() {
        val layoutManager = recyclerView.layoutManager ?: return
        val visibleRect = Rect()

        // 获取 RecyclerView 在屏幕上的可见区域
        recyclerView.getGlobalVisibleRect(visibleRect)

        // 1. 遍历当前 RecyclerView 中所有 attach 的子 View
        val childCount = recyclerView.childCount

        // 记录本轮检测中可见的位置，用于后续判断哪些 Item 消失了
        val currentVisiblePositions = mutableSetOf<Int>()

        for (i in 0 until childCount) {
            val child = recyclerView.getChildAt(i) ?: continue
            val position = recyclerView.getChildAdapterPosition(child)

            // 排除无效位置、Header(0) 和 Footer
            if (position <= 0 || position > dataList.size) continue

            // 获取数据源索引 (减去Header)
            val dataIndex = position - 1
            val itemData = dataList.getOrNull(dataIndex) ?: continue

            currentVisiblePositions.add(position)

            // 计算可见比例
            val percentage = calculateVisiblePercentage(child)

            // 确定新状态
            val newState = when {
                percentage >= 1.0f -> ExposureState.FULL_VISIBLE
                percentage >= 0.5f -> ExposureState.OVER_50
                percentage > 0f -> ExposureState.VISIBLE
                else -> ExposureState.INVISIBLE
            }

            // 获取旧状态 (默认为 INVISIBLE)
            val oldState = stateMap.getOrDefault(position, ExposureState.INVISIBLE)

            // 如果状态发生跃迁
            if (newState != oldState) {
                dispatchEvents(position, itemData.styleId, oldState, newState)
                stateMap[position] = newState
            }
        }

        // 2. 处理消失的 Item
        val iterator = stateMap.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val pos = entry.key
            val state = entry.value

            if (!currentVisiblePositions.contains(pos) && state != ExposureState.INVISIBLE) {
                // 触发消失事件
                val dataIndex = pos - 1
                val styleId = dataList.getOrNull(dataIndex)?.styleId ?: "unknown"

                dispatchEvents(pos, styleId, state, ExposureState.INVISIBLE)
                iterator.remove() // 移除记录
            }
        }
    }

    private fun dispatchEvents(position: Int, styleId: String, oldState: ExposureState, newState: ExposureState) {
        // 获取当前时间戳
        val currentTime = System.currentTimeMillis()
        listener.onStateChanged(position, styleId, oldState, newState, currentTime)
    }

    /**
     * 计算 View 的可见比例 (0.0 ~ 1.0)
     */
    private fun calculateVisiblePercentage(view: View): Float {
        val viewRect = Rect()
        val isVisible = view.getGlobalVisibleRect(viewRect)

        if (!isVisible) return 0f

        val viewHeight = view.height.toFloat()
        if (viewHeight == 0f) return 0f

        // 简单算法：可见高度 / 总高度
        val visibleHeight = viewRect.height().toFloat()

        return (visibleHeight / viewHeight).coerceIn(0f, 1f)
    }

    // 销毁时调用
    fun detach() {
        recyclerView.removeOnScrollListener(this)
        stateMap.clear()
    }
}

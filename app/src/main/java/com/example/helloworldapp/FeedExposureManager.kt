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
        timestamp: Long
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

    private val visibleRect = Rect()
    private val currentVisiblePositions = mutableSetOf<Int>()

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
     * 生命周期处理：页面不可见（暂停）
     * 强制结束所有当前的曝光
     */
    fun onPause() {
        val iterator = stateMap.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val pos = entry.key
            val state = entry.value

            if (state != ExposureState.INVISIBLE) {
                // 获取 styleId
                val dataIndex = pos - 1
                val itemData = dataList.getOrNull(dataIndex)
                var styleId = itemData?.styleId ?: "unknown"
                
                // 修正 styleId (Grid/List)
                val layoutManager = recyclerView.layoutManager
                val isGridMode = layoutManager is androidx.recyclerview.widget.StaggeredGridLayoutManager
                if (isGridMode) {
                    if (styleId.endsWith("_list")) styleId = styleId.replace("_list", "_grid")
                } else {
                    if (styleId.endsWith("_grid")) styleId = styleId.replace("_grid", "_list")
                }

                // 分发结束曝光事件
                dispatchEvents(pos, styleId, state, ExposureState.INVISIBLE)
                
                // 更新状态为不可见，以便下次 onResume 能重新触发开始曝光
                entry.setValue(ExposureState.INVISIBLE)
            }
        }
    }

    /**
     * 生命周期处理：页面可见（恢复）
     * 重新检测当前可见性
     */
    fun onResume() {
        recyclerView.post {
            checkVisibility()
        }
    }

    /**
     * 核心方法：检测所有可见 Item 的状态
     */
    private fun checkVisibility() {
        val layoutManager = recyclerView.layoutManager ?: return

        recyclerView.getGlobalVisibleRect(visibleRect)

        // 判断当前是否是网格模式 (根据 LayoutManager 类型判断最准确)
        val isGridMode = layoutManager is androidx.recyclerview.widget.StaggeredGridLayoutManager

        val childCount = recyclerView.childCount

        currentVisiblePositions.clear()

        for (i in 0 until childCount) {
            val child = recyclerView.getChildAt(i) ?: continue
            val position = recyclerView.getChildAdapterPosition(child)

            if (position <= 0 || position > dataList.size) continue

            val dataIndex = position - 1
            val itemData = dataList.getOrNull(dataIndex) ?: continue

            // --- 核心修复：在这里修正 styleId ---
            var styleId = itemData.styleId

            if (isGridMode) {
                // 如果是网格模式，强制视作 _grid
                if (styleId.endsWith("_list")) {
                    styleId = styleId.replace("_list", "_grid")
                }
            } else {
                // 如果是列表模式，强制视作 _list
                if (styleId.endsWith("_grid")) {
                    styleId = styleId.replace("_grid", "_list")
                }
            }
            // ----------------------------------

            currentVisiblePositions.add(position)

            val percentage = calculateVisiblePercentage(child)

            val newState = when {
                percentage >= 1.0f -> ExposureState.FULL_VISIBLE
                percentage >= 0.5f -> ExposureState.OVER_50
                percentage > 0f -> ExposureState.VISIBLE
                else -> ExposureState.INVISIBLE
            }

            val oldState = stateMap.getOrDefault(position, ExposureState.INVISIBLE)

            if (newState != oldState) {
                dispatchEvents(position, styleId, oldState, newState)
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
                val dataIndex = pos - 1
                val itemData = dataList.getOrNull(dataIndex)

                // 同样逻辑：消失的时候也要修正 styleId
                var styleId = itemData?.styleId ?: "unknown"
                if (isGridMode) {
                    if (styleId.endsWith("_list")) styleId = styleId.replace("_list", "_grid")
                } else {
                    if (styleId.endsWith("_grid")) styleId = styleId.replace("_grid", "_list")
                }

                dispatchEvents(pos, styleId, state, ExposureState.INVISIBLE)
                iterator.remove()
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
        currentVisiblePositions.clear()
    }
}

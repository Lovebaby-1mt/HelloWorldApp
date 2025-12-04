package com.example.helloworldapp

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.JsonElement

/**
 * 1. 基础接口：所有 Feed 数据实体必须实现此接口
 */
interface Feedable {
    val styleId: String      // 卡片类型ID，如 "image_card"
    val data: JsonElement    // 卡片具体数据，通常是 Gson 转成的 JsonTree
}

/**
 * 2. 基础接口：卡片处理器，负责创建 ViewHolder 和绑定数据
 */
interface CardProcessor {
    fun createViewHolder(parent: ViewGroup): RecyclerView.ViewHolder
    fun bindViewHolder(holder: RecyclerView.ViewHolder, item: Feedable, onLongClick: ((Int) -> Unit)? = null)
}

/**
 * 3. 核心注册表：管理所有卡片类型和 ViewType 的映射
 */
object CardStyleRegistry {
    // 存储 styleId -> Processor 的映射
    private val processors = mutableMapOf<String, CardProcessor>()

    // 存储 styleId <-> ViewType (Int) 的双向映射
    private val viewTypeMap = mutableMapOf<String, Int>()
    private val styleIdMap = mutableMapOf<Int, String>()

    // 自动生成 ViewType，从 1 开始（0 留给 Header）
    private var nextViewType = 1

    fun register(styleId: String, processor: CardProcessor) {
        processors[styleId] = processor

        if (!viewTypeMap.containsKey(styleId)) {
            val viewType = nextViewType++
            viewTypeMap[styleId] = viewType
            styleIdMap[viewType] = styleId
        }
    }

    fun getProcessor(styleId: String): CardProcessor? {
        return processors[styleId]
    }

    fun styleIdToViewType(styleId: String): Int {
        return viewTypeMap[styleId] ?: 0
    }

    fun viewTypeToStyleId(viewType: Int): String? {
        return styleIdMap[viewType]
    }
}
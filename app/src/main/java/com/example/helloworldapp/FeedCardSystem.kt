package com.example.helloworldapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.JsonElement

/**
 * Feed 卡片系统的核心接口与数据模型
 */

// 1. 标准化数据协议：所有卡片数据都基于此结构
interface Feedable {
    val styleId: String
    val data: JsonElement? // 用于存放该样式特有的数据
}

/**
 * 卡片处理器接口 (插件式样式的核心)
 * 每个新的卡片样式都需要实现此接口
 */
interface CardProcessor {
    /**
     * 创建该样式卡片对应的 ViewHolder
     */
    fun createViewHolder(parent: ViewGroup): RecyclerView.ViewHolder

    /**
     * 将数据绑定到 ViewHolder
     */
    fun bindViewHolder(holder: RecyclerView.ViewHolder, item: Feedable)
}

/**
 * 卡片样式注册表 (单例)
 * 用于注册、查询和管理所有的卡片处理器
 */
object CardStyleRegistry {
    // 使用 Map 存储 styleId -> CardProcessor 的映射关系
    private val processors = mutableMapOf<String, CardProcessor>()

    /**
     * 注册一个新的卡片处理器
     */
    fun register(styleId: String, processor: CardProcessor) {
        if (processors.containsKey(styleId)) {
            // 在实际项目中，这里应该有更健壮的日志或错误处理
            println("Warning: Overwriting card processor for styleId: $styleId")
        }
        processors[styleId] = processor
    }

    /**
     * 根据 styleId 获取对应的处理器
     */
    fun getProcessor(styleId: String): CardProcessor? {
        return processors[styleId]
    }

    /**
     * 将 styleId 直接作为 RecyclerView 的 ViewType 使用
     * 这里需要将 String ID 转换为 Int。使用 hashCode 是一个简单的方法，但需注意潜在的哈希冲突。
     * 在生产环境中，可以建立一个 styleId -> Int 的双向映射来保证唯一性。
     */
    fun styleIdToViewType(styleId: String): Int {
        return styleId.hashCode()
    }

    fun viewTypeToStyleId(viewType: Int): String? {
        // 遍历查找哈希值匹配的 styleId (效率较低，仅为演示)
        return processors.keys.find { it.hashCode() == viewType }
    }
}
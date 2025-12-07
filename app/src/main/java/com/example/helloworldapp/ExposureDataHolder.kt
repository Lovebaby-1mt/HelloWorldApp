package com.example.helloworldapp

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

// 统计数据模型
data class ExposureStat(
    var count: Int = 0,      // 曝光次数
    var totalDuration: Long = 0 // 总曝光时长 (毫秒)
)

object ExposureDataHolder {
    // 日志列表
    private val logs = CopyOnWriteArrayList<String>()

    // 统计数据：Map<StyleId, Stat>
    private val statsMap = ConcurrentHashMap<String, ExposureStat>()

    // 临时记录：Map<Position, StartTimestamp>，用于计算单次曝光时长
    private val activeSessions = ConcurrentHashMap<Int, Long>()

    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    fun addLog(position: Int, styleId: String, eventName: String, timestamp: Long) {
        // 1. 生成日志字符串
        val timeStr = dateFormat.format(Date(timestamp))
        val logEntry = "[$timeStr] \nPos:$position [$styleId] -> $eventName"
        logs.add(0, logEntry)
        if (logs.size > 2000) logs.removeAt(logs.size - 1)

        // 2. 统计逻辑
        val stat = statsMap.getOrPut(styleId) { ExposureStat() }

        when (eventName) {
            "开始露出" -> {
                // 【核心修改】只有当这个位置之前没有记录时，才算一次新的曝光开始
                // 防止滑动抖动导致起始时间被不断重置
                if (!activeSessions.containsKey(position)) {
                    activeSessions[position] = timestamp
                    stat.count++
                }
            }
            "消失不可见" -> {
                // 结算时长
                val startTime = activeSessions.remove(position)
                if (startTime != null) {
                    val duration = timestamp - startTime
                    if (duration > 0) {
                        stat.totalDuration += duration
                    }
                }
            }
        }
    }

    fun getLogs(): List<String> = logs

    // 获取统计结果
    fun getStats(): Map<String, ExposureStat> = statsMap

    fun clear() {
        logs.clear()
        statsMap.clear()
        activeSessions.clear()
    }
}

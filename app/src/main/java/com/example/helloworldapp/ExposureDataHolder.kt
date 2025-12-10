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
        // 1. 归一化 StyleId
        val baseStyleId = styleId.replace("_list", "").replace("_grid", "")

        // 2. 生成日志
        val timeStr = dateFormat.format(Date(timestamp))
        val logEntry = "[$timeStr] \nPos:$position [$styleId] -> $eventName"
        logs.add(logEntry)
        if (logs.size > 2000) {
            logs.removeAt(0)
        }

        // 3. 统计逻辑
        val stat = statsMap.getOrPut(baseStyleId) { ExposureStat() }

        when (eventName) {
            "开始露出" -> {
                // 【核心修复】只有当 activeSessions 中没有这个 position 的记录时，才视为一次新的开始。
                // 如果已经存在，说明它正在曝光中，忽略这次重复的“开始”信号。
                if (!activeSessions.containsKey(position)) {
                    activeSessions[position] = timestamp
                    stat.count++
                }
            }
            "消失不可见" -> {
                // 结算时长：只有能取到开始时间，才进行结算
                val startTime = activeSessions.remove(position)
                if (startTime != null) {
                    val duration = timestamp - startTime
                    // 【兜底修复】过滤异常数据，例如负数或不合理的超长时长（比如 > 1小时）
                    if (duration in 1..3600000) {
                        stat.totalDuration += duration
                    }
                }
            }
            // 其他中间状态（如50%露出、完整露出）不处理计时逻辑，防止干扰
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

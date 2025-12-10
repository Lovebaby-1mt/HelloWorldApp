package com.example.helloworldapp

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ExposureDataActivity : AppCompatActivity() {

    // 颜色列表，用于给文字设置颜色，需与 SimplePieView 中的颜色对应
    private val colors = listOf(
        Color.parseColor("#2196F3"),
        Color.parseColor("#4CAF50"),
        Color.parseColor("#FFC107"),
        Color.parseColor("#FF5722"),
        Color.parseColor("#9C27B0")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exposure_data)

        val tvLogs = findViewById<TextView>(R.id.tv_logs)
        val tvStatsDetail = findViewById<TextView>(R.id.tv_stats_detail)
        val pieChart = findViewById<SimplePieView>(R.id.pie_chart)
        val btnClear = findViewById<Button>(R.id.btn_clear)

        // 1. 加载数据
        refreshUI(tvLogs, tvStatsDetail, pieChart)

        // 2. 清空逻辑
        btnClear.setOnClickListener {
            ExposureDataHolder.clear()
            refreshUI(tvLogs, tvStatsDetail, pieChart)
        }

        // 3. 底部导航
        setupBottomNavigation()
    }

    private fun refreshUI(tvLogs: TextView, tvStats: TextView, pieChart: SimplePieView) {
        // --- 更新下半部分：日志 ---
        val logs = ExposureDataHolder.getLogs()
        if (logs.isEmpty()) {
            tvLogs.text = "暂无曝光数据...\n请回到首页滑动列表产生数据。"
        } else {
            val sb = StringBuilder()
            for (log in logs) {
                sb.append(log).append("\n")
                sb.append("------------------------------\n")
            }
            tvLogs.text = sb.toString()
        }

        // --- 更新上半部分：统计图表 ---
        val statsMap = ExposureDataHolder.getStats()

        // 1. 设置饼图数据
        // 将 Stat 转换为 Map<StyleId, Duration> 传给饼图
        val chartData = statsMap.mapValues { it.value.totalDuration }
        pieChart.setData(chartData)

        // 2. 设置统计文本 (带颜色)
        if (statsMap.isEmpty()) {
            tvStats.text = "暂无统计数据\n(需产生消失事件才会计入时长)"
        } else {
            // 使用 SpannableStringBuilder 来构建富文本
            val spannableBuilder = android.text.SpannableStringBuilder()
            var index = 0

            // 排序：按时长降序
            statsMap.entries.sortedByDescending { it.value.totalDuration }.forEach { entry ->
                val styleId = entry.key
                val stat = entry.value
                val color = colors[index % colors.size]

                // 简化卡片名称显示
                val displayName = when(styleId) {
                    "image_card" -> "图文卡片"
                    "video_card" -> "视频卡片"
                    "article_card" -> "文章卡片"
                    "ad_card" -> "广告卡片"
                    else -> styleId
                }

                val line = "● $displayName\n   时长: ${String.format("%.1f", stat.totalDuration/1000f)}s(${stat.totalDuration}ms) | 曝光: ${stat.count}次\n\n"
                val start = spannableBuilder.length

                spannableBuilder.append(line)

                spannableBuilder.setSpan(
                    ForegroundColorSpan(color),
                    start,
                    start + 1, // 结束位置（不包含），所以是 start+1
                    android.text.Spanned.SPAN_INCLUSIVE_EXCLUSIVE
                )

                index++
            }
            // 必须赋值 spannableBuilder，而不是之前的 sbStats
            tvStats.text = spannableBuilder
        }
        val scrollView = findViewById<androidx.core.widget.NestedScrollView>(R.id.scroll_view_logs)
        scrollView.post {
            scrollView.fullScroll(android.view.View.FOCUS_DOWN)
        }
    }

    private fun setupBottomNavigation() {
        val tabHome = findViewById<LinearLayout>(R.id.tab_home)
        val tabMine = findViewById<LinearLayout>(R.id.tab_mine)

        tabHome.setOnClickListener {
            finish()
            overridePendingTransition(0, 0)
        }

        tabMine.setOnClickListener {
            val intent = Intent(this, UserInfoActivity::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }
    }
}

package com.example.helloworldapp

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.random.Random

class SimplePieView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rectF = RectF()
    private var data: List<Pair<Float, Int>> = emptyList() // Value, Color

    // 预设颜色池
    private val colors = listOf(
        Color.parseColor("#2196F3"), // 蓝
        Color.parseColor("#4CAF50"), // 绿
        Color.parseColor("#FFC107"), // 黄
        Color.parseColor("#FF5722"), // 橙
        Color.parseColor("#9C27B0")  // 紫
    )

    fun setData(values: Map<String, Long>) {
        val total = values.values.sum().toFloat()
        if (total == 0f) {
            this.data = emptyList()
            invalidate()
            return
        }

        var colorIndex = 0
        this.data = values.map { entry ->
            val sweepAngle = (entry.value / total) * 360f
            val color = colors[colorIndex % colors.size]
            colorIndex++
            Pair(sweepAngle, color)
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (data.isEmpty()) {
            // 空数据画个灰圈
            paint.color = Color.LTGRAY
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 5f
            val radius = (width.coerceAtMost(height) / 2f) * 0.8f
            canvas.drawCircle(width / 2f, height / 2f, radius, paint)
            return
        }

        paint.style = Paint.Style.FILL
        val diameter = width.coerceAtMost(height) * 0.8f
        val left = (width - diameter) / 2
        val top = (height - diameter) / 2
        rectF.set(left, top, left + diameter, top + diameter)

        var startAngle = -90f // 从顶部开始
        for ((sweepAngle, color) in data) {
            paint.color = color
            // 留一点间隙
            val gap = if (data.size > 1) 2f else 0f
            canvas.drawArc(rectF, startAngle, sweepAngle - gap, true, paint)
            startAngle += sweepAngle
        }
    }
}
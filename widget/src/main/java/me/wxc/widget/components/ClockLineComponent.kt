package me.wxc.widget.components

import android.graphics.*
import me.wxc.widget.ICalendarComponent
import me.wxc.widget.ICalendarModel
import me.wxc.widget.tools.*
import java.util.*

class ClockLineComponent(override var model: ClockLineModel) : ICalendarComponent<ClockLineModel> {
    override val originRect: RectF = originRect()
    override val rect: RectF = originRect()

    override fun drawSelf(canvas: Canvas, paint: Paint, anchorPoint: Point) {
        paint.color = Color.GRAY
        paint.strokeWidth = 0.5f.dp
        paint.textSize = 14f.dp
        val stopX = screenWidth - 10f.dp
        val y = rect.top + 5f.dp
        canvas.drawLine(rect.left, y, stopX, y, paint)
    }

    override fun updateRect(anchorPoint: Point) {
        rect.top = originRect.top + anchorPoint.y
        rect.bottom = originRect.bottom + anchorPoint.y
    }
}

data class ClockLineModel(
    val clock: Int,
) : ICalendarModel {
    private val zeroClock = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    override val startTime: Long = zeroClock.timeInMillis + clock * hourMills
    override val endTime: Long = zeroClock.timeInMillis + clock * hourMills
    override val level: Int = 0
    val showText: String
        get() = startTime.hhMM
}
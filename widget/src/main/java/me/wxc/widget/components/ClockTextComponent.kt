package me.wxc.widget.components

import android.graphics.*
import me.wxc.widget.ICalendarComponent
import me.wxc.widget.ICalendarModel
import me.wxc.widget.tools.*
import java.util.*

class ClockTextComponent(override var model: ClockTextModel) : ICalendarComponent<ClockTextModel> {
    override val originRect: RectF = originRect().apply {
        left = 0f
        right = clockWidth
    }
    override val rect: RectF = originRect().apply {
        left = 0f
        right = clockWidth
    }

    override fun drawSelf(canvas: Canvas, paint: Paint, anchorPoint: Point) {
        paint.color = Color.WHITE
        canvas.drawRect(rect, paint)
        paint.color = Color.GRAY
        paint.textSize = 14f.dp
        val startX = 10f.dp
        val y = rect.top + 12f.dp
        canvas.drawText(model.showText, startX, y, paint)
    }

    override fun updateRect(anchorPoint: Point) {
        rect.top = originRect.top + anchorPoint.y
        rect.bottom = originRect.bottom + anchorPoint.y
    }
}

data class ClockTextModel(
    val clock: Int,
) : ICalendarModel {
    private val zeroClock = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    override val startTime: Long = zeroClock.timeInMillis + clock * hourMills
    override val endTime: Long = zeroClock.timeInMillis + (clock + 1) * hourMills
    override val level: Int = 3
    val showText: String
        get() = startTime.hhMM
}
package me.wxc.widget.components

import android.graphics.*
import me.wxc.widget.ICalendarComponent
import me.wxc.widget.ICalendarModel
import me.wxc.widget.tools.*

class DateLineComponent(override var model: DateLineModel) : ICalendarComponent<DateLineModel> {
    override val originRect: RectF = originRect().apply {
        top = 0f
        bottom = dayLineHeight
    }
    override val rect: RectF = originRect().apply {
        top = 0f
        bottom = dayLineHeight
    }
    override fun drawSelf(canvas: Canvas, paint: Paint, anchorPoint: Point) {
        if (rect.right < clockWidth) return
        paint.color = Color.BLACK
        paint.textSize = 20f.dp
        canvas.drawText(model.startTime.dayOfMonth.toString(), rect.left, rect.bottom - 10f.dp, paint)
        paint.textSize = 14f.dp
        canvas.drawText(model.startTime.dayOfWeekText, rect.left, rect.bottom - 30f.dp, paint)
    }

    override fun updateRect(anchorPoint: Point) {
        rect.left = originRect.left + anchorPoint.x
        rect.right = originRect.right + anchorPoint.x
    }
}

data class DateLineModel(
    private val days: Int
) : ICalendarModel {
    override var startTime: Long = startOfDay().timeInMillis - dayMills * days
    override var endTime: Long = startTime + dayMills
    override val level: Int = Int.MAX_VALUE
}
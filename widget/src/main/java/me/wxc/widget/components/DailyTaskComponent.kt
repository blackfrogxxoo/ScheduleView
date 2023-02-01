package me.wxc.widget.components

import android.graphics.*
import android.util.Log
import me.wxc.widget.ICalendarComponent
import me.wxc.widget.ICalendarModel
import me.wxc.widget.tools.dayLineHeight
import me.wxc.widget.tools.dp
import me.wxc.widget.tools.hourMills
import me.wxc.widget.tools.originRect

class DailyTaskComponent(override var model: DailyTaskModel) : ICalendarComponent<DailyTaskModel> {
    override val rect: RectF = originRect()
    override val originRect: RectF = originRect()
    override fun drawSelf(canvas: Canvas, paint: Paint, anchorPoint: Point) {
        if (rect.bottom < dayLineHeight) return
        paint.color = Color.parseColor("#330000ff")
        canvas.drawRoundRect(
            rect.left + 2f.dp,
            rect.top,
            rect.right - 2f.dp,
            rect.bottom,
            4f.dp,
            4f.dp,
            paint
        )
        paint.color = Color.parseColor("#FF0000FF")
        paint.textSize = 14f.dp
        canvas.drawText(model.title, rect.left + 12f.dp, rect.top + 22f.dp, paint)
    }

    override fun updateRect(anchorPoint: Point) {
        rect.left = originRect.left + anchorPoint.x
        rect.right = originRect.right + anchorPoint.x
        rect.top = originRect.top + anchorPoint.y
        rect.bottom = originRect.bottom + anchorPoint.y
    }
}

data class DailyTaskModel(
    override var startTime: Long = System.currentTimeMillis(),
    val duration: Long = hourMills,
    var title: String,
) : ICalendarModel {
    override val endTime: Long
        get() = startTime + duration
    override val level: Int = 1
}
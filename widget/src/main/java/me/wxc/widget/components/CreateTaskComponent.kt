package me.wxc.widget.components

import android.graphics.*
import me.wxc.widget.ICalendarComponent
import me.wxc.widget.ICalendarEditable
import me.wxc.widget.ICalendarModel
import me.wxc.widget.tools.*

class CreateTaskComponent(override var model: CreateTaskModel) : ICalendarComponent<CreateTaskModel>, ICalendarEditable {
    override val originRect: RectF = originRect()
    override val rect: RectF = originRect()
    override fun drawSelf(canvas: Canvas, paint: Paint, anchorPoint: Point) {
        if (rect.bottom < dayLineHeight) return
        paint.color = Color.parseColor("#550032ff")
        canvas.drawRoundRect(
            rect.left + 2f.dp,
            rect.top,
            rect.right - 2f.dp,
            rect.bottom,
            4f.dp,
            4f.dp,
            paint
        )
        paint.color = Color.parseColor("#FF0032ff")
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

data class CreateTaskModel(
    override var startTime: Long = System.currentTimeMillis(),
    val duration: Long = hourMills / 2,
    var title: String = "新建日程",
) : ICalendarModel {
    override val endTime: Long
        get() = startTime + duration
    override val level: Int = 1
}
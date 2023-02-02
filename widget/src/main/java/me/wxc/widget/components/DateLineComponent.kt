package me.wxc.widget.components

import android.graphics.*
import me.wxc.widget.ICalendarComponent
import me.wxc.widget.ICalendarModel
import me.wxc.widget.tools.*

class DateLineComponent(override var model: DateLineModel) : ICalendarComponent<DateLineModel> {
    override val originRect: RectF = originRect().apply {
        top = 0f
        bottom = dateLineHeight
    }
    override val drawingRect: RectF = originRect().apply {
        top = 0f
        bottom = dateLineHeight
    }

    override fun onDraw(canvas: Canvas, paint: Paint) {
        if (drawingRect.right < clockWidth) return
        canvas.save()
        canvas.clipRect(clockWidth, 0f, screenWidth.toFloat(), dateLineHeight)
        val dDays = model.startTime.dDays - System.currentTimeMillis().dDays
        paint.color = if (dDays == 0L) {
            Color.BLUE
        } else if (dDays < 0) {
            Color.GRAY
        } else {
            Color.BLACK
        }
        paint.textSize = 20f.dp
        paint.isFakeBoldText = true
        canvas.drawText(
            model.startTime.dayOfMonth.toString(),
            drawingRect.left,
            drawingRect.bottom - 10f.dp,
            paint
        )
        paint.textSize = 12f.dp
        paint.isFakeBoldText = false
        canvas.drawText(model.startTime.dayOfWeekText, drawingRect.left, drawingRect.bottom - 34f.dp, paint)
        canvas.restore()
    }

    override fun updateDrawingRect(anchorPoint: Point) {
        drawingRect.left = originRect.left + anchorPoint.x
        drawingRect.right = originRect.right + anchorPoint.x
    }
}

data class DateLineModel(
    private val days: Int
) : ICalendarModel {
    override var startTime: Long = startOfDay().timeInMillis - dayMills * days
    override var endTime: Long = startTime + dayMills
}
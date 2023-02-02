package me.wxc.widget.components

import android.graphics.*
import me.wxc.widget.ICalendarComponent
import me.wxc.widget.ICalendarModel
import me.wxc.widget.tools.*

class NowLineComponent(override var model: NowLineModel) : ICalendarComponent<NowLineModel> {
    override val originRect: RectF = originRect()
    override val drawingRect: RectF = originRect()

    override fun onDraw(canvas: Canvas, paint: Paint) {
        if (drawingRect.centerY() - 4f.dp < dateLineHeight) return
        canvas.save()
        canvas.clipRect(clockWidth, drawingRect.top - 10f.dp, drawingRect.right, drawingRect.bottom + 10f.dp)
        paint.color = Color.RED
        paint.strokeWidth = 1f.dp
        canvas.drawLine(drawingRect.left + 8f.dp, drawingRect.centerY(), drawingRect.right - 2f.dp, drawingRect.centerY(), paint)
        canvas.drawCircle(drawingRect.left + 4f.dp, drawingRect.centerY(), 3f.dp, paint)
        canvas.restore()
    }

    override fun updateDrawingRect(anchorPoint: Point) {
        drawingRect.left = originRect.left + anchorPoint.x
        drawingRect.right = originRect.right + anchorPoint.x
        drawingRect.top = originRect.top + anchorPoint.y
        drawingRect.bottom = originRect.bottom + anchorPoint.y
    }
}

object NowLineModel : ICalendarModel {
    override val startTime: Long = System.currentTimeMillis()
    override val endTime: Long = System.currentTimeMillis()
}
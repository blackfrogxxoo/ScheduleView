package me.wxc.widget.schedule.components

import android.graphics.*
import me.wxc.widget.base.IScheduleComponent
import me.wxc.widget.base.IScheduleModel
import me.wxc.widget.schedule.clockWidth
import me.wxc.widget.schedule.dateLineHeight
import me.wxc.widget.schedule.originRect
import me.wxc.widget.schedule.refreshRect
import me.wxc.widget.tools.*

class NowLineComponent(override var model: NowLineModel) : IScheduleComponent<NowLineModel> {
    override val originRect: RectF = originRect()
    override val drawingRect: RectF = originRect()

    override fun onDraw(canvas: Canvas, paint: Paint) {
        if (drawingRect.centerY() - 4f.dp < dateLineHeight) return
        canvas.save()
        canvas.clipRect(
            clockWidth,
            drawingRect.top - 10f.dp,
            drawingRect.right,
            drawingRect.bottom + 10f.dp
        )
        paint.color = Color.RED
        paint.strokeWidth = 1f.dp
        canvas.drawLine(
            drawingRect.left + 8f.dp,
            drawingRect.centerY(),
            drawingRect.right - 2f.dp,
            drawingRect.centerY(),
            paint
        )
        canvas.drawCircle(drawingRect.left + 4f.dp, drawingRect.centerY(), 3f.dp, paint)
        canvas.restore()
    }

    override fun updateDrawingRect(anchorPoint: Point) {
        refreshRect()
        drawingRect.left = originRect.left + anchorPoint.x
        drawingRect.right = originRect.right + anchorPoint.x
        drawingRect.top = originRect.top + anchorPoint.y
        drawingRect.bottom = originRect.bottom + anchorPoint.y
    }
}

object NowLineModel : IScheduleModel {
    override val startTime: Long
        get() = System.currentTimeMillis()
    override val endTime: Long
        get() = System.currentTimeMillis()
}
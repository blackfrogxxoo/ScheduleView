package me.wxc.widget.components

import android.graphics.*
import me.wxc.widget.ICalendarComponent
import me.wxc.widget.ICalendarModel
import me.wxc.widget.tools.dateLineHeight
import me.wxc.widget.tools.dp
import me.wxc.widget.tools.screenWidth

class DateLineShadowComponent : ICalendarComponent<DateLineShadowModel> {
    override var model: DateLineShadowModel = DateLineShadowModel
    override val originRect: RectF = RectF(
        0f,
        dateLineHeight,
        screenWidth.toFloat(),
        dateLineHeight + 4f.dp
    )
    override val drawingRect: RectF = originRect

    private val shadowShader by lazy {
        LinearGradient(
            drawingRect.left,
            drawingRect.top,
            drawingRect.left,
            drawingRect.bottom,
            Color.parseColor("#20000000"),
            Color.parseColor("#00000000"),
            Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas, paint: Paint) {
        canvas.save()
        canvas.clipRect(drawingRect)
        paint.shader = shadowShader
        canvas.drawRect(drawingRect, paint)
        paint.shader = null
        canvas.restore()
    }

    override fun updateDrawingRect(anchorPoint: Point) {
        // do nothing
    }
}

object DateLineShadowModel: ICalendarModel {
    override val startTime: Long = 0
    override val endTime: Long = 0
}
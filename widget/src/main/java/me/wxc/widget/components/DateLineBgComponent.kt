package me.wxc.widget.components

import android.graphics.*
import me.wxc.widget.ICalendarComponent
import me.wxc.widget.ICalendarModel
import me.wxc.widget.tools.dayLineHeight
import me.wxc.widget.tools.dp
import me.wxc.widget.tools.screenWidth

class DateLineBgComponent : ICalendarComponent<DateLineBgModel> {
    override var model: DateLineBgModel = DateLineBgModel
    override val originRect: RectF = RectF(
        0f,
        0f,
        screenWidth.toFloat(),
        dayLineHeight
    )
    override val rect: RectF = originRect

    private val shadowShader by lazy {
        LinearGradient(
            0f,
            dayLineHeight,
            0f,
            dayLineHeight + 6f.dp,
            Color.parseColor("#33000000"),
            Color.parseColor("#00000000"),
            Shader.TileMode.CLAMP
        )
    }

    override fun drawSelf(canvas: Canvas, paint: Paint, anchorPoint: Point) {
        paint.color = Color.WHITE
        canvas.drawRect(rect, paint)
        paint.shader = shadowShader
        canvas.drawRect(rect.left, rect.bottom, rect.right, rect.bottom + 4f.dp, paint)
        paint.shader = null
    }

    override fun updateRect(anchorPoint: Point) {
        // do nothing
    }
}

object DateLineBgModel: ICalendarModel {
    override val startTime: Long = 0
    override val endTime: Long = 0
    override val level: Int
        get() = Int.MAX_VALUE - 1

}
package me.wxc.widget.schedule.components

import android.graphics.*
import me.wxc.widget.ScheduleConfig
import me.wxc.widget.base.IScheduleComponent
import me.wxc.widget.base.IScheduleModel
import me.wxc.widget.schedule.*
import me.wxc.widget.tools.*

class DateLineComponent : IScheduleComponent<DateLineModel> {
    override val model: DateLineModel = DateLineModel
    override val originRect: RectF = RectF(clockWidth, 0f, screenWidth.toFloat(), dateLineHeight)
    override val drawingRect: RectF = RectF(clockWidth, 0f, screenWidth.toFloat(), dateLineHeight)
    private val shadowRect: RectF = RectF(
        0f,
        dateLineHeight,
        screenWidth.toFloat(),
        dateLineHeight + 4f.dp
    )
    private var parentWidth = screenWidth
    private var scrollX = 0f

    private val shadowShader by lazy {
        LinearGradient(
            shadowRect.left,
            shadowRect.top,
            shadowRect.left,
            shadowRect.bottom,
            ScheduleConfig.colorTransparent2,
            ScheduleConfig.colorTransparent1,
            Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas, paint: Paint) {
        if (!ScheduleWidget.isThreeDay) return
        parentWidth = canvas.width
        canvas.save()
        canvas.clipRect(drawingRect)
        var x = -dayWidth
        while (x >= -dayWidth && x < parentWidth) {
            val startX =
                (x + scrollX) / dayWidth * dayWidth + clockWidth - (x + scrollX) % dayWidth
            val beginTime = beginOfDay().timeInMillis + startX.xToDDays * dayMillis
            val dDays = startX.xToDDays - nowMillis.dDays
            paint.color = if (dDays == 0L) {
                ScheduleConfig.colorBlue1
            } else if (dDays < 0) {
                ScheduleConfig.colorBlack3
            } else {
                ScheduleConfig.colorBlack1
            }
            paint.textSize = 20f.dp
            paint.isFakeBoldText = true
            canvas.drawText(
                beginTime.dayOfMonth.toString(),
                startX - scrollX,
                drawingRect.bottom - 10f.dp,
                paint
            )
            paint.textSize = 12f.dp
            paint.isFakeBoldText = false
            canvas.drawText(
                beginTime.dayOfWeekText,
                startX - scrollX,
                drawingRect.bottom - 34f.dp,
                paint
            )
            x += dayWidth
        }
        canvas.restore()

        canvas.save()
        canvas.clipRect(shadowRect)
        paint.shader = shadowShader
        canvas.drawRect(shadowRect, paint)
        paint.shader = null
        canvas.restore()
    }

    override fun updateDrawingRect(anchorPoint: Point) {
        scrollX = -anchorPoint.x.toFloat()
    }
}

object DateLineModel : IScheduleModel {
    override var beginTime: Long = 0
    override var endTime: Long = 0
}
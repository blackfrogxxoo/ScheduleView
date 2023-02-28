package me.wxc.widget.scheduler.components

import android.graphics.*
import me.wxc.widget.SchedulerConfig
import me.wxc.widget.base.ISchedulerComponent
import me.wxc.widget.base.ISchedulerModel
import me.wxc.widget.scheduler.SchedulerWidget
import me.wxc.widget.tools.*
import kotlin.math.roundToInt

class DateLineComponent(override var model: DateLineModel) : ISchedulerComponent<DateLineModel> {
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
            SchedulerConfig.colorTransparent2,
            SchedulerConfig.colorTransparent1,
            Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas, paint: Paint) {
        if (!SchedulerWidget.isThreeDay) return
        parentWidth = canvas.width
        canvas.save()
        canvas.clipRect(drawingRect)
        var x = -dayWidth
        while (x >= -dayWidth && x < parentWidth) {
            val startX = (x + scrollX).roundToInt() / dayWidth.roundToInt() * dayWidth + clockWidth
            val startTime = startOfDay().timeInMillis + startX.xToDDays * dayMillis
            val dDays = startX.xToDDays - System.currentTimeMillis().dDays
            paint.color = if (dDays == 0L) {
                SchedulerConfig.colorBlue1
            } else if (dDays < 0) {
                SchedulerConfig.colorBlack3
            } else {
                SchedulerConfig.colorBlack1
            }
            paint.textSize = 20f.dp
            paint.isFakeBoldText = true
            canvas.drawText(
                startTime.dayOfMonth.toString(),
                startX - scrollX,
                drawingRect.bottom - 10f.dp,
                paint
            )
            paint.textSize = 12f.dp
            paint.isFakeBoldText = false
            canvas.drawText(
                startTime.dayOfWeekText,
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
        scrollX = - anchorPoint.x.toFloat()
    }
}

object DateLineModel : ISchedulerModel {
    override var startTime: Long = 0
    override var endTime: Long = 0
}
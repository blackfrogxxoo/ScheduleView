package me.wxc.widget.scheduler.components

import android.graphics.*
import me.wxc.widget.SchedulerConfig
import me.wxc.widget.base.ISchedulerComponent
import me.wxc.widget.base.ISchedulerModel
import me.wxc.widget.tools.*
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.roundToInt

class WeekLineComponent(override var model: WeekLineModel) : ISchedulerComponent<WeekLineModel> {
    override val originRect: RectF = RectF(clockWidth, 0f, screenWidth.toFloat(), dateLineHeight)
    override val drawingRect: RectF = RectF(clockWidth, 0f, screenWidth.toFloat(), dateLineHeight)
    private val shadowRect: RectF = RectF(
        0f,
        dateLineHeight,
        screenWidth.toFloat(),
        dateLineHeight + 4f.dp
    )
    private var parentWidth = screenWidth
    private val dayWidth = (screenWidth - clockWidth) / 7
    private var scrollX = 0f
    private var parentScrollX = 0f
    private var radius = 13f.dp

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
        parentWidth = canvas.width
        canvas.save()
        canvas.clipRect(drawingRect)
        var x = clockWidth
        val calendar = startOfDay().apply {
            add(Calendar.DAY_OF_YEAR, (parentScrollX + clockWidth).xToDDays)
            set(Calendar.DAY_OF_WEEK, 1)
        }
        paint.textAlign = Paint.Align.CENTER
        while (x >= clockWidth && x < parentWidth) {
            val dDays = calendar.timeInMillis.dDays
            val centerX = x + dayWidth / 2
            val selected = calendar.timeInMillis.dDays.toInt() == (parentScrollX + clockWidth).xToDDays
            if (selected) {
                paint.color = if (dDays != 0L) {
                    SchedulerConfig.colorBlack4
                } else {
                    SchedulerConfig.colorBlue1
                }
                canvas.drawCircle(
                    centerX,
                    drawingRect.bottom - 20f.dp,
                    radius,
                    paint
                )
            }
            paint.color = if (dDays == 0L) {
                if (selected) {
                    SchedulerConfig.colorWhite
                } else {
                    SchedulerConfig.colorBlue1
                }
            } else if (dDays < 0) {
                SchedulerConfig.colorBlack3
            } else {
                SchedulerConfig.colorBlack1
            }
            paint.textSize = 16f.dp
            paint.isFakeBoldText = true
            canvas.drawText(
                calendar.timeInMillis.dayOfMonth.toString(),
                centerX,
                drawingRect.bottom - 14f.dp,
                paint
            )
            paint.color = if (dDays == 0L) {
                SchedulerConfig.colorBlue1
            } else if (dDays < 0) {
                SchedulerConfig.colorBlack3
            } else {
                SchedulerConfig.colorBlack1
            }
            paint.textSize = 11f.dp
            paint.isFakeBoldText = false
            canvas.drawText(
                calendar.timeInMillis.dayOfWeekTextSimple,
                centerX,
                drawingRect.bottom - 40f.dp,
                paint
            )
            x += dayWidth
            calendar.add(Calendar.DAY_OF_WEEK, 1)
        }
        paint.textAlign = Paint.Align.LEFT
        canvas.restore()

        canvas.save()
        canvas.clipRect(shadowRect)
        paint.shader = shadowShader
        canvas.drawRect(shadowRect, paint)
        paint.shader = null
        canvas.restore()
    }

    private val weekWidth = 7 * dayWidth
    override fun updateDrawingRect(anchorPoint: Point) {
        parentScrollX = -anchorPoint.x.toFloat()
        val oldScrollX = scrollX.roundToInt() / weekWidth.roundToInt() * weekWidth
        if (abs(-anchorPoint.x - oldScrollX) > weekWidth) {
            val dest = -anchorPoint.x
            scrollX = dest / weekWidth.roundToInt() * weekWidth
        }
    }
}

object WeekLineModel : ISchedulerModel {
    override var startTime: Long = 0
    override var endTime: Long = 0
}
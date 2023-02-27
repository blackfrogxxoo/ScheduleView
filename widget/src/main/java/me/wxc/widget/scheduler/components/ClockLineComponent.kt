package me.wxc.widget.scheduler.components

import android.graphics.*
import me.wxc.widget.SchedulerConfig
import me.wxc.widget.base.ISchedulerComponent
import me.wxc.widget.base.ISchedulerModel
import me.wxc.widget.tools.*

class ClockLineComponent(override var model: ClockLineModel) : ISchedulerComponent<ClockLineModel> {
    override val originRect: RectF = originRect().apply {
        left = 0f
        right = parentWidth.toFloat()
        if (model.clock == 24) {
            top += dayHeight
            bottom += dayHeight
        }
    }
    override val drawingRect: RectF = originRect().apply {
        left = 0f
        right = parentWidth.toFloat()
        if (model.clock == 24) {
            top += dayHeight
            bottom += dayHeight
        }
    }
    private val anchorPoint: Point = Point()
    private val pathEffect = DashPathEffect(floatArrayOf(dayWidth - 3f.dp, 3f.dp), 1f)
    private var parentWidth = screenWidth
    private var parentHeight = screenHeight

    override fun onDraw(canvas: Canvas, paint: Paint) {
        parentWidth = canvas.width
        parentHeight = canvas.height
        canvas.save()
        canvas.clipRect(0f, dateLineHeight, parentWidth.toFloat(), parentHeight.toFloat())
        paint.color = SchedulerConfig.colorBlack4
        paint.textSize = 12f.dp
        val startX = 12f.dp
        val y = drawingRect.top
        canvas.drawText(model.showText, startX, y + 4f.dp, paint)
        paint.color = SchedulerConfig.colorBlack4
        paint.strokeWidth = 1f
        val stopX = parentWidth.toFloat()
        paint.pathEffect = pathEffect
        canvas.drawLine(drawingRect.left + clockWidth + 1.5f.dp, y, stopX, y, paint)
        paint.pathEffect = null
        // 绘制创建中的日程时间
        model.createTaskModel?.let {
            val startTime = it.draggingRect?.topPoint()
                ?.positionToTime(scrollX = -anchorPoint.x, scrollY = -anchorPoint.y) ?: it.startTime
            val endTime = it.draggingRect?.bottomPoint()
                ?.positionToTime(scrollX = -anchorPoint.x, scrollY = -anchorPoint.y) ?: it.endTime
            val adjustedStartTime = startTime.adjustTimeInDay(
                quarterMillis, true
            )
            val adjustedEndTime = endTime.adjustTimeInDay(
                quarterMillis, true
            )
            val createTop =
                drawingRect.top + dayHeight * (adjustedStartTime - startOfDay(adjustedStartTime).timeInMillis - model.startTime + startOfDay(
                    model.startTime
                ).timeInMillis) / (hourMillis * 24)
            val createBottom =
                drawingRect.top + dayHeight * (adjustedEndTime - startOfDay(adjustedEndTime).timeInMillis - model.startTime + startOfDay(
                    model.startTime
                ).timeInMillis) / (hourMillis * 24)
            paint.color = SchedulerConfig.colorBlue1
            if (createTop >= drawingRect.top && createTop < drawingRect.bottom && model.clock != 24) {
                canvas.drawText(adjustedStartTime.hhMM, startX, createTop + 4f.dp, paint)
            }
            if (createBottom >= drawingRect.top && createBottom < drawingRect.bottom) {
                val text = if (adjustedEndTime.hhMM == "00:00" && model.clock == 24) "24:00" else adjustedEndTime.hhMM
                canvas.drawText(text, startX, createBottom + 4f.dp, paint)
            }
        }
        canvas.restore()
    }

    override fun updateDrawingRect(anchorPoint: Point) {
        this.anchorPoint.x = anchorPoint.x
        this.anchorPoint.y = anchorPoint.y
        drawingRect.top = originRect.top + anchorPoint.y
        drawingRect.bottom = originRect.bottom + anchorPoint.y
    }
}

data class ClockLineModel(
    val clock: Int,
    var createTaskModel: CreateTaskModel? = null
) : ISchedulerModel {
    private val zeroClock = startOfDay()
    override val startTime: Long = zeroClock.timeInMillis + clock * hourMillis
    override val endTime: Long = zeroClock.timeInMillis + (clock + 1) * hourMillis
    val showText: String
        get() = if (clock == 24) "24:00" else startTime.hhMM
}
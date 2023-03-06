package me.wxc.widget.schedule.components

import android.graphics.*
import me.wxc.widget.ScheduleConfig
import me.wxc.widget.base.IScheduleComponent
import me.wxc.widget.base.IScheduleEditable
import me.wxc.widget.base.IScheduleModel
import me.wxc.widget.base.RepeatMode
import me.wxc.widget.schedule.clockWidth
import me.wxc.widget.schedule.dateLineHeight
import me.wxc.widget.schedule.dayWidth
import me.wxc.widget.schedule.originRect
import me.wxc.widget.tools.*
import kotlin.math.roundToInt

class DailyTaskComponent(override var model: DailyTaskModel) : IScheduleComponent<DailyTaskModel>,
    IScheduleEditable {
    override val drawingRect: RectF = originRect()
    override val originRect: RectF = originRect()
    private val bgColor = if (model.expired) {
        ScheduleConfig.colorBlue5
    } else {
        ScheduleConfig.colorBlue4
    }
    private val textColor = if (model.expired) {
        ScheduleConfig.colorBlue2
    } else {
        ScheduleConfig.colorBlue1
    }
    private val circleRadius = 4f.dp
    private val circlePadding = 20f.dp

    private val shader: Shader
        get() = run {
            val color1 = ScheduleConfig.colorTransparent1
            val color2 = bgColor
            val colors = intArrayOf(color1, color1, color2, color2)
            val positions = floatArrayOf(0f, 0.5f, 0.5f, 1.0f)
            LinearGradient(
                drawingRect.left,
                drawingRect.top,
                drawingRect.left + 6f.dp,
                drawingRect.top + 6f.dp,
                colors,
                positions,
                Shader.TileMode.REPEAT
            )
        }
    private val pathEffect = DashPathEffect(floatArrayOf(3f.dp, 1.5f.dp), 1f)
    private var parentWidth = screenWidth
    private var parentHeight = screenHeight

    override val editable: Boolean = false
    override val editingRect: RectF? = null

    override fun onDraw(canvas: Canvas, paint: Paint) {
        parentWidth = canvas.width
        parentHeight = canvas.height
        if (drawingRect.bottom < dateLineHeight) return
        canvas.save()
        canvas.clipRect(clockWidth, dateLineHeight, parentWidth.toFloat(), parentHeight.toFloat())
        paint.shader = shader
        paint.alpha = 255
        canvas.drawRoundRect(
            drawingRect.left + 4f.dp,
            drawingRect.top + 2f.dp,
            drawingRect.right - 4f.dp,
            drawingRect.bottom - 2f.dp,
            4f.dp,
            4f.dp,
            paint
        )
        paint.shader = null
        paint.alpha = 255
        paint.style = Paint.Style.STROKE
        paint.pathEffect = pathEffect
        paint.strokeWidth = 1f.dp
        paint.color = textColor
        canvas.drawRoundRect(
            drawingRect.left + 4f.dp,
            drawingRect.top + 2f.dp,
            drawingRect.right - 4f.dp,
            drawingRect.bottom - 2f.dp,
            4f.dp,
            4f.dp,
            paint
        )
        paint.pathEffect = null
        paint.style = Paint.Style.FILL
        paint.textSize = 14f.dp
        canvas.drawText(
            model.title,
            drawingRect.left + 12f.dp,
            drawingRect.top + 22f.dp,
            paint,
            drawingRect.width() - 12f.dp
        )
        if (editable) { // TODO 长按改变editable
            drawUpdatingRect(canvas, paint, drawingRect)
        }
        canvas.restore()
    }

    private fun drawUpdatingRect(canvas: Canvas, paint: Paint, drawRect: RectF) {
        paint.color = ScheduleConfig.colorBlue1
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.5f.dp
        canvas.drawRoundRect(
            drawRect.left + 2f.dp,
            drawRect.top,
            drawRect.right - 2f.dp,
            drawRect.bottom,
            4f.dp,
            4f.dp,
            paint
        )
        paint.style = Paint.Style.FILL
        paint.color = ScheduleConfig.colorWhite
        canvas.drawCircle(drawRect.right - circlePadding, drawRect.top, circleRadius, paint)
        canvas.drawCircle(drawRect.left + circlePadding, drawRect.bottom, circleRadius, paint)
        paint.color = ScheduleConfig.colorBlue1
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f.dp
        canvas.drawCircle(drawRect.right - circlePadding, drawRect.top, circleRadius, paint)
        canvas.drawCircle(drawRect.left + circlePadding, drawRect.bottom, circleRadius, paint)
        paint.style = Paint.Style.FILL
    }

    override fun updateDrawingRect(anchorPoint: Point) {
        drawingRect.left = originRect.left + anchorPoint.x
        drawingRect.right = originRect.right + anchorPoint.x
        drawingRect.top = originRect.top + anchorPoint.y
        drawingRect.bottom = originRect.bottom + anchorPoint.y
    }

    override fun setCoincidedScheduleModels(coincided: List<IScheduleModel>) {
        if (coincided.isNotEmpty()) {
            // TODO 优化时间冲突的日程UI
            val index = coincided.indexOf(model)
            val size = coincided.size
            val padding = 15f.dp
            val width = (dayWidth - (size - 1) * padding).coerceAtLeast(50f.dp)
            if (originRect.width().roundToInt() == dayWidth.roundToInt()) {
                val originR = originRect.right
                originRect.left = (originRect.left + index * padding).coerceAtMost(originR - 50f.dp)
                originRect.right = (originRect.left + width).coerceAtMost(originR)
            }
        }
    }
}

data class DailyTaskModel(
    val id: Long = 0,
    override var startTime: Long = System.currentTimeMillis(),
    var duration: Long = hourMillis,
    var title: String,
    var repeatId: String,
    var repeatMode: RepeatMode = RepeatMode.Never
) : IScheduleModel {
    override val endTime: Long
        get() = startTime + duration

    fun changeStartTime(time: Long) {
        val temp = startTime
        startTime = time
        duration -= time - temp
    }

    fun changeEndTime(time: Long) {
        duration += time - endTime
    }

    internal val expired: Boolean
        get() = System.currentTimeMillis() > endTime
}
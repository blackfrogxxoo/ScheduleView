package me.wxc.widget.scheduler.components

import android.graphics.*
import me.wxc.widget.SchedulerConfig
import me.wxc.widget.base.ISchedulerComponent
import me.wxc.widget.base.ISchedulerEditable
import me.wxc.widget.base.ISchedulerModel
import me.wxc.widget.tools.*

class DailyTaskComponent(override var model: DailyTaskModel) : ISchedulerComponent<DailyTaskModel>,
    ISchedulerEditable {
    override val drawingRect: RectF = originRect()
    override val originRect: RectF = originRect()
    private val bgColor = if (model.expired) {
        SchedulerConfig.colorBlue5
    } else {
        SchedulerConfig.colorBlue4
    }
    private val textColor = if (model.expired) {
        SchedulerConfig.colorBlue2
    } else {
        SchedulerConfig.colorBlue1
    }
    private val circleRadius = 4f.dp
    private val circlePadding = 20f.dp

    private val shader: Shader
        get() = run {
            val color1 = SchedulerConfig.colorTransparent1
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
        canvas.drawText(model.title, drawingRect.left + 12f.dp, drawingRect.top + 22f.dp, paint, drawingRect.width() - 12f.dp)
        if (editable) { // TODO 长按改变editable
            drawUpdatingRect(canvas, paint, drawingRect)
        }
        canvas.restore()
    }

    private fun drawUpdatingRect(canvas: Canvas, paint: Paint, drawRect: RectF) {
        paint.color = SchedulerConfig.colorBlue1
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
        paint.color = SchedulerConfig.colorWhite
        canvas.drawCircle(drawRect.right - circlePadding, drawRect.top, circleRadius, paint)
        canvas.drawCircle(drawRect.left + circlePadding, drawRect.bottom, circleRadius, paint)
        paint.color = SchedulerConfig.colorBlue1
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
}

data class DailyTaskModel(
    val id: Long = 0,
    override var startTime: Long = System.currentTimeMillis(),
    var duration: Long = hourMillis,
    var title: String,
) : ISchedulerModel {
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
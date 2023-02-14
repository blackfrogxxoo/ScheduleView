package me.wxc.widget.components

import android.graphics.*
import me.wxc.widget.ICalendarComponent
import me.wxc.widget.ICalendarEditable
import me.wxc.widget.ICalendarModel
import me.wxc.widget.tools.*

class DailyTaskComponent(override var model: DailyTaskModel) : ICalendarComponent<DailyTaskModel>,
    ICalendarEditable {
    override val drawingRect: RectF = originRect()
    override val originRect: RectF = originRect()
    private val bgColor = if (model.expired) {
        Color.parseColor("#F2F2FF")
    } else {
        Color.parseColor("#EEEEFF")
    }
    private val textColor = if (model.expired) {
        Color.parseColor("#DDDDFF")
    } else {
        Color.parseColor("#5555FF")
    }
    private val circleRadius = 4f.dp
    private val circlePadding = 20f.dp

    private val shader: Shader
        get() = run {
            val color1 = Color.WHITE
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
        canvas.drawText(model.title, drawingRect.left + 12f.dp, drawingRect.top + 22f.dp, paint)
        if (editable) { // TODO 长按改变editable
            drawUpdatingRect(canvas, paint, drawingRect)
        }
        canvas.restore()
    }

    private fun drawUpdatingRect(canvas: Canvas, paint: Paint, drawRect: RectF) {
        paint.color = Color.parseColor("#4444ff")
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
        paint.color = Color.WHITE
        canvas.drawCircle(drawRect.right - circlePadding, drawRect.top, circleRadius, paint)
        canvas.drawCircle(drawRect.left + circlePadding, drawRect.bottom, circleRadius, paint)
        paint.color = Color.parseColor("#4444ff")
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
    val duration: Long = hourMills,
    var title: String,
) : ICalendarModel {
    override val endTime: Long
        get() = startTime + duration

    internal val expired: Boolean
        get() = System.currentTimeMillis() > endTime
}
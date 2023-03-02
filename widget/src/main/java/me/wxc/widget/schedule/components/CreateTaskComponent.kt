package me.wxc.widget.schedule.components

import android.graphics.*
import android.util.Log
import android.view.MotionEvent
import me.wxc.widget.ScheduleConfig
import me.wxc.widget.base.IScheduleComponent
import me.wxc.widget.base.IScheduleComponent.Companion.TAG
import me.wxc.widget.base.IScheduleEditable
import me.wxc.widget.base.IScheduleModel
import me.wxc.widget.base.RepeatMode
import me.wxc.widget.tools.*
import kotlin.math.abs
import kotlin.math.roundToInt

class CreateTaskComponent(override var model: CreateTaskModel) :
    IScheduleComponent<CreateTaskModel>, IScheduleEditable {
    override val originRect: RectF = originRect()
    override val drawingRect: RectF = originRect()
    private val circleRadius = 4f.dp
    private val circlePadding = 20f.dp
    private val anchorPoint: Point = Point()
    private var parentWidth = screenWidth
    private var parentHeight = screenHeight

    override val editable: Boolean = true
    override val editingRect: RectF? = null

    override fun onDraw(canvas: Canvas, paint: Paint) {
        parentWidth = canvas.width
        parentHeight = canvas.height
        val drawRect = model.draggingRect ?: drawingRect
        if (drawRect.bottom < dateLineHeight) return
        canvas.save()
        canvas.clipRect(clockWidth, dateLineHeight, canvas.width.toFloat(), canvas.height.toFloat())
        paint.color = ScheduleConfig.colorBlue3
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(
            drawRect.left + 2f.dp,
            drawRect.top,
            drawRect.right - 2f.dp,
            drawRect.bottom,
            4f.dp,
            4f.dp,
            paint
        )
        drawUpdatingRect(canvas, paint, drawRect)
        paint.style = Paint.Style.FILL
        paint.textSize = 14f.dp
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(model.title.ifBlank { "新建日程" }, drawRect.centerX(), drawRect.centerY() + 5f.dp, paint, drawRect.width())
        paint.textAlign = Paint.Align.LEFT
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
        if (model.state == CreateTaskModel.State.DRAG_TOP) {
            model.draggingRect?.run {
                bottom += anchorPoint.y - this@CreateTaskComponent.anchorPoint.y
            }
        } else if (model.state == CreateTaskModel.State.DRAG_BOTTOM) {
            model.draggingRect?.run {
                top += anchorPoint.y - this@CreateTaskComponent.anchorPoint.y
            }
        }
        this.anchorPoint.x = anchorPoint.x
        this.anchorPoint.y = anchorPoint.y
    }

    private var downX = 0f
    private var downY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var downTimestamp = System.currentTimeMillis()
    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                downTimestamp = System.currentTimeMillis()
                downX = e.x
                downY = e.y
                lastX = e.x
                lastY = e.y
                val downOnCreate =
                    e.ifInRect(drawingRect, padding = 4.dp)
                val downOnCreateTop = e.ifAtRectTop(drawingRect, padding = 4.dp)
                val downOnCreateBottom = e.ifAtRectBottom(drawingRect, padding = 4.dp)
                model.state = if (downOnCreateBottom) {
                    CreateTaskModel.State.DRAG_BOTTOM
                } else if (downOnCreateTop) {
                    CreateTaskModel.State.DRAG_TOP
                } else if (downOnCreate) {
                    CreateTaskModel.State.DRAG_BODY
                } else {
                    CreateTaskModel.State.IDLE
                }
            }
            MotionEvent.ACTION_MOVE -> {
                val distanceX = lastX - e.x
                val distanceY = lastY - e.y
                when (model.state) {
                    CreateTaskModel.State.IDLE -> {}
                    CreateTaskModel.State.DRAG_BODY -> {
                        // 拖动过程中独立绘制，不受scrollX/Y影响
                        val topAtLeast = zeroClockY.coerceAtMost(drawingRect.top)
                        val topAtMost =
                            (parentHeight - drawingRect.height() - canvasPadding).coerceAtLeast(
                                drawingRect.top
                            )
                        if (model.draggingRect == null) {
                            model.draggingRect = RectF(
                                drawingRect.left,
                                drawingRect.top,
                                drawingRect.right,
                                drawingRect.bottom
                            )
                        }
                        model.draggingRect!!.run {
                            val destTop = (top - distanceY)
                                .coerceAtLeast(topAtLeast)
                                .coerceAtMost(topAtMost)
                            val movedY = destTop - top
                            left -= distanceX
                            right -= distanceX
                            top += movedY
                            bottom += movedY
                            // 即将超出屏幕时，滑动View
                            if (left + (dayWidth / 3).coerceAtMost(50f.dp) < clockWidth) {
                                model.onNeedScrollBlock(-dayWidth.roundToInt(), 0)
                            } else if (left + width() - (dayWidth / 3).coerceAtMost(50f.dp) > parentWidth) {
                                model.onNeedScrollBlock(dayWidth.roundToInt(), 0)
                            }
                            if (distanceY > 0 && top - 50f.dp < zeroClockY) {
                                model.onNeedScrollBlock(0, -2 * clockHeight.roundToInt())
                            } else if (distanceY < 0 && bottom + 50f.dp > parentHeight) {
                                model.onNeedScrollBlock(0, 2 * clockHeight.roundToInt())
                            } else {
                                // do nothing
                            }
                        }
                    }
                    CreateTaskModel.State.DRAG_TOP -> {
                        // 拖动过程中独立绘制，不受scrollX/Y影响
                        if (model.draggingRect == null) {
                            model.draggingRect = RectF(
                                drawingRect.left,
                                drawingRect.top,
                                drawingRect.right,
                                drawingRect.bottom
                            )
                        }
                        model.draggingRect!!.run {
                            val destTop = (top - distanceY)
                                .coerceAtLeast(zeroClockY)
                                .coerceAtMost(parentHeight - clockHeight / 2 - canvasPadding)
                            top = destTop.coerceAtMost(bottom - clockHeight / 2)
                            if (top - 50f.dp < zeroClockY) {
                                model.onNeedScrollBlock(0, -2 * clockHeight.roundToInt())
                            }
                        }
                    }
                    CreateTaskModel.State.DRAG_BOTTOM -> {
                        // 拖动过程中独立绘制，不受scrollX/Y影响
                        if (model.draggingRect == null) {
                            model.draggingRect = RectF(
                                drawingRect.left,
                                drawingRect.top,
                                drawingRect.right,
                                drawingRect.bottom
                            )
                        }
                        model.draggingRect!!.run {
                            val destBottom = (bottom - distanceY)
                                .coerceAtLeast(zeroClockY)
                                .coerceAtMost((parentHeight - canvasPadding).toFloat())
                            bottom = destBottom.coerceAtLeast(top + clockHeight / 2)
                            if (bottom + 50f.dp > parentHeight) {
                                model.onNeedScrollBlock(0, 2 * clockHeight.roundToInt())
                            }
                        }
                    }
                }
                lastX = e.x
                lastY = e.y
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (e.action == MotionEvent.ACTION_UP && abs(lastX - downX) < 5.dp && abs(lastY - downY) < 5.dp && System.currentTimeMillis() - downTimestamp < 1000) {
                    ScheduleConfig.onCreateTaskClickBlock(model)
                    return true
                }
                model.run {
                    draggingRect?.let {
                        startTime = it.topPoint()!!
                            .positionToTime(scrollX = -anchorPoint.x, scrollY = -anchorPoint.y)
                        duration = it.bottomPoint()!!.positionToTime(
                            scrollX = -anchorPoint.x,
                            scrollY = -anchorPoint.y
                        ) - startTime
                    }
                    startTime = startTime.adjustTimeInDay(quarterMillis, true)
                    duration = duration.adjustTimeSelf(quarterMillis, true)
                    Log.i(
                        TAG,
                        "updated task: ${sdf_yyyyMMddHHmmss.format(startTime)}, ${1f * duration / hourMillis}"
                    )
                    draggingRect = null
                    refreshRect()
                }
            }
        }

        return model.state != CreateTaskModel.State.IDLE
    }
}

data class CreateTaskModel(
    override var startTime: Long = System.currentTimeMillis(),
    var duration: Long = hourMillis / 2,
    var title: String = "",
    var draggingRect: RectF? = null,
    var state: State = State.IDLE,
    var repeatMode: RepeatMode = RepeatMode.Never,
    val onNeedScrollBlock: (x: Int, y: Int) -> Unit
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

    enum class State {
        IDLE, DRAG_BODY, DRAG_TOP, DRAG_BOTTOM
    }
}
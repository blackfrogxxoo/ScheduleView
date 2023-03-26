package me.wxc.widget.schedule.components

import android.graphics.*
import android.util.Log
import android.view.MotionEvent
import me.wxc.widget.ScheduleConfig
import me.wxc.widget.base.*
import me.wxc.widget.schedule.*
import me.wxc.widget.tools.*
import kotlin.math.abs
import kotlin.math.roundToInt

class DailyTaskComponent(override val model: DailyTaskModel) :
    IScheduleComponent<DailyTaskModel> {
    override val originRect: RectF = originRect()
    override val drawingRect: RectF = originRect()
    private val circleRadius = 4f.dp
    private val circlePadding = 20f.dp
    private val anchorPoint: Point = Point()
    internal var existEditing = false
    private val bgColor: Int
        get() = if (existEditing) {
            ScheduleConfig.colorBlue6
        } else if (model.expired) {
            ScheduleConfig.colorBlue5
        } else {
            ScheduleConfig.colorBlue4
        }
    private val textColor: Int
        get() = if (existEditing) {
            ScheduleConfig.colorBlue4
        } else if (model.expired) {
            ScheduleConfig.colorBlue2
        } else {
            ScheduleConfig.colorBlue1
        }

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

    override fun onDraw(canvas: Canvas, paint: Paint) {
        if (model.id > 0) {
            drawExistsTask(canvas, paint)
        } else {
            drawCreateTask(canvas, paint)
        }
    }

    private fun drawCreateTask(canvas: Canvas, paint: Paint) {
        parentWidth = canvas.width
        parentHeight = canvas.height
        val drawRect = model.editingTaskModel?.draggingRect ?: drawingRect
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
        canvas.drawText(
            model.title.ifBlank { "新建日程" },
            drawRect.centerX(),
            drawRect.centerY() + 5f.dp,
            paint,
            drawRect.width(),
            autoFeed = model.duration > hourMillis / 2
        )
        paint.textAlign = Paint.Align.LEFT
        canvas.restore()
    }

    private fun drawExistsTask(canvas: Canvas, paint: Paint) {
        parentWidth = canvas.width
        parentHeight = canvas.height
        val drawRect = model.editingTaskModel?.draggingRect ?: drawingRect
        if (drawRect.bottom < dateLineHeight) return
        canvas.save()
        canvas.clipRect(clockWidth, dateLineHeight, parentWidth.toFloat(), parentHeight.toFloat())
        // 浅灰底色
        paint.color = ScheduleConfig.colorBlack6
        canvas.drawRoundRect(
            drawRect.left + 4f.dp,
            drawRect.top + 2f.dp,
            drawRect.right - 4f.dp,
            drawRect.bottom - 2f.dp,
            4f.dp,
            4f.dp,
            paint
        )
        // 背景
        paint.shader = shader
        paint.alpha = 255
        canvas.drawRoundRect(
            drawRect.left + 4f.dp,
            drawRect.top + 2f.dp,
            drawRect.right - 4f.dp,
            drawRect.bottom - 2f.dp,
            4f.dp,
            4f.dp,
            paint
        )
        paint.shader = null
        paint.alpha = 255
        // 背景
        paint.style = Paint.Style.STROKE
        paint.pathEffect = pathEffect
        paint.strokeWidth = 1f.dp
        paint.color = textColor
        canvas.drawRoundRect(
            drawRect.left + 4f.dp,
            drawRect.top + 2f.dp,
            drawRect.right - 4f.dp,
            drawRect.bottom - 2f.dp,
            4f.dp,
            4f.dp,
            paint
        )
        paint.pathEffect = null
        paint.style = Paint.Style.FILL
        paint.textSize = 14f.dp
        canvas.drawText(
            model.title,
            drawRect.left + 12f.dp,
            drawRect.top + 22f.dp,
            paint,
            drawRect.width() - 12f.dp,
            autoFeed = model.duration > quarterMillis * 3
        )
        if (model.editingTaskModel != null) {
            drawUpdatingRect(canvas, paint, drawRect)
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
        if (model.editingTaskModel?.state == State.DRAG_TOP) {
            model.editingTaskModel.draggingRect?.run {
                bottom += anchorPoint.y - this@DailyTaskComponent.anchorPoint.y
            }
        } else if (model.editingTaskModel?.state == State.DRAG_BOTTOM) {
            model.editingTaskModel.draggingRect?.run {
                top += anchorPoint.y - this@DailyTaskComponent.anchorPoint.y
            }
        }
        this.anchorPoint.x = anchorPoint.x
        this.anchorPoint.y = anchorPoint.y
    }

    override fun setCoincidedScheduleModels(coincided: List<IScheduleModel>) {
        if (coincided.any()) {
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

    internal var downX = 0f
    internal var downY = 0f
    internal var lastX = 0f
    internal var lastY = 0f
    internal var downTimestamp = nowMillis
    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                downTimestamp = nowMillis
                downX = e.x
                downY = e.y
                lastX = e.x
                lastY = e.y
                val downOnCreate =
                    e.ifInRect(drawingRect, padding = 4.dp)
                val downOnCreateTop = e.ifAtRectTop(drawingRect, padding = 6.dp)
                val downOnCreateBottom = e.ifAtRectBottom(drawingRect, padding = 6.dp)
                model.editingTaskModel?.state = if (downOnCreateBottom) {
                    State.DRAG_BOTTOM
                } else if (downOnCreateTop) {
                    State.DRAG_TOP
                } else if (downOnCreate) {
                    State.DRAG_BODY
                } else {
                    State.IDLE
                }
                Log.i(TAG, "onTouchEvent: ${model.editingTaskModel?.state}")
            }
            MotionEvent.ACTION_MOVE -> {
                val distanceX = lastX - e.x
                val distanceY = lastY - e.y
                when (model.editingTaskModel?.state) {
                    State.IDLE -> {}
                    State.DRAG_BODY -> {
                        // 拖动过程中独立绘制，不受scrollX/Y影响
                        val topAtLeast = zeroClockY.coerceAtMost(drawingRect.top)
                        val topAtMost =
                            (parentHeight - drawingRect.height() - canvasPadding).coerceAtLeast(
                                drawingRect.top
                            )
                        if (model.editingTaskModel.draggingRect == null) {
                            model.editingTaskModel.draggingRect = RectF(
                                drawingRect.left,
                                drawingRect.top,
                                drawingRect.right,
                                drawingRect.bottom
                            )
                        }
                        model.editingTaskModel.draggingRect!!.run {
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
                                model.editingTaskModel.onNeedScrollBlock.invoke(
                                    -dayWidth.roundToInt(),
                                    0
                                )
                            } else if (left + width() - (dayWidth / 3).coerceAtMost(50f.dp) > parentWidth) {
                                model.editingTaskModel.onNeedScrollBlock.invoke(
                                    dayWidth.roundToInt(),
                                    0
                                )
                            }
                            if (distanceY > 0 && top - 50f.dp < zeroClockY) {
                                model.editingTaskModel.onNeedScrollBlock.invoke(
                                    0,
                                    -2 * clockHeight.roundToInt()
                                )
                            } else if (distanceY < 0 && bottom + 50f.dp > parentHeight) {
                                model.editingTaskModel.onNeedScrollBlock?.invoke(
                                    0,
                                    2 * clockHeight.roundToInt()
                                )
                            } else {
                                // do nothing
                            }
                        }
                    }
                    State.DRAG_TOP -> {
                        // 拖动过程中独立绘制，不受scrollX/Y影响
                        if (model.editingTaskModel.draggingRect == null) {
                            model.editingTaskModel.draggingRect = RectF(
                                drawingRect.left,
                                drawingRect.top,
                                drawingRect.right,
                                drawingRect.bottom
                            )
                        }
                        model.editingTaskModel.draggingRect!!.run {
                            val destTop = (top - distanceY)
                                .coerceAtLeast(zeroClockY)
                                .coerceAtMost(parentHeight - clockHeight / 2 - canvasPadding)
                            top = destTop.coerceAtMost(bottom - clockHeight / 2)
                            if (top - 50f.dp < zeroClockY) {
                                model.editingTaskModel.onNeedScrollBlock.invoke(
                                    0,
                                    -2 * clockHeight.roundToInt()
                                )
                            }
                        }
                    }
                    State.DRAG_BOTTOM -> {
                        // 拖动过程中独立绘制，不受scrollX/Y影响
                        if (model.editingTaskModel.draggingRect == null) {
                            model.editingTaskModel.draggingRect = RectF(
                                drawingRect.left,
                                drawingRect.top,
                                drawingRect.right,
                                drawingRect.bottom
                            )
                        }
                        model.editingTaskModel.draggingRect!!.run {
                            val destBottom = (bottom - distanceY)
                                .coerceAtLeast(zeroClockY)
                                .coerceAtMost((parentHeight - canvasPadding).toFloat())
                            bottom = destBottom.coerceAtLeast(top + clockHeight / 2)
                            if (bottom + 50f.dp > parentHeight) {
                                model.editingTaskModel.onNeedScrollBlock.invoke(
                                    0,
                                    2 * clockHeight.roundToInt()
                                )
                            }
                        }
                    }
                    else -> {}
                }
                lastX = e.x
                lastY = e.y
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (e.action == MotionEvent.ACTION_UP && abs(lastX - downX) < 5.dp && abs(lastY - downY) < 5.dp && nowMillis - downTimestamp < 1000) {
                    if (model.id == 0L) {
                        ScheduleConfig.onCreateTaskClickBlock(model)
                    }
                    return true
                }
                model.run {
                    editingTaskModel?.draggingRect?.let {
                        beginTime = it.topPoint()!!
                            .positionToTime(scrollX = -anchorPoint.x, scrollY = -anchorPoint.y)
                        duration = it.bottomPoint()!!.positionToTime(
                            scrollX = -anchorPoint.x,
                            scrollY = -anchorPoint.y
                        ) - beginTime
                    }
                    beginTime = beginTime.adjustTimestamp(quarterMillis, true)
                    duration = duration.adjustDuration(quarterMillis, true)
                    Log.i(
                        TAG,
                        "updated task: ${beginTime.yyyyMMddHHmmss}, ${1f * duration / hourMillis}"
                    )
                    editingTaskModel?.draggingRect = null
                    refreshRect()
                    if (model.id != 0L) {
                        ScheduleConfig.onTaskDraggedBlock(model)
                    }
                }
            }
        }

        return model.editingTaskModel?.state != State.IDLE
    }

    companion object {
        private const val TAG = "DailyTaskComponent"
    }
}

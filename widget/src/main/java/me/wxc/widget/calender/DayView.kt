package me.wxc.widget.calender

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.View
import me.wxc.widget.base.ICalendarRender
import me.wxc.widget.base.ISchedulerModel
import me.wxc.widget.scheduler.components.DailyTaskModel
import me.wxc.widget.tools.*
import java.util.*
import kotlin.properties.Delegates

class DayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), ICalendarRender {
    override val parentRender: ICalendarRender
        get() = parent as ICalendarRender
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    override val calendar: Calendar = startOfDay()
    override val startTime: Long
        get() = calendar.timeInMillis
    override val endTime: Long
        get() = calendar.timeInMillis + dayMills
    override var selectedTime: Long by Delegates.observable(-1) { _, _, time ->
        invalidate()
    }
    override var schedulerModels: List<ISchedulerModel> = listOf()
        set(value) {
            field = value
            invalidate()
        }


    private val selected: Boolean
        get() = selectedTime.dDays == startTime.dDays


    private val pathEffect = DashPathEffect(floatArrayOf(3f.dp, 1.5f.dp), 1f)

    private val arrowPath = Path()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = screenWidth
        val desiredHeight = screenHeight

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val width: Int = when (widthMode) {
            MeasureSpec.EXACTLY -> {
                widthSize
            }
            MeasureSpec.AT_MOST -> {
                desiredWidth.coerceAtMost(widthSize)
            }
            else -> {
                desiredWidth
            }
        }
        val height: Int = when (heightMode) {
            MeasureSpec.EXACTLY -> {
                heightSize
            }
            MeasureSpec.AT_MOST -> {
                desiredHeight.coerceAtMost(heightSize)
            }
            else -> {
                desiredHeight
            }
        }
        setMeasuredDimension(width, height)
        Log.i(TAG, "day view size: $measuredWidth $measuredHeight")
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawDate(canvas)
        drawTasks(canvas)
        drawArrow(canvas)
    }

    private fun drawArrow(canvas: Canvas) {
        if (selected) {
            paint.color = Color.parseColor("#AAEEEEEE")
            canvas.drawRect(9f.dp, 27f.dp, canvas.width.toFloat(), canvas.height.toFloat(), paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f.dp
            paint.strokeCap = Paint.Cap.ROUND
            paint.color = Color.DKGRAY
            if (arrowPath.isEmpty) {
                val centerX = (canvas.width + 9f.dp) / 2f
                val centerY = canvas.height / 2f
                val arrowHeight = 9f.dp
                val arrowWidth = 20f.dp
                arrowPath.moveTo(centerX - arrowWidth / 2, centerY + arrowHeight)
                arrowPath.lineTo(centerX, centerY)
                arrowPath.lineTo(centerX + arrowWidth / 2, centerY + arrowHeight)
            }
            canvas.drawPath(arrowPath, paint)
        }
    }

    private fun drawTasks(canvas: Canvas) {
        paint.textAlign = Paint.Align.LEFT
        var top = 30f.dp
        val height = canvas.height - 25f.dp
        val maxSize = (height / 25f.dp).toInt()
        var size = 0
        schedulerModels.filterIsInstance<DailyTaskModel>().apply {
            size = this.size
        }.forEachIndexed { index, it ->
            if (index >= maxSize) {
                paint.color = Color.LTGRAY
                canvas.drawText("+${size - maxSize}", canvas.width - 20f.dp, 20f.dp, paint)
                return
            }
            val textColor = if (it.expired) {
                Color.parseColor("#DDDDFF")
            } else {
                Color.parseColor("#5555FF")
            }
            paint.style = Paint.Style.STROKE
            paint.pathEffect = pathEffect
            paint.strokeWidth = 1f.dp
            paint.color = textColor
            canvas.drawRoundRect(
                10f.dp,
                top,
                canvas.width - 2f.dp,
                top + 20f.dp,
                4f.dp,
                4f.dp,
                paint
            )
            paint.pathEffect = null
            paint.style = Paint.Style.FILL
            paint.textSize = 10f.dp
            canvas.drawText(it.title, 12f.dp, top + 14f.dp, paint)
            top += 25f.dp
        }
    }

    private fun drawDate(canvas: Canvas) {
        paint.style = Paint.Style.FILL
        paint.strokeCap = Paint.Cap.SQUARE
        val textX = 16f.dp
        val textY = 16f.dp
        paint.color = if (startTime.dDays == System.currentTimeMillis().dDays) {
            Color.BLUE
        } else if (selected) {
            Color.LTGRAY
        } else {
            Color.TRANSPARENT
        }
        if (paint.color != Color.TRANSPARENT) {
            canvas.drawCircle(textX, textY, 10f.dp, paint)
        }
        paint.strokeWidth = 1f.dp
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 12f.dp
        paint.color = if (startTime.dDays == System.currentTimeMillis().dDays) {
            Color.WHITE
        } else if (startTime < parentRender.calendar.firstDayOfMonthTime || startTime > parentRender.calendar.lastDayOfMonthTime) {
            Color.GRAY
        } else {
            Color.BLACK
        }
        paint.isFakeBoldText = true
        canvas.drawText(startTime.dayOfMonth.toString(), textX, textY + 4f.dp, paint)
    }
}
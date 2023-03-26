package me.wxc.widget.calender

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.core.content.res.ResourcesCompat
import me.wxc.widget.R
import me.wxc.widget.ScheduleConfig
import me.wxc.widget.base.DailyTaskModel
import me.wxc.widget.base.ICalendarRender
import me.wxc.widget.base.IScheduleModel
import me.wxc.widget.tools.*
import java.util.*

class DayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), ICalendarRender {
    override val parentRender: ICalendarRender
        get() = parent as ICalendarRender
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(
            ResourcesCompat.getFont(context, R.font.product_sans_regular2),
            Typeface.NORMAL
        )
    }
    override val calendar: Calendar = beginOfDay()
    override val beginTime: Long
        get() = calendar.timeInMillis
    override val endTime: Long
        get() = calendar.timeInMillis + dayMillis
    override var focusedDayTime: Long by setter(-1) { _, _ ->
        invalidate()
    }
    override var scheduleModels: List<IScheduleModel> by setter(listOf()) { _, _ ->
        invalidate()
    }
    override var selectedDayTime: Long by setter(-1) { _, _ ->
        // do nothing
    }

    private val focused: Boolean
        get() = focusedDayTime.dDays == beginTime.dDays


    private val pathEffect = DashPathEffect(floatArrayOf(1.5f.dp, 2.5f.dp), 0f)

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
        if (focused) {
            paint.color = ScheduleConfig.colorTransparent3
            canvas.drawRect(9f.dp, 27f.dp, canvas.width.toFloat(), canvas.height.toFloat(), paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f.dp
            paint.strokeCap = Paint.Cap.ROUND
            paint.color = ScheduleConfig.colorBlack2
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
        var top = 28f.dp
        val height = canvas.height - 22f.dp
        val maxSize = (height / 22f.dp).toInt()
        var size = 0
        scheduleModels.filterIsInstance<DailyTaskModel>().apply {
            size = this.size
        }.forEachIndexed { index, it ->
            if (index >= maxSize) {
                paint.color = ScheduleConfig.colorBlack3
                canvas.drawText("+${size - maxSize}", canvas.width - 20f.dp, 20f.dp, paint)
                return
            }
            val textColor = if (it.expired) {
                ScheduleConfig.colorBlue2
            } else {
                ScheduleConfig.colorBlue1
            }
            paint.style = Paint.Style.STROKE
            paint.pathEffect = pathEffect
            paint.strokeCap = Paint.Cap.SQUARE
            paint.strokeWidth = 1f.dp
            paint.color = textColor
            canvas.drawRoundRect(
                10f.dp,
                top + 2f.dp,
                canvas.width - 2f.dp,
                top + 18f.dp,
                3f.dp,
                3f.dp,
                paint
            )
            paint.pathEffect = null
            paint.style = Paint.Style.FILL
            paint.textSize = 11f.dp
            canvas.drawText(it.title, 12f.dp, top + 14.5f.dp, paint, canvas.width - 14f.dp, false)
            top += 22f.dp
        }
    }

    private fun drawDate(canvas: Canvas) {
        paint.style = Paint.Style.FILL
        paint.strokeCap = Paint.Cap.SQUARE
        val textX = 16f.dp
        val textY = 16f.dp
        val drawCircle: Boolean
        paint.color = if (beginTime.dDays == nowMillis.dDays) {
            if (focusedDayTime == -1L || focusedDayTime.dDays == beginTime.dDays) {
                drawCircle = true
                ScheduleConfig.colorBlue1
            } else {
                drawCircle = false
                ScheduleConfig.colorTransparent1
            }
        } else if (focused) {
            drawCircle = true
            ScheduleConfig.colorBlack4
        } else {
            drawCircle = false
            ScheduleConfig.colorTransparent1
        }
        if (drawCircle) {
            canvas.drawCircle(textX, textY, 10f.dp, paint)
        }
        paint.strokeWidth = 1f.dp
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 13f.dp
        paint.color = if (beginTime.dDays == nowMillis.dDays) {
            if (drawCircle) {
                ScheduleConfig.colorWhite
            } else {
                ScheduleConfig.colorBlue1
            }
        } else if (beginTime < parentRender.calendar.firstDayOfMonthTime || beginTime > parentRender.calendar.lastDayOfMonthTime) {
            ScheduleConfig.colorBlack3
        } else {
            ScheduleConfig.colorBlack1
        }
        paint.isFakeBoldText = true
        canvas.drawText(beginTime.dayOfMonth.toString(), textX, textY + 4f.dp, paint)
    }

    companion object {
        private const val TAG = "DayView"
    }
}
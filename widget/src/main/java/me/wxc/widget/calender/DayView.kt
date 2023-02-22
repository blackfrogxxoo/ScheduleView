package me.wxc.widget.calender

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.core.content.res.ResourcesCompat
import me.wxc.widget.R
import me.wxc.widget.SchedulerConfig
import me.wxc.widget.base.ICalendarRender
import me.wxc.widget.base.ISchedulerModel
import me.wxc.widget.base.ISelectedDayTimeHolder
import me.wxc.widget.scheduler.components.DailyTaskModel
import me.wxc.widget.tools.*
import java.util.*
import kotlin.properties.Delegates

class DayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), ICalendarRender, ISelectedDayTimeHolder {
    override val parentRender: ICalendarRender
        get() = parent as ICalendarRender
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(
            ResourcesCompat.getFont(context, R.font.product_sans_regular2),
            Typeface.NORMAL
        )
    }
    override val calendar: Calendar = startOfDay()
    override val startTime: Long
        get() = calendar.timeInMillis
    override val endTime: Long
        get() = calendar.timeInMillis + dayMills
    override var focusedDayTime: Long by Delegates.observable(-1) { _, _, time ->
        invalidate()
    }
    override var schedulerModels: List<ISchedulerModel> = listOf()
        set(value) {
            field = value
            invalidate()
        }
    override var selectedDayTime: Long = SchedulerConfig.selectedDayTime

    private val focused: Boolean
        get() = focusedDayTime.dDays == startTime.dDays


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
            paint.color = SchedulerConfig.colorTransparent3
            canvas.drawRect(9f.dp, 27f.dp, canvas.width.toFloat(), canvas.height.toFloat(), paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f.dp
            paint.strokeCap = Paint.Cap.ROUND
            paint.color = SchedulerConfig.colorBlack2
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
        schedulerModels.filterIsInstance<DailyTaskModel>().apply {
            size = this.size
        }.forEachIndexed { index, it ->
            if (index >= maxSize) {
                paint.color = SchedulerConfig.colorBlack3
                canvas.drawText("+${size - maxSize}", canvas.width - 20f.dp, 20f.dp, paint)
                return
            }
            val textColor = if (it.expired) {
                SchedulerConfig.colorBlue2
            } else {
                SchedulerConfig.colorBlue1
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
            canvas.drawText(it.title, 12f.dp, top + 14.5f.dp, paint, canvas.width - 14f.dp)
            top += 22f.dp
        }
    }

    private fun drawDate(canvas: Canvas) {
        paint.style = Paint.Style.FILL
        paint.strokeCap = Paint.Cap.SQUARE
        val textX = 16f.dp
        val textY = 16f.dp
        val drawCircle: Boolean
        paint.color = if (startTime.dDays == System.currentTimeMillis().dDays) {
            if (focusedDayTime == -1L || focusedDayTime.dDays == startTime.dDays) {
                drawCircle = true
                SchedulerConfig.colorBlue1
            } else {
                drawCircle = false
                SchedulerConfig.colorTransparent1
            }
        } else if (focused) {
            drawCircle = true
            SchedulerConfig.colorBlack4
        } else {
            drawCircle = false
            SchedulerConfig.colorTransparent1
        }
        if (drawCircle) {
            canvas.drawCircle(textX, textY, 10f.dp, paint)
        }
        paint.strokeWidth = 1f.dp
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 13f.dp
        paint.color = if (startTime.dDays == System.currentTimeMillis().dDays) {
            if (drawCircle) {
                SchedulerConfig.colorWhite
            } else {
                SchedulerConfig.colorBlue1
            }
        } else if (startTime < parentRender.calendar.firstDayOfMonthTime || startTime > parentRender.calendar.lastDayOfMonthTime) {
            SchedulerConfig.colorBlack3
        } else {
            SchedulerConfig.colorBlack1
        }
        paint.isFakeBoldText = true
        canvas.drawText(startTime.dayOfMonth.toString(), textX, textY + 4f.dp, paint)
    }
}
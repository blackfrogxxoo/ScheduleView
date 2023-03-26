package me.wxc.widget.schedule.header

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.core.content.res.ResourcesCompat
import me.wxc.widget.R
import me.wxc.widget.ScheduleConfig
import me.wxc.widget.base.ICalendarRender
import me.wxc.widget.base.IScheduleModel
import me.wxc.widget.tools.*
import java.util.*

class WeekDayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), ICalendarRender {
    override val parentRender: ICalendarRender
        get() = parent as ICalendarRender
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(
            ResourcesCompat.getFont(context, R.font.product_sans_regular2),
            Typeface.NORMAL
        )
        textAlign = Paint.Align.CENTER
    }
    override val calendar: Calendar = beginOfDay()
    override val beginTime: Long
        get() = calendar.timeInMillis
    override val endTime: Long
        get() = calendar.timeInMillis + dayMillis
    override var focusedDayTime: Long by setter(-1) { _, time ->
        invalidate()
    }
    override var selectedDayTime: Long by setter(-1) { _, time ->
        invalidate()
    }
    override var scheduleModels: List<IScheduleModel> = listOf()
        set(value) {
            field = value
            invalidate()
        }

    private val focused: Boolean
        get() = focusedDayTime.dDays == beginTime.dDays || selectedDayTime.dDays == beginTime.dDays


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
    }

    private fun drawDate(canvas: Canvas) {
        paint.style = Paint.Style.FILL
        paint.strokeCap = Paint.Cap.SQUARE
        val textX = width / 2f
        val textY = height / 2f
        if (focused) {
            paint.color = if (beginTime.dDays == nowMillis.dDays) {
                ScheduleConfig.colorBlue1
            } else {
                ScheduleConfig.colorBlack4
            }
            canvas.drawCircle(textX, textY + 11f.dp, 14f.dp, paint)
        }
        paint.strokeWidth = 1f.dp
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 12f.dp
        paint.color = if (beginTime.dDays == nowMillis.dDays) {
            ScheduleConfig.colorBlue1
        } else if (beginTime.dDays < nowMillis.dDays) {
            ScheduleConfig.colorBlack3
        } else {
            ScheduleConfig.colorBlack1
        }
        paint.isFakeBoldText = false
        canvas.drawText(beginTime.dayOfWeekTextSimple, textX, textY - 6f.dp, paint)
        paint.textSize = 16f.dp
        paint.color = if (beginTime.dDays == nowMillis.dDays) {
            if (focused) {
                ScheduleConfig.colorWhite
            } else {
                ScheduleConfig.colorBlue1
            }
        } else if (beginTime.dDays < nowMillis.dDays) {
            ScheduleConfig.colorBlack3
        } else {
            ScheduleConfig.colorBlack1
        }
        paint.isFakeBoldText = true
        canvas.drawText(beginTime.dayOfMonth.toString(), textX, textY + 16f.dp, paint)
    }

    companion object {
        private const val TAG = "WeekDayView"
    }
}
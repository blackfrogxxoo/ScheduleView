package me.wxc.widget.flow.header

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import androidx.core.content.res.ResourcesCompat
import me.wxc.widget.R
import me.wxc.widget.ScheduleConfig
import me.wxc.widget.base.CalendarMode
import me.wxc.widget.base.ICalendarModeHolder
import me.wxc.widget.base.ICalendarRender
import me.wxc.widget.base.IScheduleModel
import me.wxc.widget.tools.*
import java.util.*

class FlowDayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), ICalendarRender, ICalendarModeHolder {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(
            ResourcesCompat.getFont(context, R.font.product_sans_regular2),
            Typeface.NORMAL
        )
        textAlign = Paint.Align.CENTER
    }
    override val parentRender: ICalendarRender
        get() = parent as ICalendarRender
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
    override var scheduleModels: List<IScheduleModel> by setter(listOf()) { _, list ->
        invalidate()
    }

    override var calendarMode: CalendarMode
        set(value) {}
        get() = (parentRender as ICalendarModeHolder).calendarMode

    private val focused: Boolean
        get() = focusedDayTime.dDays == beginTime.dDays || selectedDayTime.dDays == beginTime.dDays

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawDate(canvas)
        drawSchedulers(canvas)
    }

    private fun drawSchedulers(canvas: Canvas) {
        if (scheduleModels.isNotEmpty()) {
            paint.color = ScheduleConfig.colorBlue1
            canvas.drawCircle(width / 2f, height - 10f.dp, 2f.dp, paint)
        }
    }

    private fun drawDate(canvas: Canvas) {
        paint.style = Paint.Style.FILL
        paint.strokeCap = Paint.Cap.SQUARE
        val textX = width / 2f
        val textY = height / 2f
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
            canvas.drawCircle(textX, textY, 14f.dp, paint)
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
        } else {
            if (calendarMode is CalendarMode.MonthMode) {
                if (beginTime < parentRender.calendar.firstDayOfMonthTime || beginTime > parentRender.calendar.lastDayOfMonthTime) {
                    ScheduleConfig.colorBlack3
                } else {
                    ScheduleConfig.colorBlack1
                }
            } else {
                if (endTime < nowMillis) {
                    ScheduleConfig.colorBlack3
                } else {
                    ScheduleConfig.colorBlack1
                }
            }
        }
        paint.isFakeBoldText = true
        canvas.drawText(beginTime.dayOfMonth.toString(), textX, textY + 4f.dp, paint)
    }
}
package me.wxc.widget.flow.header

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.Log
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.children
import me.wxc.widget.R
import me.wxc.widget.flow.flowHeaderDayHeight
import me.wxc.widget.base.*
import me.wxc.widget.tools.*
import java.util.*
import kotlin.math.roundToInt

class FlowHeaderView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr), ICalendarRender, ICalendarModeHolder, ICalendarParent {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 11f.dp
        typeface = Typeface.create(
            ResourcesCompat.getFont(context, R.font.product_sans_regular2),
            Typeface.NORMAL
        )
        textAlign = Paint.Align.CENTER
    }
    override val parentRender: ICalendarRender?
        get() = parent as? ICalendarRender
    override val calendar: Calendar = beginOfDay()
    override var focusedDayTime: Long by setter(-1) { _, time ->
        childRenders.forEach { it.focusedDayTime = time }
    }
    override var selectedDayTime: Long by setter(-1) { _, time ->
        if (focusedDayTime in beginTime..endTime && focusedDayTime.dDays != time.dDays) {
            parentRender?.focusedDayTime = -1
        }
        childRenders.forEach { it.selectedDayTime = time }
    }
    override var scheduleModels: List<IScheduleModel> by setter(listOf()) { _, list ->
        childRenders.forEach { it.getSchedulesFrom(list) }
    }
    override val beginTime: Long
        get() = if (isMonthMode) {
            beginOfDay(calendar.firstDayOfMonthTime).apply {
                set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            }.timeInMillis
        } else {
            beginOfDay(calendar.timeInMillis).apply {
                set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            }.timeInMillis
        }
    override val endTime: Long
        get() = if (isMonthMode) {
            beginOfDay(calendar.lastDayOfMonthTime).apply {
                set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
            }.timeInMillis
        } else {
            beginOfDay(calendar.timeInMillis).apply {
                set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
            }.timeInMillis
        }

    private val dayWidth: Float = screenWidth / 7f
    private val dayHeight: Float
        get() = flowHeaderDayHeight
    private val weekCount: Int
        get() = (1 + endTime.dDays - beginTime.dDays).toInt() / 7
    private val isMonthMode: Boolean
        get() = calendarMode is CalendarMode.MonthMode
    private val selectedLineIndex: Int
        get() = if (selectedDayTime != -1L) {
            ((selectedDayTime - beginTime) / (7 * dayMillis)).toInt()
        } else {
            0
        }

    override var calendarMode: CalendarMode by setter(CalendarMode.WeekMode) { _, mode ->
        translationY = if (mode is CalendarMode.WeekMode) {
            0f
        } else {
            val fraction = (mode as CalendarMode.MonthMode).expandFraction
            (-selectedLineIndex * (1 - fraction) * dayHeight).apply {
                Log.i(TAG, "translation y : $fraction, $this")
            }
        }
    }


    override val childRenders: List<ICalendarRender>
        get() = children.filterIsInstance<ICalendarRender>().toList()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(screenWidth, calculateHeight())
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawText(beginTime.yyyyMMddHHmmss, width / 2f, height / 2f, paint)
    }

    override fun onLayout(p0: Boolean, p1: Int, p2: Int, p3: Int, p4: Int) {
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            val calendar = (child as ICalendarRender).calendar
            val dDays = calendar.timeInMillis.dDays - beginTime.dDays
            val line = dDays / 7
            val left = dDays % 7 * dayWidth
            val top = paddingTop + line * dayHeight
            val right = left + dayWidth
            val bottom = top + dayHeight
            if (top.isNaN()) continue
            child.layout(
                left.roundToInt(),
                top.roundToInt(),
                right.roundToInt(),
                bottom.roundToInt()
            )
        }
    }

    private fun calculateHeight(): Int {
        return (weekCount * dayHeight).roundToInt()
    }


    override fun onAttachedToWindow() { // TODO View复用
        super.onAttachedToWindow()
        for (time in beginTime..endTime step dayMillis) {
            FlowDayView(context).let { child ->
                child.calendar.timeInMillis = time
                addView(child)
                child.focusedDayTime = focusedDayTime
                child.selectedDayTime = selectedDayTime
                child.setOnClickListener {
                    rootCalendarRender?.focusedDayTime = child.beginTime
                }
                if (scheduleModels.any()) {
                    child.getSchedulesFrom(scheduleModels)
                }
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeAllViews()
    }

    companion object {
        private const val TAG = "FlowHeaderView"
    }
}
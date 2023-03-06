package me.wxc.widget.flow

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import me.wxc.widget.R
import me.wxc.widget.base.*
import me.wxc.widget.tools.*
import java.util.*
import kotlin.math.roundToInt
import kotlin.properties.Delegates

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
    override val calendar: Calendar = startOfDay()
    override var focusedDayTime: Long by Delegates.observable(-1) { _, _, time ->
        _children.forEach {
            it.focusedDayTime = time
        }
    }
    override var scheduleModels: List<IScheduleModel> by Delegates.observable(listOf()) { _, _, list ->
        _children.forEach {
            it.getSchedulesFrom(list)
        }
    }
    override val startTime: Long
        get() = if (isMonthMode) {
            startOfDay(calendar.firstDayOfMonthTime).apply {
                set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            }.timeInMillis
        } else {
            startOfDay(calendar.timeInMillis).apply {
                set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            }.timeInMillis
        }
    override val endTime: Long
        get() = if (isMonthMode) {
            startOfDay(calendar.lastDayOfMonthTime).apply {
                set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
            }.timeInMillis
        } else {
            startOfDay(calendar.timeInMillis).apply {
                set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
            }.timeInMillis
        }

    private val dayWidth: Float = screenWidth / 7f
    private val dayHeight: Float
        get() = flowHeaderDayHeight
    private val weekCount: Int
        get() = (1 + endTime.dDays - startTime.dDays).toInt() / 7
    private val isMonthMode: Boolean
        get() = calendarMode is CalendarMode.MonthMode
    private val focusedLineIndex: Int
        get() = if (focusedDayTime != -1L) {
            ((focusedDayTime - startTime) / (7 * dayMillis)).toInt()
        } else {
            0
        }

    override var calendarMode: CalendarMode by Delegates.observable(CalendarMode.WeekMode) { _, _, mode ->
        translationY = if (mode is CalendarMode.WeekMode) {
            0f
        } else {
            val fraction = (mode as CalendarMode.MonthMode).expandFraction
            (-focusedLineIndex * (1 - fraction) * dayHeight).apply {
                Log.i(TAG, "translation y : $fraction, $this")
            }
        }
    }


    override val childRenders: List<ICalendarRender>
        get() = _children.toList()

    private val _children = mutableListOf<ICalendarRender>()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(screenWidth, calculateHeight())
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawText(sdf_yyyyMMddHHmmss.format(startTime), width / 2f, height / 2f, paint)
    }

    override fun onLayout(p0: Boolean, p1: Int, p2: Int, p3: Int, p4: Int) {
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            val calendar = (child as ICalendarRender).calendar
            val dDays = calendar.timeInMillis.dDays - startTime.dDays
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


    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Log.i(
            TAG,
            "onAttachedToWindow ${sdf_yyyyMMddHHmmss.format(startTime)} ${
                sdf_yyyyMMddHHmmss.format(
                    endTime
                )
            }"
        )
        if (_children.isEmpty()) {
            for (time in startTime..endTime step dayMillis) {
                FlowDayView(context).let { child ->
                    child.calendar.timeInMillis = time
                    _children.add(child)
                    addView(child)
                    child.focusedDayTime = focusedDayTime
                    child.setOnClickListener {
                        parentRender?.focusedDayTime = if (focusedDayTime != child.startTime) {
                            child.startTime
                        } else {
                            -1
                        }
                    }
                    if (scheduleModels.any()) {
                        child.getSchedulesFrom(scheduleModels)
                    }
                }
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        Log.i(
            TAG,
            "onDetachedFromWindow ${sdf_yyyyMMddHHmmss.format(startTime)} ${
                sdf_yyyyMMddHHmmss.format(
                    endTime
                )
            }"
        )
        if (_children.isNotEmpty()) {
            _children.forEach {
                this@FlowHeaderView.removeView(it as View)
            }
            _children.clear()
        }
        focusedDayTime = -1L
    }

}
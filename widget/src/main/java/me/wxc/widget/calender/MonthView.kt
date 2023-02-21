package me.wxc.widget.calender

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.updatePadding
import me.wxc.widget.R
import me.wxc.widget.SchedulerConfig
import me.wxc.widget.base.ISelectedTimeObserver
import me.wxc.widget.base.ICalendarParent
import me.wxc.widget.base.ICalendarRender
import me.wxc.widget.base.ISchedulerModel
import me.wxc.widget.tools.*
import java.util.*
import kotlin.math.roundToInt
import kotlin.properties.Delegates

class MonthView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr), ICalendarRender, ICalendarParent,
    ISelectedTimeObserver {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 11f.dp
        typeface = Typeface.create(ResourcesCompat.getFont(context, R.font.product_sans_regular2), Typeface.NORMAL)
    }
    private lateinit var dailyTaskListViewGroup: DailyTaskListViewGroup

    override val parentRender: ICalendarRender? = null
    override val calendar: Calendar = startOfDay()
    override val startTime: Long
        get() = startOfDay(calendar.firstDayOfMonthTime).apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        }.timeInMillis
    override val endTime: Long
        get() = startOfDay(calendar.lastDayOfMonthTime).apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
        }.timeInMillis
    override var selectedTime: Long by Delegates.observable(-1L) { _, _, time ->
        collapseLine = if (time > 0) {
            ((time - startTime) / (7 * dayMills)).toInt()
        } else {
            -1
        }
        children.forEach {
            it.selectedTime = time
        }
        dailyTaskListViewGroup.selectedTime = time
    }
    override var schedulerModels: List<ISchedulerModel> = listOf()
        set(value) {
            field = value
            _children.forEach { child ->
                child.schedulerModels = child.schedulersFrom(value)
            }
            dailyTaskListViewGroup.schedulerModels = dailyTaskListViewGroup.schedulersFrom(value)
        }

    override val children: List<ICalendarRender>
        get() = _children.toList()

    private val _children = mutableListOf<ICalendarRender>().apply {
    }

    private val dayWidth: Float = screenWidth / 7f
    private val dayHeight: Float
        get() = 1f * (measuredHeight - paddingTop) / (_children.size / 7)
    private val topPadding = 26f.dp
    private var collapseLine = -1
        set(value) {
            onCollapseLineChanged(field, value)
            field = value
        }
    private var animatingCollapseLine = -1
    private var collapseCenter: Float = -1f
    private var collapseTop: Float = -1f
    private var collapseBottom: Float = -1f

    init {
        setWillNotDraw(false)
        updatePadding(top = topPadding.roundToInt())
        dailyTaskListViewGroup = DailyTaskListViewGroup(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            setBackgroundColor(SchedulerConfig.colorBlack5)
        }
        addView(dailyTaskListViewGroup)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        paint.color = SchedulerConfig.colorBlack3
        for (i in 0 until 7) {
            val time = startTime + i * dayMills
            val left = 10f.dp + i * dayWidth
            canvas.drawText(time.dayOfWeekTextSimple, left, 15f.dp, paint)
        }
        paint.color = SchedulerConfig.colorBlack4
        paint.strokeWidth = .5f.dp
        canvas.drawLine(0f, paddingTop.toFloat(), measuredWidth.toFloat(), paddingTop.toFloat(), paint)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            if (child is DailyTaskListViewGroup) {
                if (!collapseTop.isNaN() && !collapseBottom.isNaN()) {
                    child.layout(l, collapseTop.roundToInt(), r, collapseBottom.roundToInt())
                }
                continue
            }
            val calendar = (child as ICalendarRender).calendar
            val dDays = calendar.timeInMillis.dDays - startTime.dDays
            val line = dDays / 7
            val left = dDays % 7 * dayWidth
            var top = paddingTop + line * dayHeight
            if (animatingCollapseLine >= 0) {
                if (line <= animatingCollapseLine) {
                    top -= collapseCenter - collapseTop
                } else {
                    top += collapseBottom - collapseCenter
                }
            }
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

    private fun onCollapseLineChanged(old: Int, new: Int, doOnEnd: () -> Unit = {}) {
        Log.i(TAG, "onCollapseLineChanged: $old, $new")
        if (old == -1 && new >= 0) {
            dailyTaskListViewGroup.calendar.timeInMillis = startTime + new * 7 * dayMills
            dailyTaskListViewGroup.schedulerModels = schedulersFrom(schedulerModels)
            collapseCenter = paddingTop + (new + 1) * dayHeight
            val destTop = paddingTop + dayHeight
            val destBottom = if (new < _children.size / 7 - 1) {
                paddingTop + (_children.size / 7 - 1) * dayHeight
            } else {
                paddingTop + _children.size / 7 * dayHeight
            }
            ValueAnimator.ofFloat(0f, 1f).apply {
                doOnStart {
                    animatingCollapseLine = new
                }
                doOnEnd {
                    animatingCollapseLine = new
                }
                duration = 300
                addUpdateListener {
                    collapseTop = collapseCenter + (destTop - collapseCenter) * it.animatedFraction
                    collapseBottom =
                        collapseCenter + (destBottom - collapseCenter) * it.animatedFraction
                    requestLayout()
                }
            }.start()
        } else if (old >= 0 && new == -1) {
            dailyTaskListViewGroup.calendar.timeInMillis = -1
            dailyTaskListViewGroup.schedulerModels = emptyList()
            collapseCenter = paddingTop + (old + 1) * dayHeight
            val startTop = collapseTop
            val startBottom = collapseBottom
            ValueAnimator.ofFloat(0f, 1f).apply {
                doOnStart {
                    animatingCollapseLine = old
                }
                doOnEnd {
                    animatingCollapseLine = new
                    doOnEnd.invoke()
                }
                duration = 300
                addUpdateListener {
                    collapseTop = startTop + (collapseCenter - startTop) * it.animatedFraction
                    collapseBottom =
                        startBottom + (collapseCenter - startBottom) * it.animatedFraction
                    requestLayout()
                }
            }.start()
        } else if (old != new && old >= 0 && new >= 0) {
            onCollapseLineChanged(old, -1) {
                onCollapseLineChanged(-1, new)
            }
        }
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
            for (time in startTime..endTime step dayMills) {
                DayView(context).let { child ->
                    child.calendar.timeInMillis = time
                    _children.add(child)
                    addView(child)
                    child.setOnClickListener {
                        if (selectedTime != child.startTime) {
                            selectedTime = child.startTime
                        } else {
                            selectedTime = -1
                        }
                    }
                    if (schedulerModels.any()) {
                        child.schedulerModels = child.schedulersFrom(schedulerModels)
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
                this@MonthView.removeView(it as View)
            }
            _children.clear()
        }
        selectedTime = -1L
    }

    override fun onSelectedTime(time: Long) {
        collapseLine = -1
    }
}
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
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.RecyclerView
import me.wxc.widget.R
import me.wxc.widget.ScheduleConfig
import me.wxc.widget.base.ICalendarParent
import me.wxc.widget.base.ICalendarRender
import me.wxc.widget.base.IScheduleModel
import me.wxc.widget.tools.*
import java.util.*
import kotlin.math.roundToInt

class MonthView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr), ICalendarRender, ICalendarParent {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 11f.dp
        typeface = Typeface.create(
            ResourcesCompat.getFont(context, R.font.product_sans_regular2),
            Typeface.NORMAL
        )
    }
    private val dailyTaskListViewGroup: DailyTaskListViewGroup

    override val parentRender: ICalendarRender? = null
    override val calendar: Calendar = beginOfDay()
    override val beginTime: Long
        get() = beginOfDay(calendar.firstDayOfMonthTime).apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        }.timeInMillis
    override val endTime: Long
        get() = beginOfDay(calendar.lastDayOfMonthTime).apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
        }.timeInMillis
    override var focusedDayTime: Long by setter(-1L) { _, time ->
        collapseLine = if (time > 0) {
            ((time - beginTime) / (7 * dayMillis)).toInt()
        } else {
            -1
        }
        childRenders.forEach {
            it.focusedDayTime = time
        }
        if ((parent as? RecyclerView)?.isVisible == true && (parent as? RecyclerView)?.scrollState == RecyclerView.SCROLL_STATE_IDLE) {
            if (time != -1L) {
                ScheduleConfig.onDateSelectedListener.invoke(beginOfDay(time))
            } else if (nowMillis in (beginTime + 1) until endTime) {
                ScheduleConfig.onDateSelectedListener.invoke(beginOfDay())
            } else {
                ScheduleConfig.onDateSelectedListener.invoke(calendar)
            }
            Log.i(TAG, ": ${ScheduleConfig.selectedDayTime.yyyyMd}")
        }
    }
    override var selectedDayTime: Long by setter(nowMillis) { _, time ->
        collapseLine = -1
    }
    override var scheduleModels: List<IScheduleModel> by setter(listOf()) { _, list ->
        childRenders.forEach { it.getSchedulesFrom(list) }
    }
    override val childRenders: List<ICalendarRender>
        get() = children.filterIsInstance<ICalendarRender>().toList()

    private val dayWidth: Float = screenWidth / 7f
    private val dayHeight: Float
        get() = 1f * (measuredHeight - paddingTop) / (childCount / 7)
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
            setBackgroundColor(ScheduleConfig.colorBlack6)
        }
        addView(dailyTaskListViewGroup)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        paint.color = ScheduleConfig.colorBlack3
        val todayWeekDayIndex =
            if (calendar.timeInMillis.dMonths == nowMillis.dMonths) {
                beginOfDay().get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
            } else {
                -1
            }
        for (i in 0 until 7) {
            val time = beginTime + i * dayMillis
            val left = 10f.dp + i * dayWidth
            if (todayWeekDayIndex == i) {
                paint.color = ScheduleConfig.colorBlue1
            }
            canvas.drawText(time.dayOfWeekTextSimple, left, 15f.dp, paint)
            if (todayWeekDayIndex == i) {
                paint.color = ScheduleConfig.colorBlack3
            }
        }
        paint.color = ScheduleConfig.colorBlack4
        paint.strokeWidth = .5f.dp
        canvas.drawLine(
            0f,
            paddingTop.toFloat(),
            measuredWidth.toFloat(),
            paddingTop.toFloat(),
            paint
        )
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
            val dDays = calendar.timeInMillis.dDays - beginTime.dDays
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
            dailyTaskListViewGroup.calendar.timeInMillis = beginTime + new * 7 * dayMillis
            dailyTaskListViewGroup.getSchedulesFrom(scheduleModels)
            collapseCenter = paddingTop + (new + 1) * dayHeight
            val destTop = paddingTop + dayHeight
            val destBottom = if (new < childCount / 7 - 1) {
                paddingTop + (childCount / 7 - 1) * dayHeight
            } else {
                paddingTop + childCount / 7 * dayHeight
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
            dailyTaskListViewGroup.scheduleModels = emptyList()
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
        Log.i(TAG, "onAttachedToWindow ${beginTime.yyyyMMddHHmmss} ${endTime.yyyyMMddHHmmss}")
        for (time in beginTime..endTime step dayMillis) {
            DayView(context).let { child ->
                child.calendar.timeInMillis = time
                addView(child)
                child.setOnClickListener {
                    focusedDayTime = if (focusedDayTime != child.beginTime) {
                        child.beginTime
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

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        Log.i(TAG, "onDetachedFromWindow ${beginTime.yyyyMMddHHmmss} ${endTime.yyyyMMddHHmmss}")
        removeViews(1, childCount - 1)
        focusedDayTime = -1L
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (changedView == parent && visibility == GONE) {
            focusedDayTime = -1L
        }
    }

    companion object {
        private const val TAG = "MonthView"
    }
}
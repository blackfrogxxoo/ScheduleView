package me.wxc.widget.flow.header

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.Log
import android.view.ViewGroup
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.children
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import me.wxc.widget.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.wxc.widget.ScheduleConfig
import me.wxc.widget.flow.flowHeaderDayHeight
import me.wxc.widget.base.*
import me.wxc.widget.tools.*
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt

class FlowHeaderGroup @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : RecyclerView(context, attrs), ICalendarRender, ICalendarParent, ICalendarModeHolder {
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
    override val calendar: Calendar = beginOfDay(ScheduleConfig.scheduleBeginTime)
    override var focusedDayTime: Long by setter(-1) { _, time ->
        childRenders.forEach { it.focusedDayTime = time }
    }
    override var selectedDayTime: Long by setter(-1) { _, time ->
        childRenders.forEach { it.selectedDayTime = time }
        if (!byDrag) {
            val position = time.parseIndex()
            scrollToPosition(position)
        }
    }
    override var scheduleModels: List<IScheduleModel> by setter(emptyList()) { _, list ->
        adapter?.notifyDataSetChanged()
        scrollToPosition(selectedDayTime.parseIndex())
    }
    override val beginTime: Long
        get() = ScheduleConfig.scheduleBeginTime
    override val endTime: Long
        get() = ScheduleConfig.scheduleEndTime

    override val childRenders: List<ICalendarRender>
        get() = children.filterIsInstance<ICalendarRender>().toList()
    override var calendarMode: CalendarMode by setter(CalendarMode.WeekMode) { oldMode, mode ->
        if (oldMode is CalendarMode.MonthMode && mode is CalendarMode.MonthMode) {
            onExpandFraction(mode.expandFraction)
        }
        onCalendarModeSet(mode)
    }

    internal val isMonthMode: Boolean
        get() = calendarMode is CalendarMode.MonthMode


    private val monthCount: Int by lazy {
        val start = beginOfDay(ScheduleConfig.scheduleBeginTime)
        val end = beginOfDay(ScheduleConfig.scheduleEndTime)
        val result =
            (end.get(Calendar.YEAR) - start.get(Calendar.YEAR)) * 12 + (end.get(Calendar.MONTH) - start.get(
                Calendar.MONTH
            ))
        result.apply { Log.i(TAG, "month count = $result") }
    }

    private val weekCount: Int by lazy {
        val startWeekDay = beginOfDay(beginTime).apply {
            timeInMillis -= (get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY) * dayMillis
        }.timeInMillis
        val result = ((endTime - startWeekDay) / (7 * dayMillis)).toInt()
        result.apply { Log.i(TAG, "week count = $result") }
    }

    private val topPadding = 26f.dp

    private val dayWidth: Float = screenWidth / 7f
    private var lastPosition = -1
    private var autoSwitchingMode = false

    private var byDrag = false

    init {
        setWillNotDraw(false)
        updatePadding(top = topPadding.roundToInt())
        layoutManager = LinearLayoutManager(context, HORIZONTAL, false)
        PagerSnapHelper().attachToRecyclerView(this)
        adapter = Adapter()
        addOnScrollListener(object : OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                recyclerView.children.filterIsInstance<FlowHeaderView>().forEach {
                    if (it.calendarMode is CalendarMode.MonthMode && (it.calendarMode as CalendarMode.MonthMode).expandFraction != 1f) {
                        it.calendarMode = CalendarMode.MonthMode(1f)
                    }
                }
                if (newState == SCROLL_STATE_DRAGGING) {
                    byDrag = true
                } else if (newState == SCROLL_STATE_IDLE) {
                    val llm = recyclerView.layoutManager as LinearLayoutManager
                    val position = llm.findFirstVisibleItemPosition()
                    if (position != -1 && lastPosition != position) {
                        lastPosition = position
                        onPosition(position)
                    }
                    byDrag = false
                }
            }
        })
        scrollToPosition(nowMillis.parseIndex())
    }

    private fun onPosition(position: Int) {
        val llm = layoutManager as LinearLayoutManager
        adjustHeight(position)
        (llm.findViewByPosition(position) as? ICalendarRender)?.let {
            val beginTime =
                if (isMonthMode) it.calendar.firstDayOfMonthTime else it.beginTime
            val endTime =
                if (isMonthMode) it.calendar.lastDayOfMonthTime else it.endTime
            if (focusedDayTime < beginTime || focusedDayTime > endTime) {
                if (byDrag) {
                    parentRender?.focusedDayTime =
                        if (nowMillis in it.beginTime..it.endTime) {
                            beginOfDay().timeInMillis
                        } else if (isMonthMode) {
                            beginTime
                        } else {
                            beginTime.calendar.apply {
                                set(Calendar.DAY_OF_WEEK, focusedDayTime.dayOfWeek)
                            }.timeInMillis
                        }
                    ScheduleConfig.onDateSelectedListener.invoke(selectedDayTime.calendar)
                } else {
                    focusedDayTime = -1
                }
            }
            if (selectedDayTime in beginTime..endTime) {
                it.selectedDayTime = selectedDayTime
            }
            ScheduleConfig.lifecycleScope.launch {
                it.scheduleModels = withContext(Dispatchers.IO) {
                    ScheduleConfig.scheduleModelsProvider.invoke(
                        it.beginTime,
                        it.endTime + dayMillis
                    )
                }.apply {
                    Log.i(TAG, "$this")
                }
            }
        }
    }

    private fun Long.parseIndex(): Int {
        if (this == -1L) return nowMillis.parseIndex()
        return if (isMonthMode) {
            parseMonthIndex()
        } else {
            parseWeekIndex()
        }
    }

    private val llm: LinearLayoutManager
        get() = layoutManager as LinearLayoutManager

    private var adjustHeightAnimator: ValueAnimator? = null
    private fun adjustHeight(position: Int) {
        val fraction = (calendarMode as? CalendarMode.MonthMode)?.expandFraction ?: 0f
        if (fraction > 0f && fraction < 1f) return // 切换模式过程中不打断
        adjustHeightAnimator?.cancel()
        val parentHeight = measuredHeight - paddingTop
        val childHeight = llm.findViewByPosition(position)?.measuredHeight ?: parentHeight
        Log.i(TAG, "adjustHeight: $childHeight, $parentHeight")
        if (childHeight != parentHeight) {
            ValueAnimator.ofInt(parentHeight, childHeight).apply {
                adjustHeightAnimator = this
                addUpdateListener {
                    updateLayoutParams {
                        height = paddingTop + it.animatedValue as Int
                    }
                }
            }.start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        paint.color = ScheduleConfig.colorBlack3
        val todayWeekDayIndex = nowMillis.dayOfWeek - Calendar.SUNDAY
        for (weekDay in Calendar.SUNDAY..Calendar.SATURDAY) {
            val index = weekDay - Calendar.SUNDAY
            val time = beginOfDay().apply { set(Calendar.DAY_OF_WEEK, weekDay) }.timeInMillis
            val left = index * dayWidth + 0.5f * dayWidth
            if (todayWeekDayIndex == index && selectedDayTime.dDays == nowMillis.dDays) {
                paint.color = ScheduleConfig.colorBlue1
            } else {
                paint.color = ScheduleConfig.colorBlack1
            }
            canvas.drawText(time.dayOfWeekTextSimple, left, 18f.dp, paint)
        }
    }

    internal fun autoSwitchMode(mode: CalendarMode) {
        if (autoSwitchingMode) return
        Log.i(TAG, "autoSwitchMode: $calendarMode -> $mode")
        if (mode is CalendarMode.MonthMode && calendarMode is CalendarMode.WeekMode) {
            calendarMode = CalendarMode.MonthMode(0f)
        }
        val srcFraction = (calendarMode as CalendarMode.MonthMode).expandFraction
        val targetFraction = if (mode is CalendarMode.MonthMode) 1f else 0f
        ValueAnimator.ofFloat(
            srcFraction,
            targetFraction
        ).apply {
            duration = (200 * abs(srcFraction - targetFraction)).toLong().coerceAtLeast(100)
            addUpdateListener {
                calendarMode =
                    (calendarMode as CalendarMode.MonthMode).copy(expandFraction = it.animatedValue as Float)
            }
            doOnStart {
                autoSwitchingMode = true
                if (mode is CalendarMode.MonthMode) {
                    onCalendarModeSet(mode)
                }
            }
            doOnEnd {
                autoSwitchingMode = false
                if (mode is CalendarMode.WeekMode) {
                    calendarMode = mode
                }
            }
        }.start()
    }

    private fun onCalendarModeSet(mode: CalendarMode) {
        childRenders.filterIsInstance<ICalendarModeHolder>().forEach {
            it.calendarMode = mode
        }
        if (mode is CalendarMode.WeekMode || (mode as? CalendarMode.MonthMode)?.expandFraction == 0f) {
            adapter?.notifyDataSetChanged()
            scrollToPosition(selectedDayTime.parseIndex())
        }
    }

    private fun onExpandFraction(fraction: Float) {
        val dayHeight: Float = flowHeaderDayHeight
        val calendar = ScheduleConfig.selectedDayTime.calendar
        val monthStart = beginOfDay(calendar.firstDayOfMonthTime).apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        }.timeInMillis
        val monthEnd = beginOfDay(calendar.lastDayOfMonthTime).apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
        }.timeInMillis
        val weekCount: Int = (1 + monthEnd.dDays - monthStart.dDays).toInt() / 7
        val expandHeight = weekCount * dayHeight
        val collapseHeight = dayHeight
        Log.i(
            TAG,
            "onMonthModeExpandFraction: $fraction, $weekCount, ${monthStart.yyyyMMddHHmmss}"
        )
        updateLayoutParams {
            height =
                (topPadding + collapseHeight + (expandHeight - collapseHeight) * fraction).roundToInt()
        }
    }

    override fun scrollToPosition(position: Int) {
        super.scrollToPosition(position)
        post { onPosition(position) }
    }

    inner class Adapter : RecyclerView.Adapter<VH>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            return VH(parent.context)
        }

        override fun getItemCount(): Int = if (isMonthMode) monthCount else weekCount

        override fun onBindViewHolder(holder: VH, position: Int) {
            (holder.itemView as FlowHeaderView).run {
                this.calendar.timeInMillis =
                    beginOfDay(this@FlowHeaderGroup.beginTime).apply {
                        if (isMonthMode) {
                            add(Calendar.MONTH, position)
                        } else {
                            timeInMillis += position * 7 * dayMillis
                        }
                    }.timeInMillis
                this.selectedDayTime = this@FlowHeaderGroup.selectedDayTime
                this.focusedDayTime = this@FlowHeaderGroup.focusedDayTime
                this.calendarMode = this@FlowHeaderGroup.calendarMode
            }
        }

    }

    class VH(context: Context) : ViewHolder(FlowHeaderView(context))

    companion object {
        private const val TAG = "FlowHeaderGroup"
    }
}
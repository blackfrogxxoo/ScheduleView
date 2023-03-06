package me.wxc.widget.flow

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.ViewGroup
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.view.children
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.wxc.widget.ScheduleConfig
import me.wxc.widget.base.*
import me.wxc.widget.tools.*
import java.util.*
import kotlin.math.roundToInt
import kotlin.properties.Delegates

class FlowHeaderGroup @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : RecyclerView(context, attrs), ICalendarRender, ICalendarParent, ICalendarModeHolder {
    override val parentRender: ICalendarRender? = null
    override val calendar: Calendar = startOfDay(ScheduleConfig.scheduleStartTime)
    override var focusedDayTime: Long by Delegates.observable(-1) { _, _, time ->
        childRenders.forEach {
            it.focusedDayTime = time
        }
        smoothScrollToPosition((if (focusedDayTime == -1L) System.currentTimeMillis() else focusedDayTime).parseIndex())
    }
    override var scheduleModels: List<IScheduleModel> by Delegates.observable(emptyList()) { _, _, list ->
        adapter?.notifyDataSetChanged()
    }
    override val startTime: Long
        get() = ScheduleConfig.scheduleStartTime
    override val endTime: Long
        get() = ScheduleConfig.scheduleEndTime

    override val childRenders: List<ICalendarRender>
        get() = children.filterIsInstance<ICalendarRender>().toList()
    override var calendarMode: CalendarMode by Delegates.observable(CalendarMode.WeekMode) { _, oldMode, mode ->
        if (oldMode is CalendarMode.MonthMode && mode is CalendarMode.MonthMode) {
            onExpandFraction(mode.expandFraction)
        }
        onCalendarModeSet(mode)
    }

    private val isMonthMode: Boolean
        get() = calendarMode is CalendarMode.MonthMode


    private val monthCount: Int by lazy {
        val start = startOfDay(ScheduleConfig.scheduleStartTime)
        val end = startOfDay(ScheduleConfig.scheduleEndTime)
        val result =
            (end.get(Calendar.YEAR) - start.get(Calendar.YEAR)) * 12 + (end.get(Calendar.MONTH) - start.get(
                Calendar.MONTH
            ))
        result.apply { Log.i("ScheduleFlowHeaderGroup", "month count = $result") }
    }

    private val weekCount: Int by lazy {
        val startWeekDay = startOfDay(startTime).apply {
            timeInMillis -= (get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY) * dayMillis
        }.timeInMillis
        val result = ((endTime - startWeekDay) / (7 * dayMillis)).toInt()
        result.apply { Log.i("ScheduleFlowHeaderGroup", "week count = $result") }
    }

    private fun Long.parseIndex(): Int {
        return if (isMonthMode) {
            val start = startOfDay(ScheduleConfig.scheduleStartTime)
            val end = startOfDay(this)
            val result =
                (end.get(Calendar.YEAR) - start.get(Calendar.YEAR)) * 12 + (end.get(Calendar.MONTH) - start.get(
                    Calendar.MONTH
                ))
            result.apply { Log.i("ScheduleFlowHeaderGroup", "month index = $result") }
        } else {
            val startWeekDay = startOfDay(startTime).apply {
                timeInMillis -= (get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY) * dayMillis
            }.timeInMillis
            val result = ((this - startWeekDay) / (7 * dayMillis)).toInt()
            result.apply { Log.i("ScheduleFlowHeaderGroup", "week index = $result") }
        }
    }

    private var autoSwitchingMode = false

    init {
        layoutManager = LinearLayoutManager(context, HORIZONTAL, false)
        PagerSnapHelper().attachToRecyclerView(this)
        adapter = Adapter()
        addOnScrollListener(object : OnScrollListener() {
            private var lastPosition = -1
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val llm = recyclerView.layoutManager as LinearLayoutManager
                val position = llm.findFirstCompletelyVisibleItemPosition()
                if (position != -1 && lastPosition != position) {
                    lastPosition = position
                    val childHeight = llm.findViewByPosition(position)?.measuredHeight
                        ?: recyclerView.measuredHeight
                    val recyclerHeight = recyclerView.measuredHeight
                    if (childHeight != recyclerHeight && (calendarMode as? CalendarMode.MonthMode)?.touching == false) {
                        ValueAnimator.ofInt(recyclerHeight, childHeight).apply {
                            addUpdateListener {
                                recyclerView.updateLayoutParams {
                                    height = it.animatedValue as Int
                                }
                            }
                        }.start()
                    }
                    (llm.findViewByPosition(position) as? ICalendarRender)?.let {
                        val startTime =
                            if (isMonthMode) it.calendar.firstDayOfMonthTime else it.startTime
                        val endTime =
                            if (isMonthMode) it.calendar.lastDayOfMonthTime else it.endTime
                        if (focusedDayTime < startTime || focusedDayTime > endTime) {
                            focusedDayTime =
                                if (System.currentTimeMillis() in it.startTime..it.endTime) {
                                    startOfDay().timeInMillis
                                } else if (isMonthMode) {
                                    startTime
                                } else {
                                    startTime.calendar.apply {
                                        set(Calendar.DAY_OF_WEEK, focusedDayTime.dayOfWeek)
                                    }.timeInMillis
                                }
                        }
                        ScheduleConfig.lifecycleScope.launch {
                            it.scheduleModels = withContext(Dispatchers.IO) {
                                ScheduleConfig.scheduleModelsProvider.invoke(
                                    it.startTime,
                                    it.endTime + dayMillis
                                )
                            }.apply {
                                Log.i(TAG, "$this")
                            }
                        }
                    }
                    ScheduleConfig.onDateSelectedListener.invoke(
                        focusedDayTime.calendar
                    )
                }
            }
        })
        scrollToPosition(System.currentTimeMillis().parseIndex())
    }

    internal fun autoSwitchMode(mode: CalendarMode) {
        if (autoSwitchingMode) return
        Log.i(TAG, "autoSwitchMode: $calendarMode -> $mode")
        if (mode is CalendarMode.MonthMode && calendarMode is CalendarMode.WeekMode) {
            calendarMode = CalendarMode.MonthMode(0f)
        }
        val targetFraction = if (mode is CalendarMode.MonthMode) 1f else 0f
        ValueAnimator.ofFloat(
            (calendarMode as CalendarMode.MonthMode).expandFraction,
            targetFraction
        ).apply {
            duration = 100
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
            scrollToPosition((if (focusedDayTime == -1L) System.currentTimeMillis() else focusedDayTime).parseIndex())
        }
    }

    private fun onExpandFraction(fraction: Float) {
        val dayHeight: Float = flowHeaderDayHeight
        val calendar = ScheduleConfig.selectedDayTime.calendar
        val monthStart = startOfDay(calendar.firstDayOfMonthTime).apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        }.timeInMillis
        val monthEnd = startOfDay(calendar.lastDayOfMonthTime).apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
        }.timeInMillis
        val weekCount: Int = (1 + monthEnd.dDays - monthStart.dDays).toInt() / 7
        val expandHeight = weekCount * dayHeight
        val collapseHeight = dayHeight
        Log.i(
            TAG,
            "onMonthModeExpandFraction: $fraction, $weekCount, ${
                sdf_yyyyMMddHHmmss.format(monthStart)
            }"
        )
        updateLayoutParams {
            height = (collapseHeight + (expandHeight - collapseHeight) * fraction).roundToInt()
        }
    }

    inner class Adapter : RecyclerView.Adapter<VH>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            return VH(parent.context)
        }

        override fun getItemCount(): Int = if (isMonthMode) monthCount else weekCount

        override fun onBindViewHolder(holder: VH, position: Int) {
            (holder.itemView as FlowHeaderView).run {
                this.calendar.timeInMillis =
                    startOfDay(this@FlowHeaderGroup.startTime).apply {
                        if (isMonthMode) {
                            add(Calendar.MONTH, position)
                        } else {
                            timeInMillis += position * 7 * dayMillis
                        }
                    }.timeInMillis
                this.focusedDayTime = this@FlowHeaderGroup.focusedDayTime
                this.calendarMode = this@FlowHeaderGroup.calendarMode
            }
        }

    }

    class VH(context: Context) : ViewHolder(FlowHeaderView(context))

}
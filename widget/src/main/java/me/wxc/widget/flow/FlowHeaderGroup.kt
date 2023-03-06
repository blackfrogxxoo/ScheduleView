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
            onMonthModeExpandFraction(mode.expandFraction)
        }
        childRenders.filterIsInstance<ICalendarModeHolder>().forEach {
            it.calendarMode = mode
        }
        adapter?.notifyDataSetChanged()
        scrollToPosition((if (focusedDayTime == -1L) System.currentTimeMillis() else focusedDayTime).parseIndex())
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
                    ScheduleConfig.onDateSelectedListener.invoke(
                        if (isMonthMode) {
                            startOfDay(startTime).apply {
                                add(Calendar.MONTH, position)
                            }
                        } else {
                            val startWeekDay = startOfDay(startTime).apply {
                                timeInMillis -= (get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY) * dayMillis
                            }.timeInMillis
                            startOfDay(startWeekDay).apply {
                                timeInMillis += position * 7 * dayMillis
                            }
                        }
                    )
                }
            }
        })
        scrollToPosition(System.currentTimeMillis().parseIndex())
    }

    internal fun autoSwitchMode(mode: CalendarMode) {
        Log.i(TAG, "autoSwitchMode: $calendarMode -> $mode")
        if (mode is CalendarMode.MonthMode) {
            if (calendarMode is CalendarMode.WeekMode) {
                calendarMode = CalendarMode.MonthMode(0f)
            }
            ValueAnimator.ofFloat((calendarMode as CalendarMode.MonthMode).expandFraction, 1f)
                .apply {
                    addUpdateListener {
                        calendarMode =
                            (calendarMode as CalendarMode.MonthMode).copy(expandFraction = it.animatedValue as Float)
                    }
                    doOnStart {
                        childRenders.filterIsInstance<ICalendarModeHolder>().forEach {
                            it.calendarMode = mode
                        }
                        adapter?.notifyDataSetChanged()
                        scrollToPosition((if (focusedDayTime == -1L) System.currentTimeMillis() else focusedDayTime).parseIndex())
                    }
                }.start()
        } else {
            ValueAnimator.ofFloat((calendarMode as CalendarMode.MonthMode).expandFraction, 0f)
                .apply {
                    addUpdateListener {
                        calendarMode =
                            (calendarMode as CalendarMode.MonthMode).copy(expandFraction = it.animatedValue as Float)
                    }
                    doOnEnd {
                        calendarMode = mode
                        childRenders.filterIsInstance<ICalendarModeHolder>().forEach {
                            it.calendarMode = mode
                        }
                        adapter?.notifyDataSetChanged()
                        scrollToPosition((if (focusedDayTime == -1L) System.currentTimeMillis() else focusedDayTime).parseIndex())
                    }
                }.start()
        }
    }


    private fun onMonthModeExpandFraction(fraction: Float) {
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
        Log.i(TAG, "onMonthModeExpandFraction: $fraction, $weekCount, ${sdf_yyyyMMddHHmmss.format(monthStart)}")
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
                ScheduleConfig.lifecycleScope.launch {
                    scheduleModels = withContext(Dispatchers.IO) {
                        ScheduleConfig.scheduleModelsProvider.invoke(
                            startTime,
                            endTime
                        )
                    }.apply {
                        Log.i(TAG, "$this")
                    }
                }
            }
        }

    }

    class VH(context: Context) : ViewHolder(FlowHeaderView(context))

}
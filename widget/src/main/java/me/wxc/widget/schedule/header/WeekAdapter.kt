package me.wxc.widget.schedule.header

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import androidx.core.view.children
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutParams
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import me.wxc.widget.ScheduleConfig
import me.wxc.widget.base.ICalendarParent
import me.wxc.widget.base.ICalendarRender
import me.wxc.widget.base.IScheduleModel
import me.wxc.widget.tools.*
import java.util.Calendar
import kotlin.math.abs

class WeekAdapter(private val recyclerView: RecyclerView) : RecyclerView.Adapter<VH>(),
    ICalendarRender, ICalendarParent {
    override val parentRender: ICalendarRender?
        get() = recyclerView.parent as? ICalendarRender
    override val calendar: Calendar = beginOfDay()
    override var focusedDayTime: Long by setter(-1) { _, time ->
        childRenders.forEach { it.focusedDayTime = time }
    }

    override var selectedDayTime: Long by setter(nowMillis) { oldTime, time ->
        if (!byDrag && abs(oldTime.dDays - time.dDays) < 30) {
            recyclerView.smoothScrollToPosition(time.parseWeekIndex())
        } else {
            recyclerView.scrollToPosition(time.parseWeekIndex())
        }
        recyclerView.post {
            childRenders.forEach { it.selectedDayTime = time }
        }
    }
    override var scheduleModels: List<IScheduleModel> = listOf()
    override val beginTime: Long
        get() = ScheduleConfig.scheduleBeginTime
    override val endTime: Long
        get() = ScheduleConfig.scheduleEndTime

    override val childRenders: List<ICalendarRender>
        get() = recyclerView.children.filterIsInstance<ICalendarRender>().toList()

    private val weekCount: Int by lazy {
        val startWeekDay = beginOfDay(beginTime).apply {
            timeInMillis -= (get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY) * dayMillis
        }.timeInMillis
        val result = ((endTime - startWeekDay) / (7 * dayMillis)).toInt()
        result.apply { Log.i(TAG, "week count = $result") }
    }
    private var byDrag = false

    init {
        recyclerView.run {
            adapter = this@WeekAdapter
            post {
                scrollToPosition(selectedDayTime.parseWeekIndex())
            }
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                private var lastPosition = -1

                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                        byDrag = true
                    } else if (newState == RecyclerView.SCROLL_STATE_IDLE) {
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
        }
    }

    private fun onPosition(position: Int) {
        val llm = recyclerView.layoutManager as LinearLayoutManager
        (llm.findViewByPosition(position) as? ICalendarRender)?.let {
            val beginTime = it.beginTime
            val endTime = it.endTime
            if (selectedDayTime < beginTime || selectedDayTime > endTime) {
                if (byDrag) {
                    val time = beginTime.calendar.apply {
                        set(Calendar.DAY_OF_WEEK, selectedDayTime.dayOfWeek)
                    }.timeInMillis
                    rootCalendarRender?.selectedDayTime = time
                    Log.i(TAG, "onPosition: ${time.yyyyMMddHHmmss}")
                    ScheduleConfig.onDateSelectedListener.invoke(time.calendar)
                }
            } else if (selectedDayTime in beginTime..endTime) {
                it.selectedDayTime = selectedDayTime
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(parent.context)
    }

    override fun getItemCount() = weekCount

    override fun onBindViewHolder(holder: VH, position: Int) {
        val weekView = holder.itemView as WeekView
        weekView.calendar.timeInMillis =
            beginOfDay(ScheduleConfig.scheduleBeginTime).timeInMillis + position * 7 * dayMillis
        weekView.reloadSchedulesFromProvider()
    }

    companion object {
        private const val TAG = "WeekAdapter"
    }
}

class VH(context: Context) : ViewHolder(WeekView(context).apply {
    layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
})
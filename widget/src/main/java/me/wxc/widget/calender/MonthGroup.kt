package me.wxc.widget.calender

import android.content.Context
import android.util.AttributeSet
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import me.wxc.widget.ScheduleConfig
import me.wxc.widget.base.ICalendarParent
import me.wxc.widget.base.ICalendarRender
import me.wxc.widget.base.IScheduleModel
import me.wxc.widget.tools.*
import java.util.*
import kotlin.math.abs

class MonthGroup @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : RecyclerView(context, attrs), ICalendarRender, ICalendarParent {
    override val parentRender: ICalendarRender? = null
    override val calendar: Calendar = beginOfDay()
    override var focusedDayTime: Long by setter(nowMillis) { _, time ->
        childRenders.forEach { it.focusedDayTime = time }
    }
    override var selectedDayTime: Long by setter(nowMillis) { _, time ->
        if (!isVisible) return@setter
        childRenders.forEach { it.selectedDayTime = time }
        post {
            val position = time.parseMonthIndex()
            if (abs(lastPosition - position) < 5) {
                smoothScrollToPosition(position)
            } else {
                scrollToPosition(position)
            }
            lastPosition = position
        }
    }
    override var scheduleModels: List<IScheduleModel> by setter(emptyList()) { _, list ->
        childRenders.forEach { it.getSchedulesFrom(list) }
    }
    override val beginTime: Long
        get() = ScheduleConfig.scheduleBeginTime
    override val endTime: Long
        get() = ScheduleConfig.scheduleEndTime

    override val childRenders: List<ICalendarRender>
        get() = children.filterIsInstance<ICalendarRender>().toList()

    private var lastPosition = -1

    init {
        layoutManager = LinearLayoutManager(context, HORIZONTAL, false)
        PagerSnapHelper().attachToRecyclerView(this)
        adapter = MonthAdapter()

        post {
            selectedDayTime = ScheduleConfig.selectedDayTime
        }
        addOnScrollListener(object : OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == SCROLL_STATE_IDLE) {
                    val llm = recyclerView.layoutManager as LinearLayoutManager
                    val position = llm.findFirstCompletelyVisibleItemPosition()
                    if (position != -1 && lastPosition != position) {
                        lastPosition = position
                        val calendar = beginOfDay(ScheduleConfig.scheduleBeginTime).apply {
                            add(Calendar.MONTH, position)
                        }
                        if (calendar.timeInMillis.dMonths == nowMillis.dMonths) {
                            ScheduleConfig.onDateSelectedListener.invoke(beginOfDay())
                        } else {
                            ScheduleConfig.onDateSelectedListener.invoke(calendar)
                        }
                        (llm.findViewByPosition(position) as? ICalendarRender)?.reloadSchedulesFromProvider()
                    }
                }
            }
        })
    }

    override fun reloadSchedulesFromProvider(onReload: () -> Unit) {
        childRenders.forEach {
            it.reloadSchedulesFromProvider(onReload)
        }
    }
}
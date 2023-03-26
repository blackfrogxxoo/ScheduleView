package me.wxc.widget.flow.list

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import me.wxc.widget.R
import me.wxc.widget.base.ICalendarRender
import me.wxc.widget.base.IScheduleModel
import me.wxc.widget.tools.*
import java.util.*

class ScheduleFlowView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : RecyclerView(context, attrs), ICalendarRender {
    private val flowAdapter: ScheduleFlowAdapter by lazy {
        ScheduleFlowAdapter()
    }
    override val parentRender: ICalendarRender?
        get() = parent as? ICalendarRender
    override val calendar: Calendar = beginOfDay()
    override var focusedDayTime: Long by setter(-1L) { _, time ->
        val temp = flowAdapter.currentList.toMutableList()
        temp.removeIf {
            (it as? FlowDailySchedules)?.beginTime?.dDays != nowMillis.dDays
                    && (it as? FlowDailySchedules)?.schedules?.isEmpty() == true
        }
        flowAdapter.submitList(temp)
        if (time == -1L || temp.filterIsInstance<FlowDailySchedules>().map { it.beginTime }
                .contains(time)) {
            return@setter
        }
        val emptyModel = FlowDailySchedules(
            time,
            listOf()
        )
        temp.add(emptyModel)
        var index = 0
        flowAdapter.submitList(temp.sortedBy { it.sortValue }.apply {
            index = indexOf(emptyModel)
        })
        if (time.dayOfMonth == 1) {
            index--
        }
        post {
            (layoutManager as LinearLayoutManager).scrollToPositionWithOffset(index.apply {
                Log.i(TAG, "focused: $time index: $this")
            }, 0)
        }
    }
    override var selectedDayTime: Long by setter(-1L) { _, time ->
        if (!byDrag) {
            flowAdapter.run {
                var index = 0
                currentList.toList().forEach { model ->
                    if (model.beginTime >= beginOfDay(time).timeInMillis) {
                        return@forEach
                    }
                    index++
                }
                (layoutManager as LinearLayoutManager).scrollToPositionWithOffset(index.apply {
                    Log.i(TAG, "focused: $time index: $this")
                }, 0)
            }
        }
    }
    override var scheduleModels: List<IScheduleModel> by setter(emptyList()) { _, list ->
        generateViewModels(list)
    }

    override var beginTime: Long by setter(nowMillis) { _, time ->
        Log.i(TAG, "update beginTime: ${time.yyyyMMddHHmmss}")
    }
    override var endTime: Long by setter(nowMillis) { _, time ->
        Log.i(TAG, "update endTime: ${time.yyyyMMddHHmmss}")
    }
    private var byDrag = false
    private var loadingMore = false

    init {
        layoutManager = LinearLayoutManager(context, VERTICAL, false)
        adapter = flowAdapter
        addOnScrollListener(object : OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == SCROLL_STATE_DRAGGING) {
                    byDrag = true
                } else if (newState == SCROLL_STATE_IDLE) {
                    byDrag = false
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val llm = recyclerView.layoutManager as LinearLayoutManager
                val firstVisible = llm.findFirstVisibleItemPosition()
                val lastVisible = llm.findLastVisibleItemPosition()
                (llm.findViewByPosition(firstVisible)?.tag as? Long)?.let {
                    if (byDrag) {
                        parentRender?.selectedDayTime = it
                    }
                }
                adjustCurrentDateText(firstVisible, lastVisible, llm)
                checkLoadMore(firstVisible, lastVisible)
            }
        })
    }

    private fun adjustCurrentDateText(
        firstVisible: Int,
        lastVisible: Int,
        llm: LinearLayoutManager
    ) {
        for (index in firstVisible..lastVisible) {
            (llm.findViewByPosition(index) as? ConstraintLayout)?.let { parent ->
                val dateContainer = parent.findViewById<View>(R.id.dateContainer)
                val scheduleList = parent.findViewById<View>(R.id.scheduleList)
                if (firstVisible == index) {
                    dateContainer.translationY = (-parent.top).toFloat()
                        .coerceAtMost((scheduleList.height - dateContainer.height).toFloat())
                        .coerceAtLeast(0f)
                } else {
                    dateContainer.translationY = 0f
                }
            }
        }
    }

    private fun checkLoadMore(firstVisible: Int, lastVisible: Int) {
        if (firstVisible < 10) {
            beginTime = beginTime.calendar.apply {
                add(Calendar.YEAR, -1)
            }.timeInMillis
            if (!loadingMore) {
                loadingMore = true
                reloadSchedulesFromProvider()
            }
        } else if (lastVisible > ((adapter?.itemCount ?: 0) - 10).coerceAtLeast(0)) {
            endTime = endTime.calendar.apply {
                add(Calendar.YEAR, 1)
            }.timeInMillis
            if (!loadingMore) {
                loadingMore = true
                reloadSchedulesFromProvider()
            }
        }
    }

    private fun generateViewModels(list: List<IScheduleModel>) {
        list.groupBy { it.beginTime.dDays }.values.map {
            FlowDailySchedules(
                beginTime = beginOfDay(it[0].beginTime).timeInMillis,
                schedules = it.sortedBy { model -> model.beginTime }
            )
        }.toMutableList<IFlowModel>().apply {
            val days = map { it.beginTime.dDays }
            for (time in beginTime..endTime step dayMillis) {
                if ((time.dDays == nowMillis.dDays || time.dDays == focusedDayTime.dDays) && !days.contains(
                        time.dDays
                    )
                ) {
                    add(
                        FlowDailySchedules(
                            beginTime = time,
                            schedules = emptyList()
                        )
                    )
                }
                if (time.dayOfMonth == 1) {
                    add(MonthText(time))
                }
                if (time.dayOfWeek == Calendar.SUNDAY) {
                    add(WeekText(time))
                }
            }
        }.sortedBy { it.sortValue }.apply {
            flowAdapter.submitList(this)
        }
        if (loadingMore) {
            loadingMore = false
        }
    }

    companion object {
        private const val TAG = "ScheduleFlowView"
    }
}
package me.wxc.widget.calender

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import me.wxc.widget.base.ICalendarRender
import me.wxc.widget.base.IScheduleModel
import me.wxc.widget.tools.*
import java.util.*

class DailyTaskListViewGroup @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : StableOrientationRecyclerView(context, attrs), ICalendarRender {
    override val parentRender: ICalendarRender
        get() = parent as ICalendarRender
    override val calendar: Calendar = beginOfDay()
    override val beginTime: Long
        get() = calendar.timeInMillis
    override val endTime: Long
        get() = calendar.timeInMillis + 7 * dayMillis
    override var focusedDayTime: Long by setter(-1L) { oldTime, time ->
        if (time >= 0) {
            val index = time.dDays - beginTime.dDays
            if (oldTime == -1L) {
                scrollToPosition(index.toInt())
            } else {
                smoothScrollToPosition(index.toInt())
            }
        }
    }
    override var scheduleModels: List<IScheduleModel> by setter(listOf()) { _, list ->
        adapter = if (beginTime == -1L) {
            null
        } else {
            Adapter(scheduleModels).apply {
                if (focusedDayTime != -1L) {
                    val index = focusedDayTime.dDays - beginTime.dDays
                    scrollToPosition(index.toInt())
                }
            }
        }
        if (adapter != null && focusedDayTime != -1L) {
            val index = focusedDayTime.dDays - beginTime.dDays
            smoothScrollToPosition(index.toInt()) // FIXME 增删任务后，不滑动一下显示不出来
        }
    }

    override var selectedDayTime: Long by setter(nowMillis) { _, time ->
        val index = time.dDays - beginTime.dDays
        if (focusedDayTime == -1L || focusedDayTime == time) {
            scrollToPosition(index.toInt())
        } else {
            smoothScrollToPosition(index.toInt())
        }
    }

    init {
        layoutManager = LinearLayoutManager(context, HORIZONTAL, false)
        PagerSnapHelper().attachToRecyclerView(this)
        addOnScrollListener(object : OnScrollListener() {
            private var lastPosition = -1
            private var dragged = false
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == SCROLL_STATE_DRAGGING) {
                    dragged = true
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val llm = recyclerView.layoutManager as LinearLayoutManager
                val position = llm.findFirstCompletelyVisibleItemPosition()
                if (position != -1 && lastPosition != position) {
                    lastPosition = position
                    if (dragged) {
                        parentRender.focusedDayTime = beginTime + position * dayMillis
                    }
                    dragged = false
                }
            }
        })
    }

    override fun dispatchTouchEvent(e: MotionEvent): Boolean {
        parent.requestDisallowInterceptTouchEvent(true)
        return super.dispatchTouchEvent(e)
    }

    inner class Adapter(private val scheduleModels: List<IScheduleModel>) :
        RecyclerView.Adapter<VH>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            return VH(DailyTaskListView(parent.context).apply {
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            })
        }

        override fun getItemCount() = 7

        override fun onBindViewHolder(holder: VH, position: Int) {
            val from = beginTime + position * dayMillis
            holder.dailyTaskListView.calendar.timeInMillis = from
            holder.dailyTaskListView.getSchedulesFrom(scheduleModels)
        }


    }

    class VH(val dailyTaskListView: DailyTaskListView) : ViewHolder(dailyTaskListView)

}
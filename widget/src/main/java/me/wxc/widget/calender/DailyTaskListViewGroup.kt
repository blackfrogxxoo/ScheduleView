package me.wxc.widget.calender

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import me.wxc.widget.base.ICalendarRender
import me.wxc.widget.base.ISchedulerModel
import me.wxc.widget.tools.dDays
import me.wxc.widget.tools.dayMills
import me.wxc.widget.tools.startOfDay
import java.util.*

class DailyTaskListViewGroup @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : StableOrientationRecyclerView(context, attrs), ICalendarRender {
    override val parentRender: ICalendarRender
        get() = parent as ICalendarRender
    override val calendar: Calendar = startOfDay()
    override val startTime: Long
        get() = calendar.timeInMillis
    override val endTime: Long
        get() = calendar.timeInMillis + 7 * dayMills
    override var selectedTime: Long = -1L
        set(value) {
            if (field == value) return
            onSelectedTime(value)
            field = value
        }
    override var schedulerModels: List<ISchedulerModel> = listOf()
        set(value) {
            field = value
            adapter = if (startTime == -1L) {
                null
            } else {
                Adapter(startTime, schedulerModels)
            }
            if (selectedTime != -1L) {
                onSelectedTime(selectedTime)
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
                        parentRender.selectedTime = startTime + position * dayMills
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

    private fun onSelectedTime(time: Long) {
        val index = time.dDays - startTime.dDays
        if (selectedTime == -1L || selectedTime == time) {
            scrollToPosition(index.toInt())
        } else {
            smoothScrollToPosition(index.toInt())
        }
    }

    class Adapter(
        private val startTime: Long,
        private val schedulerModels: List<ISchedulerModel>
    ) : RecyclerView.Adapter<VH>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            return VH(DailyTaskListView(parent.context).apply {
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            })
        }

        override fun getItemCount() = 7

        override fun onBindViewHolder(holder: VH, position: Int) {
            val from = startTime + position * dayMills
            val to = from + dayMills
            schedulerModels.filter { it.startTime >= from && it.endTime <= to }.run {
                holder.dailyTaskListView.calendar.timeInMillis = from
                holder.dailyTaskListView.schedulerModels = this
            }
        }


    }

    class VH(val dailyTaskListView: DailyTaskListView) : ViewHolder(dailyTaskListView)

}
package me.wxc.widget.calender

import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.core.view.descendants
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutParams
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.wxc.widget.SchedulerConfig
import me.wxc.widget.SchedulerConfig.lifecycleScope
import me.wxc.widget.base.ISelectedDayTimeHolder
import me.wxc.widget.tools.TAG
import me.wxc.widget.tools.dMonths
import me.wxc.widget.tools.startOfDay
import java.util.Calendar

class MonthAdapter(private val recyclerView: RecyclerView) : RecyclerView.Adapter<VH>(),
    ISelectedDayTimeHolder {

    override var selectedDayTime: Long
        get() = SchedulerConfig.selectedDayTime
        set(value) {
            val selectedDay = startOfDay(value)
            val firstDay = startOfDay(SchedulerConfig.schedulerStartTime)
            val position =
                12 * (selectedDay.get(Calendar.YEAR) - firstDay.get(Calendar.YEAR)) + (selectedDay.get(
                    Calendar.MONTH
                ) - firstDay.get(Calendar.MONTH))
            recyclerView.scrollToPosition(position)
            recyclerView.descendants.filterIsInstance<ISelectedDayTimeHolder>()
                .filter { (it as? View)?.isAttachedToWindow == true }
                .forEach { baseRender ->
                    baseRender.selectedDayTime = value
                }
        }

    private val monthCount: Int by lazy {
        val start = startOfDay(SchedulerConfig.schedulerStartTime)
        val end = startOfDay(SchedulerConfig.schedulerEndTime)
        val result =
            (end.get(Calendar.YEAR) - start.get(Calendar.YEAR)) * 12 + (end.get(Calendar.MONTH) - start.get(
                Calendar.MONTH
            ))
        result.apply { Log.i("MonthAdapter", "month count = $result") }
    }

    init {
        recyclerView.run {
            post {
                selectedDayTime = SchedulerConfig.selectedDayTime
            }
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                private var lastPosition = -1
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val llm = recyclerView.layoutManager as LinearLayoutManager
                    val position = llm.findFirstCompletelyVisibleItemPosition()
                    if (position != -1 && lastPosition != position) {
                        lastPosition = position
                        val calendar = startOfDay(SchedulerConfig.schedulerStartTime).apply {
                            add(Calendar.MONTH, position)
                        }
                        if (calendar.timeInMillis.dMonths == System.currentTimeMillis().dMonths) {
                            SchedulerConfig.onDateSelectedListener.invoke(startOfDay())
                        } else {
                            SchedulerConfig.onDateSelectedListener.invoke(calendar)
                        }
                    }
                }
            })
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(parent.context)
    }

    override fun getItemCount() = monthCount

    override fun onBindViewHolder(holder: VH, position: Int) {
        val monthView = holder.itemView as MonthView
        monthView.calendar.timeInMillis = startOfDay(SchedulerConfig.schedulerStartTime).apply {
            add(Calendar.MONTH, position)
        }.timeInMillis

        lifecycleScope.launch {
            monthView.schedulerModels = withContext(Dispatchers.IO) {
                SchedulerConfig.schedulerModelsProvider.invoke(
                    monthView.startTime,
                    monthView.endTime
                )
            }.apply {
                Log.i(monthView.TAG, "$this")
            }
        }
    }

    fun refreshCurrentItem() {
        recyclerView.descendants.filterIsInstance<MonthView>().filter { it.isAttachedToWindow }
            .forEach { monthView ->
                lifecycleScope.launch {
                    monthView.schedulerModels = withContext(Dispatchers.IO) {
                        SchedulerConfig.schedulerModelsProvider.invoke(
                            monthView.startTime,
                            monthView.endTime
                        )
                    }.apply {
                        Log.i(monthView.TAG, "$this")
                    }
                }
            }
    }
}

class VH(context: Context) : ViewHolder(MonthView(context).apply {
    layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
})
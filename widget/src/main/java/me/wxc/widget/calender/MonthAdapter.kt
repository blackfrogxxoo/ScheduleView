package me.wxc.widget.calender

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutParams
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import me.wxc.widget.ScheduleConfig
import me.wxc.widget.tools.*
import java.util.Calendar

class MonthAdapter : RecyclerView.Adapter<VH>() {

    private val monthCount: Int by lazy {
        val start = beginOfDay(ScheduleConfig.scheduleBeginTime)
        val end = beginOfDay(ScheduleConfig.scheduleEndTime)
        val result =
            (end.get(Calendar.YEAR) - start.get(Calendar.YEAR)) * 12 + (end.get(Calendar.MONTH) - start.get(
                Calendar.MONTH
            ))
        result.apply { Log.i("MonthAdapter", "month count = $result") }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(parent.context)
    }

    override fun getItemCount() = monthCount

    override fun onBindViewHolder(holder: VH, position: Int) {
        val monthView = holder.itemView as MonthView
        monthView.calendar.timeInMillis = beginOfDay(ScheduleConfig.scheduleBeginTime).apply {
            add(Calendar.MONTH, position)
        }.timeInMillis
        monthView.reloadSchedulesFromProvider()
    }
}

class VH(context: Context) : ViewHolder(MonthView(context).apply {
    layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
})
package me.wxc.widget.calender

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import me.wxc.widget.R
import me.wxc.widget.base.ICalendarRender
import me.wxc.widget.base.ISchedulerModel
import me.wxc.widget.scheduler.components.DailyTaskModel
import me.wxc.widget.tools.dayMills
import me.wxc.widget.tools.sdf_yyyyMMddHHmmss
import me.wxc.widget.tools.startOfDay
import java.util.*

class DailyTaskListView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs), ICalendarRender {
    private val recyclerView: RecyclerView
    private val emptyView: TextView

    override val parentRender: ICalendarRender
        get() = parent as ICalendarRender
    override val calendar: Calendar= startOfDay()
    override val startTime: Long
        get() = calendar.timeInMillis
    override val endTime: Long
        get() = calendar.timeInMillis + dayMills
    override var selectedTime: Long = -1L
        set(value) {
            field = value
        }
    override var schedulerModels: List<ISchedulerModel> = listOf()
        set(value) {
            field = value
            if (value.any()) {
                recyclerView.visibility = View.VISIBLE
                emptyView.visibility = View.GONE
                recyclerView.adapter = Adapter(value)
            } else {
                recyclerView.visibility = View.GONE
                emptyView.visibility = View.VISIBLE
            }
        }


    init {
        inflate(context, R.layout.daily_task_list_view, this)
        recyclerView = findViewById(R.id.recyclerView)
        emptyView = findViewById(R.id.emptyView)
    }

    class Adapter(private val schedulerModels: List<ISchedulerModel>) : RecyclerView.Adapter<VH>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.daily_task_item_view, parent, false)
            return VH(view)
        }

        override fun getItemCount() = schedulerModels.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val model = schedulerModels[position]
            holder.title.text = (model as? DailyTaskModel)?.title
            holder.timeRange.text = "${sdf_yyyyMMddHHmmss.format(model.startTime)} ~ ${sdf_yyyyMMddHHmmss.format(model.endTime)}"
            holder.fg.visibility = if (model.startTime < System.currentTimeMillis()) VISIBLE else GONE
        }

    }

    class VH(itemView: View) : ViewHolder(itemView) {
        val title: TextView
        val timeRange: TextView
        val fg: View
        init {
            title = itemView.findViewById(R.id.title)
            timeRange = itemView.findViewById(R.id.timeRange)
            fg = itemView.findViewById(R.id.fg)
        }
    }
}
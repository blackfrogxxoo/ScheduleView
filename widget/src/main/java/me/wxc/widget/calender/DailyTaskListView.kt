package me.wxc.widget.calender

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import me.wxc.widget.R
import me.wxc.widget.SchedulerConfig
import me.wxc.widget.base.ICalendarRender
import me.wxc.widget.base.ISchedulerModel
import me.wxc.widget.scheduler.components.CreateTaskModel
import me.wxc.widget.scheduler.components.DailyTaskModel
import me.wxc.widget.tools.*
import java.util.*

class DailyTaskListView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs), ICalendarRender {
    private val recyclerView: RecyclerView
    private val emptyView: TextView

    override val parentRender: ICalendarRender
        get() = parent as ICalendarRender
    override val calendar: Calendar = startOfDay()
    override val startTime: Long
        get() = calendar.timeInMillis
    override val endTime: Long
        get() = calendar.timeInMillis + dayMillis
    override var focusedDayTime: Long = -1L
        set(value) {
            field = value
        }
    override var schedulerModels: List<ISchedulerModel> = listOf()
        set(value) {
            field = value
            if (value.any()) {
                recyclerView.visibility = View.VISIBLE
                emptyView.visibility = View.GONE
                recyclerView.adapter = Adapter()
            } else {
                recyclerView.visibility = View.GONE
                emptyView.visibility = View.VISIBLE
                recyclerView.adapter = null
            }
        }


    init {
        inflate(context, R.layout.daily_task_list_view, this)
        recyclerView = findViewById(R.id.recyclerView)
        emptyView = findViewById(R.id.emptyView)
        emptyView.text = buildSpannedString {
            append("暂无日程安排，")
            color(SchedulerConfig.colorBlue1) {
                append("点击创建")
            }
        }
        emptyView.setOnClickListener {
            SchedulerConfig.onCreateTaskClickBlock(CreateTaskModel(
                startTime = SchedulerConfig.selectedDayTime + 10 * hourMillis,
                duration = quarterMillis * 2,
                onNeedScrollBlock = { _, _ -> }
            ))
        }
    }

    inner class Adapter : RecyclerView.Adapter<ViewHolder>() {
        private var nowLineIndex = -1
        private var size = 0

        init {
            generateSize()
        }

        private fun generateSize() {
            size = schedulerModels.size
            if (size > 0 && schedulerModels[0].startTime.dDays == System.currentTimeMillis().dDays) {
                size += 1
                nowLineIndex = 0
                schedulerModels.forEach {
                    if (it.endTime < System.currentTimeMillis()) {
                        nowLineIndex++
                    } else {
                        return
                    }
                }
            }
        }

        override fun getItemViewType(position: Int): Int {
            return if (position == nowLineIndex) TYPE_NOW_LINE else TYPE_TASK
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            if (viewType == TYPE_NOW_LINE) {
                return NowLineVH(parent.context)
            }
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.daily_task_item_view, parent, false)
            return VH(view)
        }

        override fun getItemCount() = size

        override fun onBindViewHolder(holder: ViewHolder, p: Int) {
            if (holder is VH) {
                if (p == nowLineIndex) return
                var position = p
                if (nowLineIndex in 0 until p) {
                    position -= 1
                }
                val model = schedulerModels[position]
                holder.title.text = (model as? DailyTaskModel)?.title
                holder.timeRange.text =
                    "${sdf_yyyyMMddHHmmss.format(model.startTime)} ~ ${
                        sdf_yyyyMMddHHmmss.format(
                            model.endTime
                        )
                    }"
                holder.fg.visibility =
                    if (model.endTime < System.currentTimeMillis()) VISIBLE else GONE
                holder.itemView.setOnClickListener {
                    SchedulerConfig.onDailyTaskClickBlock(model as DailyTaskModel)
                }
            }
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

    class NowLineVH(context: Context) : ViewHolder(NowLineView(context))

    private companion object {
        private const val TYPE_TASK = 0
        private const val TYPE_NOW_LINE = 1
    }
}
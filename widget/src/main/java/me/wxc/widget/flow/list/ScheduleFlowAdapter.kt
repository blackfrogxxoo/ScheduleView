package me.wxc.widget.flow.list

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import me.wxc.widget.R
import me.wxc.widget.ScheduleConfig
import me.wxc.widget.base.DailyTaskModel
import me.wxc.widget.base.EditingTaskModel
import me.wxc.widget.base.IScheduleModel
import me.wxc.widget.calender.NowLineView
import me.wxc.widget.tools.*
import java.util.*

class ScheduleFlowAdapter : ListAdapter<IFlowModel, VH>(
    object : DiffUtil.ItemCallback<IFlowModel>() {
        override fun areItemsTheSame(
            oldItem: IFlowModel,
            newItem: IFlowModel
        ) = oldItem == newItem

        override fun areContentsTheSame(
            oldItem: IFlowModel,
            newItem: IFlowModel
        ): Boolean {
            if (oldItem is MonthText && newItem is MonthText) {
                return oldItem.beginTime == newItem.beginTime
            } else if (oldItem is WeekText && newItem is WeekText) {
                return oldItem.beginTime == newItem.beginTime
            } else if (oldItem is FlowDailySchedules && newItem is FlowDailySchedules) {
                return oldItem.beginTime == newItem.beginTime && oldItem.schedules == newItem.schedules
            }
            return false
        }
    }
) {
    private val MONTH_TEXT = 1
    private val WEEK_TEXT = 2
    private val DAILY_TASK = 3
    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is MonthText -> MONTH_TEXT
            is WeekText -> WEEK_TEXT
            else -> DAILY_TASK
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return when (viewType) {
            MONTH_TEXT -> MonthTextVH(parent.context)
            WEEK_TEXT -> WeekTextVH(parent.context)
            else -> DailyTaskVH(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.flow_daily_item, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.itemView.tag = getItem(position).beginTime
        holder.onBind(getItem(position))
    }

}

abstract class VH(view: View) : RecyclerView.ViewHolder(view) {
    abstract fun onBind(scheduleModel: IScheduleModel)
}

class MonthTextVH(context: Context) : VH(TextView(context).apply {
    textSize = 24f
    typeface = ResourcesCompat.getFont(context, R.font.product_sans_bold3)
    setTextColor(ScheduleConfig.colorBlack1)
    updatePadding(left = 15.dp, top = 15.dp, bottom = 15.dp)
}) {
    override fun onBind(scheduleModel: IScheduleModel) {
        (itemView as TextView).text =
            if (scheduleModel.beginTime.years == nowMillis.years) {
                scheduleModel.beginTime.M
            } else {
                scheduleModel.beginTime.yyyyM
            }
    }

}

class WeekTextVH(context: Context) : VH(TextView(context).apply {
    textSize = 14f
    typeface = ResourcesCompat.getFont(context, R.font.product_sans_regular2)
    setTextColor(ScheduleConfig.colorBlack3)
    updatePadding(left = 30.dp, top = 15.dp, bottom = 15.dp)
}) {
    override fun onBind(scheduleModel: IScheduleModel) {
        val beginTime = scheduleModel.beginTime
        val endTime = scheduleModel.endTime
        (itemView as TextView).text =
            "第${beginTime.calendar.get(Calendar.WEEK_OF_YEAR)}周，" +
                    "${beginTime.Md} ~ ${(endTime - dayMillis).Md}"
    }
}

class DailyTaskVH(itemView: View) : VH(itemView) {
    private val weekDay: TextView
    private val monthDay: TextView
    private val dateContainer: LinearLayout
    private val scheduleList: LinearLayout

    init {
        weekDay = itemView.findViewById(R.id.weekDay)
        monthDay = itemView.findViewById(R.id.monthDay)
        scheduleList = itemView.findViewById(R.id.scheduleList)
        dateContainer = itemView.findViewById(R.id.dateContainer)
    }

    override fun onBind(scheduleModel: IScheduleModel) {
        (scheduleModel as? FlowDailySchedules)?.apply {
            dateContainer.translationY = 0f
            weekDay.text = scheduleModel.beginTime.dayOfWeekText
            monthDay.text = scheduleModel.beginTime.dayOfMonth.toString()
            scheduleList.removeAllViews()
            schedules.filterIsInstance<DailyTaskModel>().forEach { taskModel ->
                val view = LayoutInflater.from(itemView.context)
                    .inflate(R.layout.flow_daily_task_item, scheduleList, false)
                view.findViewById<TextView>(R.id.title).text = taskModel.title
                view.findViewById<TextView>(R.id.timeRange).text =
                    "${taskModel.beginTime.HHmm} ~ ${taskModel.endTime.HHmm}"
                view.findViewById<View>(R.id.fg).visibility =
                    if (taskModel.endTime < nowMillis) FrameLayout.VISIBLE else FrameLayout.GONE
                view.setOnClickListener {
                    ScheduleConfig.onDailyTaskClickBlock(taskModel)
                }
                scheduleList.addView(view)
            }
            if (schedules.none()) {
                scheduleList.addView(TextView(itemView.context).apply {
                    updatePadding(left = 15.dp, top = 15.dp, right = 15.dp, bottom = 15.dp)
                    textSize = 14f
                    setTextColor(ScheduleConfig.colorBlack2)
                    text = buildSpannedString {
                        append("暂无日程安排，")
                        color(ScheduleConfig.colorBlue1) {
                            append("点击创建")
                        }
                    }
                    setOnClickListener {
                        ScheduleConfig.onCreateTaskClickBlock(
                            DailyTaskModel(
                                beginTime = scheduleModel.beginTime + 10 * hourMillis,
                                editingTaskModel = EditingTaskModel()
                            )
                        )
                    }
                })
            }
            if (scheduleModel.beginTime.dDays == nowMillis.dDays) {
                var nowIndex = 0
                if (schedules.none()) nowIndex++
                schedules.forEach { iScheduleModel ->
                    if (iScheduleModel.endTime < nowMillis) {
                        nowIndex++
                    }
                }
                scheduleList.addView(NowLineView(itemView.context), nowIndex)
            }
        }
    }

}

package me.wxc.widget.flow.list

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import me.wxc.widget.R
import me.wxc.widget.ScheduleConfig
import me.wxc.widget.base.ICalendarRender
import me.wxc.widget.base.IScheduleModel
import me.wxc.widget.schedule.components.DailyTaskModel
import me.wxc.widget.tools.*
import java.util.*
import kotlin.properties.Delegates

class FlowListItemView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : RecyclerView(context, attrs), ICalendarRender {
    override val parentRender: ICalendarRender?
        get() = parent as? ICalendarRender
    override val calendar: Calendar = startOfDay()
    override var focusedDayTime: Long by Delegates.observable(-1L) { _, _, time ->
        // TODO 新建日程
    }
    override var scheduleModels: List<IScheduleModel> by Delegates.observable(emptyList()) { _, _, list ->
        adapterModels = scheduleModels.toMutableList().apply {
            add(MonthText(startTime))
            for (time in startTime..endTime step dayMillis) {
                if (time.dayOfWeek == Calendar.SUNDAY) {
                    add(WeekText(time))
                }
            }
        }.sortedBy { it.startTime }
        adapter?.notifyDataSetChanged()
    }
    override val startTime: Long
        get() = calendar.timeInMillis
    override val endTime: Long
        get() = calendar.apply {
            add(Calendar.MONTH, 1)
        }.timeInMillis

    private var adapterModels: List<IScheduleModel> = listOf()

    init {
        layoutManager = LinearLayoutManager(context, VERTICAL, false)
        adapter = Adapter()
        isNestedScrollingEnabled = false
    }

    inner class Adapter : RecyclerView.Adapter<VH>() {
        private val MONTH_TEXT = 1
        private val WEEK_TEXT = 2
        private val DAILY_TASK = 3
        override fun getItemViewType(position: Int): Int {
            return when (adapterModels[position]) {
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
                        .inflate(R.layout.flow_daily_task_item, parent, false)
                )
            }
        }

        override fun getItemCount(): Int {
            return adapterModels.size
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.onBind(adapterModels[position])
        }

    }

    abstract class VH(view: View) : ViewHolder(view) {
        abstract fun onBind(scheduleModel: IScheduleModel)
    }

    class MonthTextVH(context: Context) : VH(TextView(context).apply {
        textSize = 24f
        typeface = ResourcesCompat.getFont(context, R.font.product_sans_bold3)
        setTextColor(ScheduleConfig.colorBlack1)
        updatePadding(left = 15.dp, top = 15.dp, bottom = 15.dp)
    }) {
        override fun onBind(scheduleModel: IScheduleModel) {
            (itemView as TextView).text = sdf_yyyyM.format(scheduleModel.startTime)
        }

    }

    class WeekTextVH(context: Context) : VH(TextView(context).apply {
        textSize = 14f
        typeface = ResourcesCompat.getFont(context, R.font.product_sans_regular2)
        setTextColor(ScheduleConfig.colorBlack2)
        updatePadding(left = 30.dp, top = 15.dp, bottom = 15.dp)
    }) {
        override fun onBind(scheduleModel: IScheduleModel) {
            val startTime = scheduleModel.startTime
            val endTime = scheduleModel.endTime
            (itemView as TextView).text =
                "第${startTime.calendar.get(Calendar.WEEK_OF_YEAR)}周，${sdf_Md.format(startTime)} ~ ${
                    sdf_Md.format(endTime - dayMillis)
                }"
        }
    }

    class DailyTaskVH(itemView: View) : VH(itemView) {
        val title: TextView
        val timeRange: TextView
        val weekDay: TextView
        val monthDay: TextView
        val fg: View

        init {
            title = itemView.findViewById(R.id.title)
            timeRange = itemView.findViewById(R.id.timeRange)
            weekDay = itemView.findViewById(R.id.weekDay)
            monthDay = itemView.findViewById(R.id.monthDay)
            fg = itemView.findViewById(R.id.fg)
        }

        override fun onBind(scheduleModel: IScheduleModel) {
            title.text = (scheduleModel as? DailyTaskModel)?.title
            timeRange.text =
                "${sdf_HHmm.format(scheduleModel.startTime)} ~ ${
                    sdf_HHmm.format(
                        scheduleModel.endTime
                    )
                }"
            fg.visibility =
                if (scheduleModel.endTime < System.currentTimeMillis()) FrameLayout.VISIBLE else FrameLayout.GONE
            weekDay.text = scheduleModel.startTime.dayOfWeekText
            monthDay.text = scheduleModel.startTime.dayOfMonth.toString()
            itemView.setOnClickListener {
                ScheduleConfig.onDailyTaskClickBlock(scheduleModel as DailyTaskModel)
            }
        }

    }


    data class MonthText(
        override val startTime: Long,
    ) : IScheduleModel {
        override val endTime: Long = startTime.calendar.lastDayOfMonthTime
    }

    data class WeekText(
        override val startTime: Long,
    ) : IScheduleModel {
        override val endTime: Long = startTime + 7 * dayMillis
    }
}
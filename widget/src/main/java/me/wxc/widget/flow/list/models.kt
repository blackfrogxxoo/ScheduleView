package me.wxc.widget.flow.list

import me.wxc.widget.base.IScheduleModel
import me.wxc.widget.tools.calendar
import me.wxc.widget.tools.dayMillis
import me.wxc.widget.tools.lastDayOfMonthTime

interface IFlowModel : IScheduleModel {
    val sortValue: Long
}

data class MonthText(
    override val beginTime: Long,
) : IFlowModel {
    override val sortValue: Long = beginTime
    override val endTime: Long = beginTime.calendar.lastDayOfMonthTime
}

data class WeekText(
    override val beginTime: Long,
) : IFlowModel {
    override val sortValue: Long = beginTime + 1
    override val endTime: Long = beginTime + 7 * dayMillis
}

data class FlowDailySchedules(
    override val beginTime: Long,
    val schedules: List<IScheduleModel>
) : IFlowModel {
    override val sortValue: Long = beginTime + 2
    override val endTime: Long = beginTime + dayMillis
}
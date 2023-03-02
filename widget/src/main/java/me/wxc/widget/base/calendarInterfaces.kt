package me.wxc.widget.base

import java.util.Calendar

interface ICalendarRender : ITimeRangeHolder {
    val parentRender: ICalendarRender?
    val calendar: Calendar
    var focusedDayTime: Long
    var scheduleModels: List<IScheduleModel>

    fun getSchedulesFrom(from: List<IScheduleModel>) {
        scheduleModels = from.filter { it.startTime >= startTime && it.endTime <= endTime }
            .sortedBy { it.startTime }
    }
}

interface ICalendarParent {
    val children: List<ICalendarRender>
}
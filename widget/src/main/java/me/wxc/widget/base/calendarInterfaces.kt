package me.wxc.widget.base

import java.util.Calendar

interface ICalendarRender : ITimeRangeHolder {
    val parentRender: ICalendarRender?
    val calendar: Calendar
    var focusedDayTime: Long
    var schedulerModels: List<ISchedulerModel>

    fun getSchedulersFrom(from: List<ISchedulerModel>) {
        schedulerModels = from.filter { it.startTime >= startTime && it.endTime <= endTime }
            .sortedBy { it.startTime }
    }
}

interface ICalendarParent {
    val children: List<ICalendarRender>
}
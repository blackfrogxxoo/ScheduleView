package me.wxc.widget.base

import java.util.Calendar

interface ICalendarRender {
    val parentRender: ICalendarRender?
    val calendar: Calendar
    val startTime: Long
    val endTime: Long
    var selectedTime: Long
    var schedulerModels: List<ISchedulerModel>
}

interface ICalendarParent {
    val children: List<ICalendarRender>
}
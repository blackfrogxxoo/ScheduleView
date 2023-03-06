package me.wxc.widget.base

import java.util.*

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
    val childRenders: List<ICalendarRender>
}

interface ICalendarModeHolder {
    var calendarMode: CalendarMode
}

sealed interface CalendarMode {
    data class MonthMode(
        val expandFraction: Float = 0f,
        val touching: Boolean = false
    ) : CalendarMode

    object WeekMode : CalendarMode
}
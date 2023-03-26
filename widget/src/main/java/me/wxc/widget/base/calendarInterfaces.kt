package me.wxc.widget.base

import android.view.View
import androidx.core.view.isVisible
import me.wxc.widget.ScheduleConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

interface ICalendarRender : ITimeRangeHolder {
    val parentRender: ICalendarRender?
    val calendar: Calendar
    var focusedDayTime: Long
    var selectedDayTime: Long
    var scheduleModels: List<IScheduleModel>

    fun getSchedulesFrom(from: List<IScheduleModel>) {
        scheduleModels = from.filter { it.beginTime >= beginTime && it.endTime <= endTime }
            .sortedBy { it.beginTime }
    }

    val rootCalendarRender: ICalendarRender?
        get()  {
            if (parentRender != null) {
                return if (parentRender?.parentRender == null) {
                    parentRender
                } else {
                    parentRender?.rootCalendarRender
                }
            }
            return null
        }

    fun reloadSchedulesFromProvider(onReload: () -> Unit = {}) {
        ScheduleConfig.lifecycleScope.launch {
            scheduleModels = withContext(Dispatchers.IO) {
                ScheduleConfig.scheduleModelsProvider.invoke(
                    beginTime,
                    endTime
                )
            }
            onReload()
        }
    }

    fun isVisible() : Boolean = (this as? View)?.isVisible == true
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
    ) : CalendarMode

    object WeekMode : CalendarMode
}
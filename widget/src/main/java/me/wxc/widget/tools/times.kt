package me.wxc.widget.tools

import java.text.SimpleDateFormat
import java.util.*


val sdf_HHmm = SimpleDateFormat("HH:mm", Locale.ROOT)

val hourMills = 60 * 60 * 1000L
val dayMills = 24 * hourMills

val Long.hours: Int
    get() = run {
        Calendar.getInstance().apply {
            time = Date(this@run)
        }.get(Calendar.HOUR_OF_DAY)
    }
val Long.days: Int
    get() = run {
        Calendar.getInstance().apply {
            time = Date(this@run)
        }.get(Calendar.DAY_OF_YEAR)
    }
val Long.years: Int
    get() = run {
        Calendar.getInstance().apply {
            time = Date(this@run)
        }.get(Calendar.YEAR)
    }

val Long.calendar: Calendar
    get() = Calendar.getInstance().apply { timeInMillis = this@calendar }

val Long.dayOfMonth: Int
    get() = calendar.get(Calendar.DAY_OF_MONTH)

val Long.dayOfWeekText: String
    get() = run {
        when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> "周日"
            Calendar.MONDAY -> "周一"
            Calendar.TUESDAY -> "周二"
            Calendar.WEDNESDAY -> "周三"
            Calendar.THURSDAY -> "周四"
            Calendar.FRIDAY -> "周五"
            else -> "周六"
        }
    }

val Long.hhMM: String
    get() = sdf_HHmm.format(this)

fun startOfDay(timestamp: Long = System.currentTimeMillis()) = Calendar.getInstance().apply {
    time = Date(timestamp)
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}
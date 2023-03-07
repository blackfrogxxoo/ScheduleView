package me.wxc.widget.tools

import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt


val sdf_HHmm = SimpleDateFormat("HH:mm", Locale.ROOT)
val sdf_yyyyMMddHHmmss = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT)
val sdf_yyyyM = SimpleDateFormat("yyyy年M月")
val sdf_Md = SimpleDateFormat("M月d日")

val quarterMillis = 15 * 60 * 1000L
val hourMillis = 60 * 60 * 1000L
val dayMillis = 24 * hourMillis

val Long.hours: Int
    get() = run {
        Calendar.getInstance().apply {
            time = Date(this@run)
        }.get(Calendar.HOUR_OF_DAY)
    }
val Long.dDays: Long
    get() = (startOfDay(this).timeInMillis - startOfDay().timeInMillis) / dayMillis
val Long.dMonths: Int
    get() = (years - System.currentTimeMillis().years) * 12 + monthOfYear - System.currentTimeMillis().monthOfYear

val Long.years: Int
    get() = run {
        Calendar.getInstance().apply {
            time = Date(this@run)
        }.get(Calendar.YEAR)
    }
val Long.monthOfYear: Int
    get() = run {
        Calendar.getInstance().apply {
            time = Date(this@run)
        }.get(Calendar.MONTH)
    }

val Long.calendar: Calendar
    get() = Calendar.getInstance().apply { timeInMillis = this@calendar }

val Long.dayOfWeek: Int
    get() = calendar.get(Calendar.DAY_OF_WEEK)

val Long.dayOfMonth: Int
    get() = calendar.get(Calendar.DAY_OF_MONTH)

val Long.dayOfYear: Int
    get() = calendar.get(Calendar.DAY_OF_YEAR)

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

val Long.dayOfWeekTextSimple: String
    get() = run {
        when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> "日"
            Calendar.MONDAY -> "一"
            Calendar.TUESDAY -> "二"
            Calendar.WEDNESDAY -> "三"
            Calendar.THURSDAY -> "四"
            Calendar.FRIDAY -> "五"
            else -> "六"
        }
    }

val Long.hhMM: String
    get() = sdf_HHmm.format(this)

val Calendar.firstDayOfMonthTime: Long
    get() = startOfDay(timeInMillis).apply {
        set(Calendar.DAY_OF_MONTH, getActualMinimum(Calendar.DAY_OF_MONTH))
    }.timeInMillis

val Calendar.lastDayOfMonthTime: Long
    get() = startOfDay(timeInMillis).apply {
        set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
    }.timeInMillis

val Calendar.firstDayOfWeekTime: Long
    get() = startOfDay(timeInMillis).apply {
        set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
    }.timeInMillis

val Calendar.lastDayOfWeekTime: Long
    get() = startOfDay(timeInMillis).apply {
        set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
    }.timeInMillis

fun startOfDay(timestamp: Long = System.currentTimeMillis()) = Calendar.getInstance().apply {
    time = Date(timestamp)
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}

fun Long.adjustTimeInDay(period: Long, roundTo: Boolean): Long {
    val zeroOfDay = startOfDay(this).timeInMillis
    return if (!roundTo) {
        this - (this - zeroOfDay) % period
    } else {
        (zeroOfDay + (1f * (this - zeroOfDay) / period).roundToInt() * period)
    }
}

fun Long.adjustTimeSelf(period: Long, roundTo: Boolean): Long {
    return if (roundTo) {
        (1f * this / period).roundToInt() * period
    } else {
        (1f * this / period).toInt() * period
    }
}
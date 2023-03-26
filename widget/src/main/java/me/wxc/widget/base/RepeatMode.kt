package me.wxc.widget.base

import me.wxc.widget.tools.*
import java.util.*

sealed interface RepeatMode : java.io.Serializable {
    val repeatModeInt: Int
    val repeatInterval: Int
    val name: String
    fun repeatedModelValid(beginTime: Long, sourceBeginTime: Long): Boolean
    fun repeatBeginTimeByIndex(beginTime: Long, index: Int): Long

    object Never : RepeatMode {
        override val repeatModeInt: Int = 0
        override val repeatInterval: Int = 0
        override val name: String = "永不"
        override fun repeatedModelValid(beginTime: Long, sourceBeginTime: Long): Boolean {
            return false
        }

        override fun repeatBeginTimeByIndex(beginTime: Long, index: Int): Long {
            return beginTime
        }
    }

    object Day : RepeatMode {
        override val repeatModeInt: Int = 1
        override val repeatInterval: Int = 0
        override val name: String = "每天"
        override fun repeatedModelValid(beginTime: Long, sourceBeginTime: Long): Boolean {
            return true
        }

        override fun repeatBeginTimeByIndex(beginTime: Long, index: Int): Long {
            return beginTime + index * dayMillis
        }
    }

    object WorkDay : RepeatMode {
        override val repeatModeInt: Int = 2
        override val repeatInterval: Int = 0
        override val name: String = "工作日"
        override fun repeatedModelValid(beginTime: Long, sourceBeginTime: Long): Boolean {
            val weekday = beginOfDay(beginTime).get(Calendar.DAY_OF_WEEK)
            return (weekday >= Calendar.MONDAY && weekday <= Calendar.FRIDAY)
        }

        override fun repeatBeginTimeByIndex(beginTime: Long, index: Int): Long {
            return beginTime + index * dayMillis
        }
    }

    object Week : RepeatMode {
        override val repeatModeInt: Int = 3
        override val repeatInterval: Int = 0
        override val name: String = "每周"
        override fun repeatedModelValid(beginTime: Long, sourceBeginTime: Long): Boolean {
            return true
        }

        override fun repeatBeginTimeByIndex(beginTime: Long, index: Int): Long {
            return beginTime + index * 7 * dayMillis
        }
    }

    object Month : RepeatMode {
        override val repeatModeInt: Int = 4
        override val repeatInterval: Int = 0
        override val name: String = "每月"
        override fun repeatedModelValid(beginTime: Long, sourceBeginTime: Long): Boolean {
            return beginTime.dayOfMonth == sourceBeginTime.dayOfMonth
        }

        override fun repeatBeginTimeByIndex(beginTime: Long, index: Int): Long {
            return beginTime.calendar.apply {
                add(Calendar.MONTH, index)
            }.timeInMillis
        }
    }

    object Year : RepeatMode {
        override val repeatModeInt: Int = 5
        override val repeatInterval: Int = 0
        override val name: String = "每年"
        override fun repeatedModelValid(beginTime: Long, sourceBeginTime: Long): Boolean {
            return beginTime.dayOfMonth == sourceBeginTime.dayOfMonth && beginTime.monthOfYear == sourceBeginTime.monthOfYear
        }

        override fun repeatBeginTimeByIndex(beginTime: Long, index: Int): Long {
            return beginTime.calendar.apply {
                add(Calendar.YEAR, index)
            }.timeInMillis
        }
    }

    data class Custom(
        override val repeatInterval: Int,
        override val name: String = "自定义"
    ) : RepeatMode {
        override val repeatModeInt: Int = 6
        override fun repeatedModelValid(beginTime: Long, sourceBeginTime: Long): Boolean {
            return true
        }

        override fun repeatBeginTimeByIndex(beginTime: Long, index: Int): Long {
            return beginTime + index * repeatInterval * dayMillis
        }
    }

    companion object {
        val Pair<Int, Int>.parseLocalRepeatMode: RepeatMode
            get() = when (first) {
                1 -> Day
                2 -> WorkDay
                3 -> Week
                4 -> Month
                5 -> Year
                6 -> Custom(repeatInterval = second)
                else -> Never
            }
    }
}
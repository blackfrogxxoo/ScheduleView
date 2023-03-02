package me.wxc.widget.tools

import android.graphics.Point
import android.graphics.RectF
import me.wxc.widget.base.IScheduleComponent
import me.wxc.widget.base.IScheduleModel
import me.wxc.widget.schedule.ScheduleWidget
import java.util.*
import kotlin.math.roundToInt
import kotlin.math.roundToLong

val dayWidth: Float
    get() = if (ScheduleWidget.isThreeDay) (screenWidth - clockWidth) / 3 else (screenWidth - clockWidth)
val clockWidth = 56f.dp
val clockHeight = 50f.dp
val dayHeight = 50f.dp * 24
val dateLineHeight = 60f.dp
val canvasPadding = 10.dp
val zeroClockY = dateLineHeight + canvasPadding



fun IScheduleComponent<*>.refreshRect() {
    // x轴： 与当天的间隔天数 * 一天的宽度
    // y轴： 当前分钟数 / 一天的分钟数 * 一天的高度
    val today = System.currentTimeMillis().dDays
    val day = model.startTime.dDays
    val left = clockWidth + (day - today) * dayWidth
    val right = left + dayWidth
    val zeroClock = startOfDay(model.startTime)
    val top = dateLineHeight + dayHeight * (model.startTime - zeroClock.time.time) / (hourMillis * 24)
    val bottom = dateLineHeight + dayHeight * (model.endTime - zeroClock.time.time) / (hourMillis * 24)
    originRect.set(left, top, right, bottom)
}

fun IScheduleModel.originRect(): RectF {
    // x轴： 与当天的间隔天数 * 一天的宽度
    // y轴： 当前分钟数 / 一天的分钟数 * 一天的高度
    val today = System.currentTimeMillis().dDays
    val day = startTime.dDays
    val left = clockWidth + (day - today) * dayWidth
    val right = left + dayWidth
    val zeroClock = startOfDay(startTime)
    val top = dateLineHeight + dayHeight * (startTime - zeroClock.time.time) / (hourMillis * 24)
    val bottom = dateLineHeight + dayHeight * (endTime - zeroClock.time.time) / (hourMillis * 24)
    return RectF(left, top, right, bottom)
}

fun IScheduleComponent<*>.originRect(): RectF = model.originRect()

fun Point.positionToTime(scrollX: Int = 0, scrollY: Int = 0): Long {
    val days = ((x - clockWidth + scrollX) / dayWidth).roundToInt()
    val millis = ((y - dateLineHeight + scrollY) * hourMillis / clockHeight).roundToLong()
    val calendar = startOfDay().apply {
        add(Calendar.DAY_OF_YEAR, days)
    }
    return calendar.timeInMillis + millis
}

val Float.yToMillis: Long
    get() = (this * hourMillis / clockHeight).roundToLong()

val Float.xToDDays: Int
    get() = ((this - clockWidth) / dayWidth).roundToInt()

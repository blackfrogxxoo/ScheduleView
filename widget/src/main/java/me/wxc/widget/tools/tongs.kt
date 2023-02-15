package me.wxc.widget.tools

import android.app.Activity
import android.content.res.Resources
import android.graphics.Point
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import androidx.fragment.app.Fragment
import me.wxc.widget.ICalendarComponent
import me.wxc.widget.ICalendarModel
import java.util.*
import kotlin.math.roundToInt
import kotlin.math.roundToLong

val View.TAG: String
    get() = this::class.java.simpleName
val Activity.TAG: String
    get() = this::class.java.simpleName
val Fragment.TAG: String
    get() = this::class.java.simpleName

val Int.dp: Int
    get() = (Resources.getSystem().displayMetrics.density * this).roundToInt()
val Float.dp: Float
    get() = Resources.getSystem().displayMetrics.density * this
val screenWidth: Int
    get() = Resources.getSystem().displayMetrics.widthPixels
val screenHeight: Int
    get() = Resources.getSystem().displayMetrics.heightPixels

// TODO 抽象为ICalendarRender的属性
val clockWidth = 56f.dp
val clockHeight = 50f.dp
val dayHeight = 50f.dp * 24
val dateLineHeight = 60f.dp
val canvasPadding = 10.dp
val zeroClockY = dateLineHeight + canvasPadding

fun MotionEvent.ifInRect(rectF: RectF?, padding: Int = 0): Boolean {
    if (rectF == null) return false
    return x > rectF.left - padding && x < rectF.right + padding && y > rectF.top - padding && y < rectF.bottom + padding
}

fun MotionEvent.ifAtRectTop(rectF: RectF?, padding: Int = 0): Boolean {
    if (rectF == null) return false
    return x > rectF.left - padding && x < rectF.right + padding && y > rectF.top - 2 * padding && y < rectF.top + padding
}

fun MotionEvent.ifAtRectBottom(rectF: RectF?, padding: Int = 0): Boolean {
    if (rectF == null) return false
    return x > rectF.left - padding && x < rectF.right + padding && y > rectF.bottom - padding && y < rectF.bottom + 2 * padding
}

fun RectF.ifVisible(view: View): Boolean {
    return right >= view.left && left <= view.right && bottom >= view.top && top <= view.bottom
}

fun ICalendarComponent<*>.refreshRect() {
    // x轴： 与当天的间隔天数 * 一天的宽度
    // y轴： 当前分钟数 / 一天的分钟数 * 一天的高度
    val today = System.currentTimeMillis().dDays
    val day = model.startTime.dDays
    val left = clockWidth + (day - today) * dayWidth
    val right = left + dayWidth
    val zeroClock = startOfDay(model.startTime)
    val top = dateLineHeight + dayHeight * (model.startTime - zeroClock.time.time) / (hourMills * 24)
    val bottom = dateLineHeight + dayHeight * (model.endTime - zeroClock.time.time) / (hourMills * 24)
    originRect.set(left, top, right, bottom)
}

fun ICalendarModel.originRect(): RectF {
    // x轴： 与当天的间隔天数 * 一天的宽度
    // y轴： 当前分钟数 / 一天的分钟数 * 一天的高度
    val today = System.currentTimeMillis().dDays
    val day = startTime.dDays
    val left = clockWidth + (day - today) * dayWidth
    val right = left + dayWidth
    val zeroClock = startOfDay(startTime)
    val top = dateLineHeight + dayHeight * (startTime - zeroClock.time.time) / (hourMills * 24)
    val bottom = dateLineHeight + dayHeight * (endTime - zeroClock.time.time) / (hourMills * 24)
    return RectF(left, top, right, bottom)
}

fun ICalendarComponent<*>.originRect(): RectF = model.originRect()

fun Point.positionToTime(scrollX: Int = 0, scrollY: Int = 0): Long {
    val days = ((x - clockWidth + scrollX) / dayWidth).roundToInt()
    val mills = ((y - dateLineHeight + scrollY) * hourMills / clockHeight).roundToLong()
    val calendar = startOfDay().apply {
        add(Calendar.DAY_OF_YEAR, days)
    }
    return calendar.timeInMillis + mills
}

val Float.yToMills: Long
    get() = (this * hourMills / clockHeight).roundToLong()

val Float.xToDDays: Int
    get() = ((this - clockWidth) / dayWidth).roundToInt()

fun RectF.move(x: Int = 0, y: Int = 0) {
    left += x
    right += x
    top += y
    bottom += y
}

fun RectF?.topPoint(): Point? {
    return this?.run { Point(left.toInt(), top.toInt()) }
}

fun RectF?.bottomPoint(): Point? {
    return this?.run { Point(left.toInt(), bottom.toInt()) }
}
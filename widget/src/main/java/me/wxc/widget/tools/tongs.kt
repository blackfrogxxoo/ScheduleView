package me.wxc.widget.tools

import android.app.Activity
import android.content.res.Resources
import android.graphics.Point
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import androidx.fragment.app.Fragment
import me.wxc.widget.ICalendarComponent
import java.util.*
import kotlin.math.roundToInt

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

val clockWidth = 70f.dp
val clockHeight = 50f.dp
val dayHeight = 50f.dp * 24
val dayWidth = (screenWidth - clockWidth) / 3
val dayLineHeight = 80f.dp

fun MotionEvent.ifInRect(rectF: RectF?): Boolean {
    return x > (rectF?.left ?: -1f) && x < (rectF?.right ?: -1f) && y > (rectF?.top
        ?: -1f) && y < (rectF?.bottom ?: -1f)
}

fun RectF.ifVisible(view: View): Boolean {
    return right > view.left && left < view.right && bottom > view.top && top < view.bottom
}

fun ICalendarComponent<*>.originPosition(): Point {
    // x轴： 与当天的间隔天数 * 一天的宽度
    // y轴： 当前分钟数 / 一天的分钟数 * 一天的高度
    val today = System.currentTimeMillis().days
    val day = model.startTime.days
    val x = clockWidth + (day - today) * dayWidth
    val zeroClock = Calendar.getInstance().apply {
        time = Date(model.startTime)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val y = dayHeight * (model.startTime - zeroClock.time.time) / (hourMills * 24)
    return Point(x.toInt(), y.toInt())
}

fun ICalendarComponent<*>.originRect(): RectF {
    // x轴： 与当天的间隔天数 * 一天的宽度
    // y轴： 当前分钟数 / 一天的分钟数 * 一天的高度
    val today = System.currentTimeMillis().days
    val day = model.startTime.days
    val left = clockWidth + (day - today) * dayWidth
    val right = left + dayWidth
    val zeroClock = startOfDay(model.startTime)
    val top = dayLineHeight + dayHeight * (model.startTime - zeroClock.time.time) / (hourMills * 24)
    val bottom = dayLineHeight + dayHeight * (model.endTime - zeroClock.time.time) / (hourMills * 24)
    return RectF(left, top, right, bottom)
}
package me.wxc.widget.tools

import android.app.Activity
import android.content.res.Resources
import android.graphics.Point
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import androidx.fragment.app.Fragment
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
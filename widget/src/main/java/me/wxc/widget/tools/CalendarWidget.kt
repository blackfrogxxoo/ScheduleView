package me.wxc.widget.tools

import android.animation.ValueAnimator
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Scroller
import androidx.core.view.doOnLayout
import me.wxc.widget.ICalendarRender
import me.wxc.widget.ICalendarTaskCreator
import me.wxc.widget.ICalendarWidget
import me.wxc.widget.ICalendarWidget.Companion.TAG
import me.wxc.widget.components.CreateTaskComponent
import me.wxc.widget.components.DailyTaskComponent
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt

class CalendarWidget(override val render: ICalendarRender) : ICalendarWidget {
    private val MIN_SCROLL_Y = 0
    private val MAX_SCROLL_Y: Int
        get() = 50.dp * 24 + dateLineHeight.roundToInt() - (render as View).height + (render as View).paddingTop + (render as View).paddingBottom

    private var dDays = 0
    private var scrollX: Int = 0
        set(value) {
            field = value
            val days = (scroller.finalX / dayWidth).toInt()
            if (dDays != days) {
                onDateSelectedListener.invoke(startOfDay().apply {
                    add(Calendar.DAY_OF_YEAR, days)
                })
                dDays = days
            }
        }
    private var scrollY: Int = 0

    private val scroller: Scroller by lazy {
        Scroller((render as View).context, DecelerateInterpolator(), false)
    }
    private val velocityTracker by lazy {
        VelocityTracker.obtain()
    }
    private val gestureDetector by lazy { createGestureDetector() }

    private var scrollHorizontal = false

    init {
        render.widget = this
        (render as View).doOnLayout {
            scrollY = initializedY()
            onScroll(scrollX, scrollY)
        }
    }


    override var onDateSelectedListener: Calendar.() -> Unit = {}

    private var removingCreateTask = false

    private var downOnDateLine = false

    private fun createGestureDetector() =
        GestureDetector(
            (render as View).context,
            object : GestureDetector.SimpleOnGestureListener() {
                private var justDown = false
                override fun onDown(e: MotionEvent): Boolean {
                    justDown = true
                    if (!scroller.isFinished) {
                        scroller.abortAnimation()
                    }
                    return true
                }

                override fun onSingleTapUp(e: MotionEvent): Boolean {
                    Log.i(TAG, "onSingleTapUp: ${e.action}")
                    val clickedTask =
                        render.adapter.visibleComponents.mapNotNull { it as? DailyTaskComponent }
                            .find { e.ifInRect(it.drawingRect) }
                    clickedTask?.let {
                        (render as? ICalendarTaskCreator)?.onDailyTaskClickBlock?.invoke(it.model)
                        return true
                    }
                    val downOnBody = e.x > clockWidth && e.y > dateLineHeight
                    if (downOnBody && !removingCreateTask && (render as? ICalendarTaskCreator)?.addCreateTask(e) == true) {
                        onScroll(scrollX, scrollY)
                    }
                    return true
                }

                override fun onScroll(
                    e1: MotionEvent,
                    e2: MotionEvent,
                    distanceX: Float,
                    distanceY: Float
                ): Boolean {
                    if (justDown) {
                        scrollHorizontal = abs(distanceX) > abs(distanceY)
                    }
                    if (scrollHorizontal) {
                        scrollX += distanceX.toInt()
                        onScroll(scrollX, scrollY)
                    } else if (!downOnDateLine) {
                        scrollY += distanceY.toInt()
                        scrollY = scrollY.coerceAtMost(MAX_SCROLL_Y).coerceAtLeast(MIN_SCROLL_Y)
                        onScroll(scrollX, scrollY)
                    }
                    justDown = false
                    return true
                }
            })

    override fun onTouchEvent(motionEvent: MotionEvent): Boolean {
        velocityTracker.addMovement(motionEvent)
        val downOnCreate = createTaskComponent?.onTouchEvent(motionEvent) ?: false
        if (motionEvent.action == MotionEvent.ACTION_DOWN) {
            downOnDateLine = motionEvent.y < dateLineHeight
            removingCreateTask = false
            createTaskComponent?.let {
                if (!downOnCreate) {
                    (render as? ICalendarTaskCreator)?.removeCreateTask()
                    removingCreateTask = true
                }
            }
        } else if (motionEvent.action == MotionEvent.ACTION_UP) {
            if (!downOnCreate && !(downOnDateLine && !scrollHorizontal)) autoSnap()
            Log.i(TAG, "onTouchEvent: ${velocityTracker.xVelocity}")
        }
        if (downOnCreate) {
            onScroll(scrollX, scrollY)
        }
        return downOnCreate || gestureDetector.onTouchEvent(motionEvent)
    }

    override fun onScroll(x: Int, y: Int) {
        render.render(-x, -y)
    }

    private val createTaskComponent: CreateTaskComponent?
        get() = render.adapter.visibleComponents.find { it is CreateTaskComponent } as? CreateTaskComponent

    private fun autoSnap() {
        Log.i(TAG, "snapToPosition: ")
        velocityTracker.computeCurrentVelocity(1000)
        if (scrollHorizontal) {
            scroller.fling(
                scrollX,
                0,
                -velocityTracker.xVelocity.toInt(),
                0,
                Int.MIN_VALUE,
                Int.MAX_VALUE,
                0,
                0
            )
            // 自适应滑动结束位置
            scroller.finalX = ((scroller.finalX / dayWidth).roundToInt() * dayWidth).roundToInt()
        } else {
            scroller.fling(
                scrollX,
                scrollY,
                0,
                -velocityTracker.yVelocity.toInt(),
                Int.MIN_VALUE,
                Int.MAX_VALUE,
                Int.MIN_VALUE,
                Int.MAX_VALUE
            )
            scroller.finalX = ((scrollX / dayWidth).roundToInt() * dayWidth).roundToInt()
        }
        callOnScrolling(true)
    }

    private fun callOnScrolling(byFling: Boolean) {
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 10_000
            val startTime = System.currentTimeMillis()
            addUpdateListener {
                if (!scroller.computeScrollOffset()) {
                    Log.i(
                        TAG,
                        "canceled in ${System.currentTimeMillis() - startTime}, $scrollX"
                    )
                    cancel()
                    return@addUpdateListener
                }
                if (byFling) {
                    if (scrollHorizontal) {
                        scrollX = scroller.currX
                        onScroll(scrollX, scrollY)
                    } else {
                        scrollY = scroller.currY.coerceAtMost(MAX_SCROLL_Y)
                            .coerceAtLeast(MIN_SCROLL_Y)
                        scrollX = scroller.currX
                        onScroll(scrollX, scrollY)
                    }
                } else {
                    scrollX = scroller.currX
                    scrollY = scroller.currY.coerceAtMost(MAX_SCROLL_Y)
                        .coerceAtLeast(MIN_SCROLL_Y)
                    onScroll(scrollX, scrollY)
                }
            }
        }.start()
    }

    override fun scrollTo(x: Int, y: Int) {
        if (!scroller.isFinished) {
            scroller.abortAnimation()
        }
        scroller.startScroll(scrollX, scrollY, x - scrollX, y - scrollY)
        callOnScrolling(false)
    }

    override fun resetScrollState() {
        (render as? ICalendarTaskCreator)?.removeCreateTask()
        scrollTo(0, initializedY())
    }

    override fun isScrolling(): Boolean {
        return !scroller.isFinished
    }

    private fun initializedY() = run {
        val mills = System.currentTimeMillis() - startOfDay().timeInMillis
        val centerMills = ((render as View).height / 2 - zeroClockY / 2 - (render as View).paddingTop) * hourMills / clockHeight
        ((mills - centerMills) * clockHeight / hourMills).toInt().coerceAtMost(MAX_SCROLL_Y)
            .coerceAtLeast(MIN_SCROLL_Y)
    }

    companion object {
    }
}


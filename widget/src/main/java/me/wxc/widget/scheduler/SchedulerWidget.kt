package me.wxc.widget.scheduler

import android.animation.ValueAnimator
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Scroller
import androidx.core.view.doOnLayout
import kotlinx.coroutines.launch
import me.wxc.widget.SchedulerConfig
import me.wxc.widget.SchedulerConfig.lifecycleScope
import me.wxc.widget.base.ISchedulerRender
import me.wxc.widget.base.ISchedulerTaskCreator
import me.wxc.widget.base.ISchedulerWidget
import me.wxc.widget.base.ISchedulerWidget.Companion.TAG
import me.wxc.widget.scheduler.components.CreateTaskComponent
import me.wxc.widget.scheduler.components.DailyTaskComponent
import me.wxc.widget.tools.*
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.properties.Delegates

class SchedulerWidget(override val render: ISchedulerRender) : ISchedulerWidget {
    private val MIN_SCROLL_Y = 0
    private val MAX_SCROLL_Y: Int
        get() = dayHeight.roundToInt() + dateLineHeight.roundToInt() - (render as View).height + (render as View).paddingTop + (render as View).paddingBottom
    private val MIN_SCROLL_X: Int
        get() = (dayWidth * (SchedulerConfig.schedulerStartTime.dDays - System.currentTimeMillis().dDays)).roundToInt()
    private val MAX_SCROLL_X: Int
        get() = (dayWidth * (SchedulerConfig.schedulerEndTime.dDays - System.currentTimeMillis().dDays)).roundToInt()

    override var renderRange: ISchedulerWidget.RenderRange by Delegates.observable(ISchedulerWidget.RenderRange.ThreeDayRange) { _, _, value ->
        val temp = isThreeDay
        isThreeDay = value is ISchedulerWidget.RenderRange.ThreeDayRange
        if (temp && !isThreeDay) { // 三日 -> 单日
            scrollX *= 3
        } else if (!temp && isThreeDay) { // 单日 -> 三日
            scrollX /= 3
        }
        render.adapter.notifyModelsChanged()
        scrollTo(scrollX, scrollY)
    }

    override var selectedDayTime: Long
        set(value) {
            (render as? ISchedulerTaskCreator)?.removeCreateTask()
            scrollTo((dayWidth * value.dDays).roundToInt(), initializedY())
        }
        get() = startOfDay().timeInMillis + dDays * dayMillis

    private val dDays: Int
        get() = (scrollX / dayWidth).toInt()
    private var scrollX: Int = 0
        set(value) {
            field = value
            if (scroller.isFinished) {
                SchedulerConfig.onDateSelectedListener.invoke(startOfDay(selectedDayTime))
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
        isThreeDay = renderRange is ISchedulerWidget.RenderRange.ThreeDayRange
        render.widget = this
        (render as View).doOnLayout {
            scrollY = initializedY()
            onScroll(scrollX, scrollY)
        }
        lifecycleScope.launch {
            render.adapter.models.addAll(
                SchedulerConfig.schedulerModelsProvider.invoke(
                    SchedulerConfig.schedulerStartTime,
                    SchedulerConfig.schedulerEndTime
                )
            )
            Log.i(TAG, "notify models changed")
            render.adapter.notifyModelsChanged()
            render.invalidate()
        }
    }

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
                        SchedulerConfig.onDailyTaskClickBlock(it.model)
                        return true
                    }
                    val downOnBody = e.x > clockWidth && e.y > dateLineHeight
                    if (downOnBody
                        && !removingCreateTask
                        && (render as? ISchedulerTaskCreator)?.addCreateTask(e) == true
                    ) {
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
                        scrollX = scrollX.coerceAtMost(MAX_SCROLL_X).coerceAtLeast(MIN_SCROLL_X)
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
                    (render as? ISchedulerTaskCreator)?.removeCreateTask()
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
            // 自适应滑动结束位置
            if (isThreeDay) { // 三日视图，正常滑动距离
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
                scroller.finalX =
                    ((scroller.finalX / dayWidth).roundToInt() * dayWidth).roundToInt()
                        .coerceAtMost(MAX_SCROLL_X).coerceAtLeast(MIN_SCROLL_X)
            } else { // 单日视图，滑动一页
                scroller.fling(
                    scrollX,
                    0,
                    -velocityTracker.xVelocity.toInt().coerceAtMost(200).coerceAtLeast(-200),
                    0,
                    Int.MIN_VALUE,
                    Int.MAX_VALUE,
                    0,
                    0
                )
                val dest = scroller.finalX
                if (dest > scrollX) {
                    scroller.finalX =
                        (((scrollX / dayWidth).roundToInt() * dayWidth).roundToInt() + dayWidth.roundToInt()).coerceAtMost(
                            MAX_SCROLL_X
                        ).coerceAtLeast(MIN_SCROLL_X)
                } else if (dest < scrollX) {
                    scroller.finalX =
                        (((scrollX / dayWidth).roundToInt() * dayWidth).roundToInt() - dayWidth.roundToInt()).coerceAtMost(
                            MAX_SCROLL_X
                        ).coerceAtLeast(MIN_SCROLL_X)
                } else {
                    scroller.finalX = ((scrollX / dayWidth).roundToInt() * dayWidth).roundToInt()
                        .coerceAtMost(MAX_SCROLL_X).coerceAtLeast(MIN_SCROLL_X)
                }
            }
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
                .coerceAtMost(MAX_SCROLL_X).coerceAtLeast(MIN_SCROLL_X)
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
                        scrollY =
                            scroller.currY.coerceAtMost(MAX_SCROLL_Y).coerceAtLeast(MIN_SCROLL_Y)
                        scrollX =
                            scroller.currX.coerceAtMost(MAX_SCROLL_X).coerceAtLeast(MIN_SCROLL_X)
                        onScroll(scrollX, scrollY)
                    }
                } else {
                    scrollX = scroller.currX.coerceAtMost(MAX_SCROLL_X).coerceAtLeast(MIN_SCROLL_X)
                    scrollY = scroller.currY.coerceAtMost(MAX_SCROLL_Y).coerceAtLeast(MIN_SCROLL_Y)
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

    override fun isScrolling(): Boolean {
        return !scroller.isFinished
    }

    private fun initializedY() = run {
        val millis = System.currentTimeMillis() - startOfDay().timeInMillis
        val centerMillis =
            ((render as View).height / 2 - zeroClockY / 2 - (render as View).paddingTop) * hourMillis / clockHeight
        ((millis - centerMillis) * clockHeight / hourMillis).toInt().coerceAtMost(MAX_SCROLL_Y)
            .coerceAtLeast(MIN_SCROLL_Y)
    }

    companion object {
        var isThreeDay = false
    }
}


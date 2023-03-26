package me.wxc.widget.schedule

import android.animation.ValueAnimator
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Scroller
import androidx.core.animation.doOnCancel
import androidx.core.view.doOnLayout
import me.wxc.widget.ScheduleConfig
import me.wxc.widget.base.*
import me.wxc.widget.schedule.components.DailyTaskComponent
import me.wxc.widget.tools.*
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class ScheduleWidget(override val render: IScheduleRender) : IScheduleWidget {
    private val MIN_SCROLL_Y = 0
    private val MAX_SCROLL_Y: Int
        get() = dayHeight.roundToInt() + dateLineHeight.roundToInt() - (render as View).height + (render as View).paddingTop + (render as View).paddingBottom
    private val MIN_SCROLL_X: Int
        get() = (dayWidth * (ScheduleConfig.scheduleBeginTime.dDays - nowMillis.dDays)).roundToInt()
    private val MAX_SCROLL_X: Int
        get() = (dayWidth * (ScheduleConfig.scheduleEndTime.dDays - nowMillis.dDays)).roundToInt()

    override var renderRange: IScheduleWidget.RenderRange by setter(IScheduleWidget.RenderRange.ThreeDayRange) { _, value ->
        isThreeDay = value is IScheduleWidget.RenderRange.ThreeDayRange
        render.adapter.notifyModelsChanged()
        scrollTo((selectedDayTime.dDays * dayWidth).roundToInt(), scrollY, 0)
    }

    override var selectedDayTime: Long by setter(nowMillis) { oldTime, time ->
        if (!scroller.isFinished || byDrag) {
            return@setter
        }
        val isNow = time % beginOfDay(time).timeInMillis != 0L
        if (isNow) {
            render.finishEditing()
            scrollTo((dayWidth * time.dDays).roundToInt(), initializedY(time))
            return@setter
        }
        if (time.dDays != oldTime.dDays) {
            scrollTo((dayWidth * time.dDays).roundToInt(), scrollY)
        }
    }

    override var focusedDayTime: Long by setter(-1) { _, time ->
    }

    override var scheduleModels: List<IScheduleModel> by setter(listOf()) { _, list ->
        render.adapter.models.clear()
        render.adapter.models.addAll(list)
        render.adapter.notifyModelsChanged()
        (render as? View)?.invalidate()
        if (loadingMore) {
            loadingMore = false
        }
    }

    override var beginTime: Long by setter(nowMillis) { _, time ->
        Log.i(TAG, "update beginTime: ${time.yyyyMMddHHmmss}")
    }
    override var endTime: Long by setter(nowMillis) { _, time ->
        Log.i(TAG, "update endTime: ${time.yyyyMMddHHmmss}")
    }

    private var scrollX: Int = 0
    private var scrollY: Int = 0

    private val singleDayScroller: Scroller by lazy {
        Scroller((render as View).context) { ot ->
            var t = ot
            t -= 1.0f
            t * t * t * t * t + 1.0f
        }
    }
    private val threeDayScroller: Scroller by lazy {
        Scroller((render as View).context, DecelerateInterpolator(), false)
    }
    private val scroller: Scroller
        get() = singleDayScroller

    //        get() = if (isThreeDay) threeDayScroller else singleDayScroller
    private val velocityTracker by lazy {
        VelocityTracker.obtain()
    }
    private val gestureDetector by lazy { createGestureDetector() }

    private var scrollHorizontal = false

    private var byDrag = false

    init {
        isThreeDay = renderRange is IScheduleWidget.RenderRange.ThreeDayRange
        render.widget = this
        (render as View).doOnLayout {
            scrollY = initializedY()
            onScroll(scrollX, scrollY)
        }
        beginTime = beginOfDay().apply { add(Calendar.MONTH, -1) }.timeInMillis
        endTime = beginOfDay(beginTime).apply { add(Calendar.MONTH, 2) }.timeInMillis
        beginTime = beginTime
        endTime = endTime
        reloadSchedulesFromProvider()
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
                    if (e.y < dateLineHeight) return true
                    render.adapter.visibleComponents.mapNotNull { it as? DailyTaskComponent }
                        .findLast { e.ifInRect(it.drawingRect) }?.let {
                            if (it.model.editingTaskModel == null) {
                                ScheduleConfig.onDailyTaskClickBlock(it.model)
                            }
                            return true
                        }
                    val downOnBody = e.x > clockWidth && e.y > dateLineHeight
                    if (downOnBody
                        && !removingCreateTask
                        && render.startEditingTask(e)
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

                override fun onLongPress(e: MotionEvent) {
                    render.adapter.visibleComponents.mapNotNull { it as? DailyTaskComponent }
                        .findLast { e.ifInRect(it.drawingRect) }
                        ?.model?.let {
                            render.startEditingTask(e, it)
                            render.invalidate()
                        }
                }
            })

    override fun onTouchEvent(motionEvent: MotionEvent): Boolean {
        byDrag = motionEvent.action != MotionEvent.ACTION_UP
        velocityTracker.addMovement(motionEvent)
        val downOnCreate = createTaskComponent?.onTouchEvent(motionEvent) ?: false
        if (motionEvent.action == MotionEvent.ACTION_DOWN) {
            downOnDateLine = motionEvent.y < dateLineHeight
            removingCreateTask = false
            createTaskComponent?.let {
                if (!downOnCreate) {
                    render.finishEditing()
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

    private var loadingMore = false
    override fun onScroll(x: Int, y: Int) {
        render.render(-x, -y)
        val startDay = -(-x / dayWidth).toInt() - 1
        val endDay = startDay + 1 + (screenWidth / dayWidth).toInt()
        if (startDay < beginTime.dDays + 15) {
            beginTime = min(beginTime.calendar.apply {
                add(Calendar.MONTH, -1)
            }.timeInMillis.apply {
                Log.i(TAG, "onScroll: load previous month: $yyyyM")
            }, beginOfDay().timeInMillis + startDay * dayMillis)
            if (!loadingMore) {
                loadingMore = true
                reloadSchedulesFromProvider()
            }
        } else if (endDay > endTime.dDays - 15) {
            endTime = max(endTime.calendar.apply {
                add(Calendar.MONTH, 1)
            }.timeInMillis.apply {
                Log.i(TAG, "onScroll: load next month: $yyyyM")
            }, beginOfDay().timeInMillis + endDay * dayMillis)
            if (!loadingMore) {
                loadingMore = true
                reloadSchedulesFromProvider()
            }
        }
        val dDays = (scrollX / dayWidth).roundToInt()
        ScheduleConfig.onDateSelectedListener.invoke((beginOfDay().timeInMillis + dDays * dayMillis).calendar)
    }

    private val createTaskComponent: DailyTaskComponent?
        get() = render.adapter.editingTaskComponent

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
                Log.i(TAG, "autoSnap: ${velocityTracker.xVelocity.toInt()}")
                val velocity = velocityTracker.xVelocity.toInt()
                if (!scroller.isFinished) {
                    scroller.abortAnimation()
                }
                val currentDDays = selectedDayTime.dDays.toInt()
                val destDDays = if (velocity < -1000) { // 左滑
                    currentDDays + 1
                } else if (velocity > 1000) { // 右滑
                    currentDDays - 1
                } else if (scrollX / dayWidth.roundToInt() == currentDDays && scrollX % dayWidth.roundToInt() > dayWidth / 2) {
                    currentDDays + 1
                } else if (scrollX / dayWidth.roundToInt() == currentDDays - 1 && scrollX % dayWidth.roundToInt() < dayWidth / 2) {
                    currentDDays - 1
                } else {
                    currentDDays
                }
                val dx = (destDDays * dayWidth).roundToInt() - scrollX
                scroller.startScroll(
                    scrollX,
                    scrollY,
                    dx,
                    0,
                    (abs(dx) - abs(velocity) / 100).coerceAtMost(400).coerceAtLeast(50)
                )
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
        callOnScrolling(true, true)
    }

    private fun callOnScrolling(byFling: Boolean, yUpdated: Boolean) {
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 10_000
            val beginTime = nowMillis
            doOnCancel {
                notifySelectedDayTime()
            }
            addUpdateListener {
                if (!scroller.computeScrollOffset()) {
                    Log.i(
                        TAG,
                        "canceled in ${nowMillis - beginTime}, $scrollX"
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
                    if (yUpdated) {
                        scrollY =
                            scroller.currY.coerceAtMost(MAX_SCROLL_Y).coerceAtLeast(MIN_SCROLL_Y)
                    }
                    onScroll(scrollX, scrollY)
                }
            }
        }.start()
    }

    override fun scrollTo(x: Int, y: Int, duration: Int) {
        if (!scroller.isFinished) {
            scroller.abortAnimation()
        }
        if (x == scrollX && y == scrollY) {
            (render as View).invalidate()
            return
        }
        val yUpdated = y != scrollY
        scroller.startScroll(scrollX, scrollY, x - scrollX, y - scrollY, duration)
        callOnScrolling(false, yUpdated)
    }

    override fun isScrolling(): Boolean {
        return !scroller.isFinished
    }

    override val parentRender: ICalendarRender?
        get() = (render as? View)?.parent as? ICalendarRender
    override val calendar: Calendar = beginOfDay()

    private fun initializedY(time: Long = nowMillis) = run {
        val millis = time - beginOfDay(time).timeInMillis
        val centerMillis =
            ((render as View).height / 2 - zeroClockY / 2 - (render as View).paddingTop) * hourMillis / clockHeight
        ((millis - centerMillis) * clockHeight / hourMillis).toInt().coerceAtMost(MAX_SCROLL_Y)
            .coerceAtLeast(MIN_SCROLL_Y)
    }

    private fun notifySelectedDayTime() {
        val dDays = (scrollX / dayWidth).roundToInt()
        if (rootCalendarRender?.selectedDayTime?.dDays?.toInt() != dDays) {
            rootCalendarRender?.selectedDayTime =
                beginOfDay().timeInMillis + dDays * dayMillis
            ScheduleConfig.onDateSelectedListener.invoke(beginOfDay(selectedDayTime))
        }
    }

    override fun reloadSchedulesFromProvider(onReload: () -> Unit) {
        super.reloadSchedulesFromProvider(onReload)
        render.finishEditing()
    }

    companion object {
        private const val TAG = "ScheduleWidget"
        var isThreeDay = true
    }
}


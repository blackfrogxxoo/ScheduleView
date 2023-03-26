package me.wxc.widget.flow

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.view.children
import me.wxc.widget.R
import me.wxc.widget.ScheduleConfig
import me.wxc.widget.base.CalendarMode
import me.wxc.widget.base.ICalendarParent
import me.wxc.widget.base.ICalendarRender
import me.wxc.widget.base.IScheduleModel
import me.wxc.widget.flow.header.FlowHeaderGroup
import me.wxc.widget.flow.list.ScheduleFlowView
import me.wxc.widget.tools.*
import java.util.*
import kotlin.math.abs

class FlowContainer @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs), ICalendarRender, ICalendarParent {
    private val flowHeader: FlowHeaderGroup
    private val flowHeaderArrow: ImageView
    private val scheduleList: ScheduleFlowView

    override val parentRender: ICalendarRender? = null
    override val calendar: Calendar = beginOfDay()

    override var scheduleModels: List<IScheduleModel> by setter(listOf())
    override val beginTime: Long = ScheduleConfig.scheduleBeginTime
    override val endTime: Long = ScheduleConfig.scheduleBeginTime

    override var focusedDayTime: Long by setter(-1) { _, time ->
        if (time > 0) {
            selectedDayTime = time
        }
        childRenders.forEach { it.focusedDayTime = time }
    }
    override var selectedDayTime: Long by setter(-1) { _, time ->
        ScheduleConfig.onDateSelectedListener.invoke(time.calendar)
        childRenders.forEach { it.selectedDayTime = time }
    }

    override val childRenders: List<ICalendarRender>
        get() = children.filterIsInstance<ICalendarRender>().toList()

    private var downX: Float = 0f
    private var downY: Float = 0f
    private var justDown: Boolean = false
    private var downTimestamp: Long = 0
    private val touchSlop = ViewConfiguration.getTouchSlop()
    private var intercept = false
    private var fromMonthMode = false

    private val velocityTracker by lazy {
        VelocityTracker.obtain()
    }

    init {
        inflate(context, R.layout.flow_container, this)
        flowHeader = findViewById(R.id.flowHeader)
        flowHeaderArrow = findViewById(R.id.flowHeaderArrow)
        scheduleList = findViewById(R.id.scheduleList)
        flowHeaderArrow.setOnClickListener {
            if (flowHeader.calendarMode is CalendarMode.MonthMode) {
                flowHeaderArrow.rotation = 180f
                flowHeader.autoSwitchMode(CalendarMode.WeekMode)
            } else {
                flowHeaderArrow.rotation = 0f
                flowHeader.autoSwitchMode(CalendarMode.MonthMode(1f))
            }
        }
        val beginTime = beginOfDay().apply { add(Calendar.MONTH, -6) }.timeInMillis
        val endTime = beginOfDay(beginTime).apply { add(Calendar.MONTH, 12) }.timeInMillis
        scheduleList.beginTime = beginTime
        scheduleList.endTime = endTime
        scheduleList.reloadSchedulesFromProvider {
            scheduleList.selectedDayTime = beginOfDay().timeInMillis
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        Log.i(TAG, "onTouchEvent: $intercept")
        return performInterceptTouchEvent(event) || super.onTouchEvent(event)
    }

    override fun reloadSchedulesFromProvider(onReload: () -> Unit) {
        childRenders.forEach { it.reloadSchedulesFromProvider(onReload) }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        val intercept = performInterceptTouchEvent(ev) || super.onInterceptTouchEvent(ev)
        Log.i(TAG, "onInterceptTouchEvent: ${ev.action}, $intercept")
        requestDisallowInterceptTouchEvent(false)
        return intercept
    }

    private val headerBottom: Int
        get() = (flowHeaderArrow.parent as View).bottom

    private fun performInterceptTouchEvent(ev: MotionEvent): Boolean {
        velocityTracker.addMovement(ev)
        return when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.x
                downY = ev.y
                justDown = true
                downTimestamp = nowMillis
                false
            }
            MotionEvent.ACTION_MOVE -> {
                if (justDown) {
                    fromMonthMode = flowHeader.calendarMode is CalendarMode.MonthMode
                }
                if (justDown && (abs(downX - ev.x) > touchSlop || abs(downY - ev.y) > touchSlop)) {
                    val moveUp = abs(downX - ev.x) < abs(downY - ev.y) && ev.y < downY
                    val moveDown = abs(downX - ev.x) < abs(downY - ev.y) && ev.y > downY
                    intercept = (moveUp && flowHeader.calendarMode is CalendarMode.MonthMode)
                            || (moveDown && downY < headerBottom && flowHeader.calendarMode is CalendarMode.WeekMode)
                            || (moveDown && downY > headerBottom && flowHeader.calendarMode is CalendarMode.MonthMode)
                    justDown = false
                    Log.i(
                        TAG,
                        "onInterceptTouchEvent intercept: $moveUp ${flowHeader.calendarMode}"
                    )
                }
                if (intercept) {
                    if (!fromMonthMode && flowHeader.calendarMode is CalendarMode.WeekMode) {
                        flowHeader.calendarMode = CalendarMode.MonthMode(0f)
                    }
                    val maxHeight = (6 * flowHeaderDayHeight)
                    if (fromMonthMode) {
                        flowHeader.calendarMode = CalendarMode.MonthMode(
                            expandFraction = ((maxHeight - downY + ev.y) / maxHeight).apply {
                                Log.i(TAG, "performInterceptTouchEvent: $this")
                            }.coerceAtLeast(
                                0f
                            ).coerceAtMost(1f),
                        )

                        Log.i(
                            TAG,
                            "onInterceptTouchEvent fraction: ${flowHeader.calendarMode}"
                        )
                    } else {
                        flowHeader.calendarMode = CalendarMode.MonthMode(
                            expandFraction = ((flowHeaderDayHeight - downY + ev.y) / maxHeight).coerceAtLeast(
                                0f
                            ).coerceAtMost(1f),
                        )

                        Log.i(
                            TAG,
                            "onInterceptTouchEvent fraction: ${flowHeader.calendarMode}"
                        )
                    }
                    true
                } else {
                    false
                }
            }
            MotionEvent.ACTION_UP -> {
                velocityTracker.computeCurrentVelocity(1000)
                val velocity = velocityTracker.yVelocity.apply {
                    Log.i(TAG, "velocity: $this")
                }
                if (intercept && flowHeader.calendarMode is CalendarMode.MonthMode) {
                    val target = if (velocity < -1000) {
                        CalendarMode.WeekMode
                    } else if (velocity > 1000) {
                        CalendarMode.MonthMode(1f)
                    } else if ((flowHeader.calendarMode as CalendarMode.MonthMode).expandFraction < 0.5f) {
                        CalendarMode.WeekMode
                    } else {
                        CalendarMode.MonthMode(1f)
                    }
                    flowHeader.autoSwitchMode(target.apply {
                        flowHeaderArrow.rotation = if (this is CalendarMode.MonthMode) {
                            0f
                        } else {
                            180f
                        }
                    })
                }
                intercept = false
                false
            }
            else -> {
                false
            }
        }
    }

    companion object {
        private const val TAG = "FlowContainer"
    }
}
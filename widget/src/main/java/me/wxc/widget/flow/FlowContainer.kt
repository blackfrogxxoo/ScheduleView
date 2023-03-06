package me.wxc.widget.flow

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.ViewConfiguration
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import me.wxc.widget.R
import me.wxc.widget.base.CalendarMode
import me.wxc.widget.tools.TAG
import kotlin.math.abs

class FlowContainer @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {
    private val flowHeader: FlowHeaderGroup
    private val flowHeaderArrow: ImageView
    private val scheduleList: RecyclerView

    private var downX: Float = 0f
    private var downY: Float = 0f
    private var justDown: Boolean = false
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
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return performInterceptTouchEvent(event)
    }

    private fun performInterceptTouchEvent(ev: MotionEvent): Boolean {
        velocityTracker.addMovement(ev)
        return when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.x
                downY = ev.y
                justDown = true
                true
            }
            MotionEvent.ACTION_MOVE -> {
                if (justDown && (abs(downX - ev.x) > touchSlop || abs(downY - ev.y) > touchSlop)) {
                    val moveUp = abs(downX - ev.x) < abs(downY - ev.y) && ev.y < downY
                    val moveDown = abs(downX - ev.x) < abs(downY - ev.y) && ev.y > downY
                    intercept = (moveUp && flowHeader.calendarMode is CalendarMode.MonthMode)
                            || (moveDown && flowHeader.calendarMode is CalendarMode.WeekMode)
                    fromMonthMode = intercept && flowHeader.calendarMode is CalendarMode.MonthMode
                    justDown = false
                    Log.i(
                        TAG,
                        "onInterceptTouchEvent intercept: $moveUp ${flowHeader.calendarMode}"
                    )
                }
                if (intercept) {
                    if (!fromMonthMode && flowHeader.calendarMode is CalendarMode.WeekMode) {
                        flowHeader.calendarMode = CalendarMode.MonthMode(0f, touching = true)
                    }
                    val maxHeight = (6 * flowHeaderDayHeight)
                    if (fromMonthMode) {
                        flowHeader.calendarMode =
                            (flowHeader.calendarMode as CalendarMode.MonthMode).copy(
                                expandFraction = ((maxHeight - downY + ev.y) / maxHeight).coerceAtLeast(
                                    0f
                                ).coerceAtMost(1f),
                                touching = true
                            )

                        Log.i(
                            TAG,
                            "onInterceptTouchEvent fraction: ${flowHeader.calendarMode}"
                        )
                    } else {
                        flowHeader.calendarMode =
                            (flowHeader.calendarMode as CalendarMode.MonthMode).copy(
                                expandFraction = ((flowHeaderDayHeight - downY + ev.y) / maxHeight).coerceAtLeast(
                                    0f
                                ).coerceAtMost(1f),
                                touching = true
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
                if (flowHeader.calendarMode is CalendarMode.MonthMode) { // TODO 根据速度方向处理
                    flowHeader.calendarMode =
                        (flowHeader.calendarMode as CalendarMode.MonthMode).copy(touching = false)
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

}
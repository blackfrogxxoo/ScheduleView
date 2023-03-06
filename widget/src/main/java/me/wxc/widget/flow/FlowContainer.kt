package me.wxc.widget.flow

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
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

        setOnTouchListener { view, ev ->
            when (ev.action) {
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
                    if (flowHeader.calendarMode is CalendarMode.MonthMode) { // TODO 根据速度方向处理
                        flowHeader.calendarMode = (flowHeader.calendarMode as CalendarMode.MonthMode).copy(touching = false)
                        if ((flowHeader.calendarMode as CalendarMode.MonthMode).expandFraction < 0.5f) {
                            flowHeader.autoSwitchMode(CalendarMode.WeekMode)
                            flowHeaderArrow.rotation = 180f
                        } else {
                            flowHeader.autoSwitchMode(CalendarMode.MonthMode(1f))
                            flowHeaderArrow.rotation = 0f
                        }
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

//    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
//        when (ev.action) {
//            MotionEvent.ACTION_DOWN -> {
//                downX = ev.x
//                downY = ev.y
//                justDown = true
//            }
//            MotionEvent.ACTION_MOVE -> {
//                if (justDown && (abs(downX - ev.x) > touchSlop || abs(downY - ev.y) > touchSlop)) {
//                    val moveUp = abs(downX - ev.x) < abs(downY - ev.y) && ev.y < downY
//                    intercept = moveUp && flowHeader.calendarMode == CalendarMode.MonthMode
//                    justDown = false
//                    Log.i(TAG, "onInterceptTouchEvent intercept: $moveUp ${flowHeader.calendarMode}")
//                }
//                if (intercept) {
//                    val maxHeight = (6 * flowHeaderDayHeight)
//                    flowHeader.expandFraction = ((maxHeight - downY + ev.y) / maxHeight).coerceAtLeast(0f).coerceAtMost(1f)
//                    Log.i(TAG, "onInterceptTouchEvent fraction: ${flowHeader.expandFraction}")
//                    if (flowHeader.expandFraction == 0f) {
//                        flowHeader.calendarMode = CalendarMode.WeekMode
//                        flowHeaderArrow.rotation = 180f
//                    }
//                }
//            }
//            MotionEvent.ACTION_UP -> {
//                if (flowHeader.expandFraction != 0f && flowHeader.expandFraction != 1f) {
//                    flowHeader.calendarMode = CalendarMode.WeekMode
//                    flowHeaderArrow.rotation = 180f
//                }
//                intercept = false
//            }
//        }
//        return super.onInterceptTouchEvent(ev)
//    }

}
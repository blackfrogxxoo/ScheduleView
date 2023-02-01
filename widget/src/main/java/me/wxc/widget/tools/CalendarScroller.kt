package me.wxc.widget.tools

import android.animation.ValueAnimator
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Scroller
import me.wxc.widget.ICalendarEditable
import me.wxc.widget.ICalendarRender
import me.wxc.widget.ICalendarScroller
import me.wxc.widget.ICalendarScroller.Companion.TAG
import me.wxc.widget.components.CreateTaskModel
import kotlin.math.abs

class CalendarScroller(override var render: ICalendarRender) : ICalendarScroller {
    private val MIN_SCROLL_Y = 0
    private val MAX_SCROLL_Y: Int
        get() = 50.dp * 24 - (render as View).height + (render as View).paddingTop + (render as View).paddingBottom

    private var scrollX: Int = 0
    private var scrollY: Int = 0
    private val scroller: Scroller =
        Scroller((render as View).context, DecelerateInterpolator(), false)
    private val gestureDetector = GestureDetector(
        (render as View).context,
        object : GestureDetector.SimpleOnGestureListener() {
            private var justDown = false
            private var scrollHorizontal = false
            private var downOnCreate = false
            override fun onDown(e: MotionEvent): Boolean {
                justDown = true
                downOnCreate =
                    e.ifInRect(render.adapter.visibleComponents.find { it is ICalendarEditable }?.rect)
                if (!downOnCreate && render.adapter.models.any { it is CreateTaskModel }) {
                    render.adapter.models.removeIf { it is CreateTaskModel }
                    render.adapter.notifyDataChanged()
                }
                if (!scroller.isFinished) {
                    scroller.abortAnimation()
                }
                return true
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                Log.i(TAG, "onSingleTapConfirmed: ")
                return true
            }

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                Log.i(TAG, "onSingleTapUp: ${e.action}")
                if (!downOnCreate && render.adapter.visibleComponents.none { it is ICalendarEditable }) {
                    render.adapter.models.add(
                        CreateTaskModel(
                            startTime = System.currentTimeMillis() - 5 * hourMills,
                            duration = hourMills / 2,
                            title = "新建日程"
                        )
                    )
                    render.adapter.notifyDataChanged()
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
                if (downOnCreate) {
                    Log.i(
                        TAG,
                        "onScroll: downOnCreate $distanceX, $distanceY ${e1.action} ${e2.action}"
                    )
                    return true
                }
                if (justDown) {
                    scrollHorizontal = abs(distanceX) > abs(distanceY)
                }
                if (scrollHorizontal) {
                    scrollX += distanceX.toInt()
                    onScroll(scrollX, scrollY)
                } else {
                    scrollY += distanceY.toInt()
                    scrollY = scrollY.coerceAtMost(MAX_SCROLL_Y).coerceAtLeast(MIN_SCROLL_Y)
                    onScroll(scrollX, scrollY)
                }
                justDown = false
                return true
            }

            override fun onFling(
                e1: MotionEvent,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (downOnCreate) {
                    Log.i(TAG, "onFling: downOnCreate")
                    return true
                }
                val velocity = if (scrollHorizontal) velocityX else velocityY
                Log.i(TAG, "startFling $velocity")
                if (scrollHorizontal) {
                    scroller.fling(
                        scrollX,
                        0,
                        -velocityX.toInt(),
                        0,
                        Int.MIN_VALUE,
                        Int.MAX_VALUE,
                        0,
                        0
                    )
                    Log.i(TAG, "compute origin: ${scroller.finalX}")
                    scroller.finalX = scroller.finalX / (dayWidth.toInt()) * (dayWidth.toInt())
                    Log.i(TAG, "compute adapted: ${scroller.finalX}")
                } else {
                    scroller.fling(
                        0,
                        scrollY,
                        0,
                        -velocityY.toInt(),
                        0,
                        0,
                        Int.MIN_VALUE,
                        Int.MAX_VALUE
                    )
                }
                ValueAnimator.ofFloat(0f, 1f).apply {
                    duration = 10_000
                    val startTime = System.currentTimeMillis()
                    addUpdateListener {
                        if (scrollHorizontal) {
                            scrollX = scroller.currX
                            onScroll(scrollX, scrollY)
                        } else {
                            scrollY = scroller.currY.coerceAtMost(MAX_SCROLL_Y)
                                .coerceAtLeast(MIN_SCROLL_Y)
                            onScroll(scrollX, scrollY)
                        }
                        if (!scroller.computeScrollOffset()) {
                            Log.i(
                                TAG,
                                "canceled in ${System.currentTimeMillis() - startTime}, $scrollX"
                            )
                            cancel()
                            return@addUpdateListener
                        }
                    }
                }.start()
                return true
            }
        })

    init {
        (render as View).setOnTouchListener { view, motionEvent ->
            gestureDetector.onTouchEvent(motionEvent)
        }
    }

    override fun onScroll(x: Int, y: Int) {
        render.render(-x, -y)
    }

    override fun snapToPosition() {

    }

    companion object {
    }
}
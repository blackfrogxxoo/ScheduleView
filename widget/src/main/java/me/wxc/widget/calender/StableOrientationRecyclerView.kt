package me.wxc.widget.calender

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs

/**
 * 滑动过程中方向不变，不会因为滑动到顶后分发给父View
 */
open class StableOrientationRecyclerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : RecyclerView(context, attrs) {
    private var downX = 0f
    private var downY = 0f
    private var justDown = false
    private val isHorizontal: Boolean
        get() = (layoutManager as? LinearLayoutManager)?.orientation == HORIZONTAL


    private val touchSlop = ViewConfiguration.getTouchSlop()

    override fun dispatchTouchEvent(e: MotionEvent): Boolean {
        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = e.x
                downY = e.y
                justDown = true
            }
            MotionEvent.ACTION_MOVE -> {
                if (justDown && (abs(downX - e.x) > touchSlop || abs(downY - e.y) > touchSlop)) {
                    val moveHorizontal = abs(downX - e.x) > abs(downY - e.y)
                    if (moveHorizontal == isHorizontal) {
                        parent.requestDisallowInterceptTouchEvent(true)
                    }
                    justDown = false
                }
            }
            MotionEvent.ACTION_UP -> {
                justDown = false
                parent.requestDisallowInterceptTouchEvent(false)
            }
        }
        return super.dispatchTouchEvent(e)
    }
}
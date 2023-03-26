package me.wxc.widget.schedule.header

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import me.wxc.widget.base.ICalendarParent
import me.wxc.widget.base.ICalendarRender
import me.wxc.widget.base.IScheduleModel
import me.wxc.widget.tools.*
import java.util.*
import kotlin.math.roundToInt

class WeekView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr), ICalendarRender, ICalendarParent {
    override val parentRender: ICalendarRender?
        get() = (parent as? RecyclerView)?.adapter as? ICalendarRender
    override val calendar: Calendar = beginOfDay()
    override val beginTime: Long
        get() = beginOfDay(calendar.firstDayOfWeekTime).timeInMillis
    override val endTime: Long
        get() = beginOfDay(calendar.lastDayOfWeekTime).timeInMillis
    override var focusedDayTime: Long by setter(-1L) { _, time ->
        childRenders.forEach { it.focusedDayTime = time }
    }
    override var selectedDayTime: Long by setter(-1L) { _, time ->
        childRenders.forEach { it.selectedDayTime = time }
    }
    override var scheduleModels: List<IScheduleModel> = listOf()
        set(value) {
            field = value
            childRenders.forEach { it.getSchedulesFrom(value) }
        }
    override val childRenders: List<ICalendarRender>
        get() = children.filterIsInstance<ICalendarRender>().toList()

    private val dayWidth: Float
        get() = measuredWidth / 7f
    private val dayHeight: Float
        get() = 1f * (measuredHeight - paddingTop) / (childCount / 7)


    init {
        setWillNotDraw(false)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            val calendar = (child as ICalendarRender).calendar
            val dDays = calendar.timeInMillis.dDays - beginTime.dDays
            val line = dDays / 7
            val left = dDays % 7 * dayWidth
            val top = line * dayHeight
            val right = left + dayWidth
            val bottom = top + dayHeight
            if (top.isNaN()) continue
            child.layout(
                left.roundToInt(),
                top.roundToInt(),
                right.roundToInt(),
                bottom.roundToInt()
            )
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Log.i(TAG, "onAttachedToWindow ${beginTime.yyyyMMddHHmmss} ${endTime.yyyyMMddHHmmss}")
        for (time in beginTime..endTime step dayMillis) {
            WeekDayView(context).let { child ->
                child.calendar.timeInMillis = time
                addView(child)
                child.setOnClickListener {
                    if (selectedDayTime.dDays != child.beginTime.dDays) {
                        rootCalendarRender?.selectedDayTime = child.beginTime
                    }
                }
                if (scheduleModels.any()) {
                    child.getSchedulesFrom(scheduleModels)
                }
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        Log.i(TAG, "onDetachedFromWindow ${beginTime.yyyyMMddHHmmss} ${endTime.yyyyMMddHHmmss}")
        removeAllViews()
        focusedDayTime = -1L
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        focusedDayTime = -1L
    }

    companion object {
        private const val TAG = "WeekView"
    }
}
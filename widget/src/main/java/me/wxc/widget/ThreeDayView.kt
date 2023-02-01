package me.wxc.widget

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.core.view.updatePadding
import me.wxc.widget.components.*
import me.wxc.widget.tools.TAG
import me.wxc.widget.tools.dayLineHeight
import me.wxc.widget.tools.dp
import me.wxc.widget.tools.ifVisible

class ThreeDayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), ICalendarRender {

    init {
        updatePadding(top = 10.dp, bottom = 10.dp)
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    override val calendarPosition: Point = Point()
    override val adapter: ICalendarRenderAdapter = ThreeDayAdapter()


    // TODO 绘制：日期标尺、时刻标尺、新增日程、已有日程、当前时刻线
    // TODO 交互：左右滑动切换日期、点击新增/取消日程、日程抽象、tab联动能力、支持fling和自动居中

    override fun render(x: Int, y: Int) {
        calendarPosition.x = x
        calendarPosition.y = y + paddingTop
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val time = System.currentTimeMillis()
        adapter.visibleComponents.sortedBy { it.model.level }.forEach {
            it.updateRect(calendarPosition)
            if (it.rect.ifVisible(this)) {
                it.drawSelf(canvas, paint, calendarPosition)
            }
        }
        Log.i(TAG, "onDraw: ${System.currentTimeMillis() - time}")
    }
}

class ThreeDayAdapter : ICalendarRenderAdapter {
    override var models: MutableList<ICalendarModel> = mutableListOf<ICalendarModel>().apply {
        for (i in 0..24) {
            add(ClockLineModel(i))
            add(ClockTextModel(i))
        }
        for (i in -100..100) {
            add(DateLineModel(i))
        }
    }

    private var _visibleComponents: List<ICalendarComponent<*>> = listOf()
    override val visibleComponents: List<ICalendarComponent<*>>
        get() = _visibleComponents

    override fun onCreateComponent(model: ICalendarModel): ICalendarComponent<*>? {
        return when (model) {
            is ClockLineModel -> ClockLineComponent(model)
            is ClockTextModel -> ClockTextComponent(model)
            is DateLineModel -> DateLineComponent(model)
            is DateLineBgModel -> DateLineBgComponent()
            is CreateTaskModel -> CreateTaskComponent(model)
            is DailyTaskModel -> DailyTaskComponent(model)
            else -> null
        }
    }

    override fun notifyDataChanged() {
        _visibleComponents = models.mapNotNull { onCreateComponent(it) }
    }
}
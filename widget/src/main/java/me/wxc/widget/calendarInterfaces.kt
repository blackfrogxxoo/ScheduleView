package me.wxc.widget

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.graphics.RectF
import android.view.MotionEvent
import me.wxc.widget.components.CreateTaskModel
import me.wxc.widget.components.DailyTaskModel
import java.util.*

interface ICalendarWidget {
    val render: ICalendarRender
    val renderRange: RenderRange
    val onDateSelectedListener: Calendar.() -> Unit
    fun onTouchEvent(motionEvent: MotionEvent): Boolean
    fun onScroll(x: Int, y: Int)
    fun scrollTo(x: Int, y: Int)
    fun resetScrollState()
    fun isScrolling(): Boolean

    sealed interface RenderRange {
        object SingleDayRange : RenderRange
        object ThreeDayRange : RenderRange
    }

    companion object {
        val ICalendarWidget.TAG: String
            get() = this::class.java.simpleName
    }
}

interface ICalendarRender {
    var widget: ICalendarWidget
    val calendarPosition: Point
    val adapter: ICalendarRenderAdapter
    fun render(x: Int, y: Int)
}

interface ICalendarTaskCreator {
    val onDailyTaskClickBlock: DailyTaskModel.() -> Unit
    val onCreateTaskClickBlock: CreateTaskModel.() -> Unit
    fun addCreateTask(motionEvent: MotionEvent): Boolean
    fun removeCreateTask(): Boolean
}

interface ICalendarComponent<T : ICalendarModel> {
    var model: T
    val originRect: RectF
    val drawingRect: RectF
    fun onDraw(canvas: Canvas, paint: Paint)
    fun updateDrawingRect(anchorPoint: Point)
    fun onTouchEvent(e: MotionEvent): Boolean = false

    companion object {
        val ICalendarComponent<*>.TAG: String
            get() = this::class.java.simpleName
    }
}

interface ICalendarRenderAdapter {
    var models: MutableList<ICalendarModel>
    val visibleComponents: List<ICalendarComponent<*>>
    fun onCreateComponent(model: ICalendarModel): ICalendarComponent<*>?
    fun notifyModelsChanged()
    fun notifyModelAdded(model: ICalendarModel)
    fun notifyModelRemoved(model: ICalendarModel)
}

// TODO 可编辑逻辑从CreateTaskComponent抽象于此
interface ICalendarEditable {
    val editable: Boolean
    val editingRect: RectF?
}

interface ICalendarModel {
    val startTime: Long
    val endTime: Long
}
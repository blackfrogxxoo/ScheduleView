package me.wxc.widget.base

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.graphics.RectF
import android.view.MotionEvent
import me.wxc.widget.scheduler.components.CreateTaskModel
import me.wxc.widget.scheduler.components.DailyTaskModel
import java.util.*

interface ISchedulerWidget {
    val render: ISchedulerRender
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
        val ISchedulerWidget.TAG: String
            get() = this::class.java.simpleName
    }
}

interface ISchedulerRender {
    var widget: ISchedulerWidget
    val calendarPosition: Point
    val adapter: ISchedulerRenderAdapter
    fun render(x: Int, y: Int)
}

interface ISchedulerTaskCreator {
    val onDailyTaskClickBlock: DailyTaskModel.() -> Unit
    val onCreateTaskClickBlock: CreateTaskModel.() -> Unit
    fun addCreateTask(motionEvent: MotionEvent): Boolean
    fun removeCreateTask(): Boolean
}

interface ISchedulerComponent<T : ISchedulerModel> {
    var model: T
    val originRect: RectF
    val drawingRect: RectF
    fun onDraw(canvas: Canvas, paint: Paint)
    fun updateDrawingRect(anchorPoint: Point)
    fun onTouchEvent(e: MotionEvent): Boolean = false

    companion object {
        val ISchedulerComponent<*>.TAG: String
            get() = this::class.java.simpleName
    }
}

interface ISchedulerRenderAdapter {
    var models: MutableList<ISchedulerModel>
    val visibleComponents: List<ISchedulerComponent<*>>
    fun onCreateComponent(model: ISchedulerModel): ISchedulerComponent<*>?
    fun notifyModelsChanged()
    fun notifyModelAdded(model: ISchedulerModel)
    fun notifyModelRemoved(model: ISchedulerModel)
}

// TODO 可编辑逻辑从CreateTaskComponent抽象于此
interface ISchedulerEditable {
    val editable: Boolean
    val editingRect: RectF?
}

interface ISchedulerModel {
    val startTime: Long
    val endTime: Long
}
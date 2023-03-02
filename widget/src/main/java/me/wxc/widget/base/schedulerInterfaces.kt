package me.wxc.widget.base

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.graphics.RectF
import android.view.MotionEvent
import me.wxc.widget.schedule.components.CreateTaskComponent

interface IScheduleWidget : ISelectedDayTimeHolder {
    val render: IScheduleRender
    val renderRange: RenderRange
    fun onTouchEvent(motionEvent: MotionEvent): Boolean
    fun onScroll(x: Int, y: Int)
    fun scrollTo(x: Int, y: Int)
    fun isScrolling(): Boolean

    sealed interface RenderRange {
        object SingleDayRange : RenderRange
        object ThreeDayRange : RenderRange
    }

    companion object {
        val IScheduleWidget.TAG: String
            get() = this::class.java.simpleName
    }
}

interface IScheduleRender {
    var widget: IScheduleWidget
    val calendarPosition: Point
    val adapter: IScheduleRenderAdapter
    fun render(x: Int, y: Int)
}

interface IScheduleCreator {
    fun addCreateTask(motionEvent: MotionEvent): Boolean
    fun removeCreateTask(): Boolean
}

interface IScheduleComponent<T : IScheduleModel> {
    var model: T
    val originRect: RectF
    val drawingRect: RectF
    fun onDraw(canvas: Canvas, paint: Paint)
    fun updateDrawingRect(anchorPoint: Point)
    fun setCoincidedScheduleModels(coincided: List<IScheduleModel>) {}
    fun onTouchEvent(e: MotionEvent): Boolean = false

    companion object {
        val IScheduleComponent<*>.TAG: String
            get() = this::class.java.simpleName
    }
}

interface IScheduleRenderAdapter {
    var models: MutableList<IScheduleModel>
    val modelsGroupByDay: MutableMap<Int, List<IScheduleModel>>
    val fixedComponents: List<IScheduleComponent<*>>
    val visibleComponent: List<IScheduleComponent<*>>
    var createTaskComponent: CreateTaskComponent?
    fun onCreateComponent(model: IScheduleModel): IScheduleComponent<*>?
    fun notifyModelsChanged()
    fun notifyModelAdded(model: IScheduleModel)
    fun notifyModelRemoved(model: IScheduleModel)
}

// TODO 可编辑逻辑从CreateTaskComponent抽象于此
interface IScheduleEditable {
    val editable: Boolean
    val editingRect: RectF?
}

interface IScheduleModel : ITimeRangeHolder, java.io.Serializable
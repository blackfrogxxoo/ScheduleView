package me.wxc.widget.base

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.graphics.RectF
import android.util.SparseArray
import android.view.MotionEvent

interface ISchedulerWidget : ISelectedDayTimeHolder {
    val render: ISchedulerRender
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
    var modelsGroupByMonth: SparseArray<MutableList<ISchedulerModel>>
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

interface ISchedulerModel : ITimeRangeHolder, java.io.Serializable
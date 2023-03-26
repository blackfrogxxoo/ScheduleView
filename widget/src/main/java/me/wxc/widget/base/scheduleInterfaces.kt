package me.wxc.widget.base

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.graphics.RectF
import android.view.MotionEvent
import me.wxc.widget.schedule.components.DailyTaskComponent

interface IScheduleModelHolder : ITimeRangeHolder {
    val schedules: List<IScheduleModel>
}

interface IScheduleWidget : ICalendarRender {
    val render: IScheduleRender
    val renderRange: RenderRange
    fun onTouchEvent(motionEvent: MotionEvent): Boolean
    fun onScroll(x: Int, y: Int)
    fun scrollTo(x: Int, y: Int, duration: Int = 250)
    fun isScrolling(): Boolean

    sealed interface RenderRange {
        object SingleDayRange : RenderRange
        object ThreeDayRange : RenderRange
    }
}

interface IScheduleRender {
    var widget: IScheduleWidget
    val calendarPosition: Point
    val adapter: IScheduleRenderAdapter
    fun render(x: Int, y: Int)
    fun startEditingTask(motionEvent: MotionEvent, toEdit: DailyTaskModel? = null): Boolean
    fun finishEditing()
}

interface IScheduleComponent<T : IScheduleModel> {
    val model: T
    val originRect: RectF
    val drawingRect: RectF
    fun onDraw(canvas: Canvas, paint: Paint)
    fun updateDrawingRect(anchorPoint: Point)
    fun setCoincidedScheduleModels(coincided: List<IScheduleModel>) {}
    fun onTouchEvent(e: MotionEvent): Boolean = false
}

interface IScheduleRenderAdapter {
    var models: MutableList<IScheduleModel>
    val backgroundComponents: List<IScheduleComponent<*>>
    val foregroundComponents: List<IScheduleComponent<*>>
    val visibleComponents: List<IScheduleComponent<*>>
    var editingTaskComponent: DailyTaskComponent?
    fun modelsGroupByDay(startDay: Int, endDay: Int): Map<Int, List<IScheduleModel>>
    fun onCreateComponent(model: IScheduleModel): IScheduleComponent<*>?
    fun notifyModelsChanged()
    fun notifyModelAdded(model: IScheduleModel)
    fun notifyModelRemoved(model: IScheduleModel)
}

interface IScheduleModel : ITimeRangeHolder, java.io.Serializable {
    val taskId: Long
        get() = (this as? DailyTaskModel)?.id ?: -1
}
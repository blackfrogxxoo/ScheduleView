package me.wxc.widget.schedule

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.util.SparseArray
import android.view.MotionEvent
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.toPoint
import androidx.core.view.updatePadding
import me.wxc.widget.R
import me.wxc.widget.base.*
import me.wxc.widget.schedule.components.*
import me.wxc.widget.tools.*
import me.wxc.widget.schedule.components.ClockLineComponent
import me.wxc.widget.schedule.components.ClockLineModel

class ScheduleView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), IScheduleRender {

    init {
        updatePadding(top = canvasPadding, bottom = canvasPadding)
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(
            ResourcesCompat.getFont(context, R.font.product_sans_regular2),
            Typeface.NORMAL
        )
    }
    override lateinit var widget: IScheduleWidget
    override val calendarPosition: Point = Point()
    override val adapter: IScheduleRenderAdapter = ScheduleAdapter()

    override fun render(x: Int, y: Int) {
        calendarPosition.x = x
        calendarPosition.y = y + paddingTop
        invalidate()
    }

    private val coincided = mutableListOf<CoincideModel>()
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        adapter.backgroundComponents.forEach {
            it.updateDrawingRect(calendarPosition)
            if (it.drawingRect.ifVisible(this)) {
                it.onDraw(canvas, paint)
            }
        }
        val startDay = -(calendarPosition.x / dayWidth).toInt() - 1
        val endDay = startDay + 1 + (width / dayWidth).toInt()
        coincided.clear()
        val modelsGroupByDays = (adapter as ScheduleAdapter).modelsGroupByDay(startDay, endDay)
        modelsGroupByDays
            .filterKeys { it in startDay..endDay }
            .flatMap { it.value }
            .sortedBy { it.beginTime - (it.endTime - it.beginTime) / 1000 } // beginTime相同时，duration更长的先绘制
            .onEach {
                if ((it as? DailyTaskModel)?.editingTaskModel == null) {
                    if (coincided.isEmpty() || coincided.last().endTime <= it.beginTime) {
                        coincided.add(CoincideModel(it.beginTime, it.endTime, mutableListOf(it)))
                    } else {
                        coincided.last().endTime =
                            it.endTime.coerceAtLeast(coincided.last().endTime)
                        coincided.last().coincided.add(it)
                    }
                }
            }
            .map { adapter.onCreateComponent(it) }
            .onEach {
                (it as? DailyTaskComponent)?.existEditing =
                    it.model.taskId == adapter.editingTaskComponent?.model?.id
            }
            .apply { adapter.visibleComponents = this }
            .forEach { component ->
                component.setCoincidedScheduleModels(
                    coincided.find { it.coincided.size > 1 && it.coincided.contains(component.model) }?.coincided
                        ?: emptyList()
                )
                component.updateDrawingRect(calendarPosition)
                if (component.drawingRect.ifVisible(this)) {
                    component.onDraw(canvas, paint)
                }
            }
        adapter.editingTaskComponent?.let {
            it.updateDrawingRect(calendarPosition)
            it.onDraw(canvas, paint)
        }
        adapter.foregroundComponents.forEach {
            it.updateDrawingRect(calendarPosition)
            if (it.drawingRect.ifVisible(this)) {
                it.onDraw(canvas, paint)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return widget.onTouchEvent(event)
    }

    override fun startEditingTask(
        motionEvent: MotionEvent,
        toEdit: DailyTaskModel?
    ): Boolean {
        if (toEdit == null && adapter.editingTaskComponent != null) return false
        val model = toEdit?.copy(
            editingTaskModel = EditingTaskModel(
                state = State.DRAG_BODY
            ) { x, y ->
                if (!widget.isScrolling()) {
                    widget.scrollTo(
                        -calendarPosition.x + x,
                        -calendarPosition.y + paddingTop + y,
                        duration = if (widget.renderRange == IScheduleWidget.RenderRange.ThreeDayRange) 250 else 500
                    )
                }
            }
        ) ?: DailyTaskModel(
            beginTime = PointF(motionEvent.x - dayWidth / 2, motionEvent.y)
                .toPoint()
                .positionToTime(-calendarPosition.x, -calendarPosition.y)
                .adjustTimestamp(quarterMillis, true),
            editingTaskModel = EditingTaskModel { x, y ->
                if (!widget.isScrolling()) {
                    widget.scrollTo(
                        -calendarPosition.x + x,
                        -calendarPosition.y + paddingTop + y,
                        duration = if (widget.renderRange == IScheduleWidget.RenderRange.ThreeDayRange) 250 else 500
                    )
                }
            }
        )
        (adapter.onCreateComponent(model) as DailyTaskComponent).apply {
            downX = motionEvent.x
            downY = motionEvent.y
            lastX = motionEvent.x
            lastY = motionEvent.y
            updateDrawingRect(calendarPosition)
        }
        return true
    }

    override fun finishEditing() {
        adapter.editingTaskComponent = null
    }

    companion object {
        private const val TAG = "ScheduleView"
    }
}

data class CoincideModel(
    override var beginTime: Long,
    override var endTime: Long,
    val coincided: MutableList<IScheduleModel>
) : IScheduleModel

class ScheduleAdapter : IScheduleRenderAdapter {
    override var models: MutableList<IScheduleModel> = mutableListOf()
    override var visibleComponents: List<IScheduleComponent<*>> = listOf()
    override var editingTaskComponent: DailyTaskComponent? = null
        set(value) {
            field = value
            backgroundComponents.map { it.model }.filterIsInstance<ClockLineModel>().forEach {
                it.createTaskModel = value?.model
            }
            if (value == null) {
                models.removeIf { (it as? DailyTaskModel)?.editingTaskModel != null }
            }
            models.removeIf { it.taskId == field?.model?.taskId }
        }
    override val backgroundComponents: List<IScheduleComponent<*>> =
        mutableListOf<IScheduleComponent<*>>().apply {
            for (i in 0..24) {
                add(ClockLineComponent(ClockLineModel(i)))
            }
        }.toList()
    override val foregroundComponents: List<IScheduleComponent<*>> = listOf(
        DateLineComponent(),
        WeekLineComponent(),
        NowLineComponent()
    )
    private val _taskComponentCache = SparseArray<IScheduleComponent<*>>()

    private val _modelsGroupByDay: MutableMap<Int, List<IScheduleModel>> = mutableMapOf()

    override fun modelsGroupByDay(startDay: Int, endDay: Int): Map<Int, List<IScheduleModel>> {
        if (_modelsGroupByDay.none() && models.any()) {
            models.filter { (it as? DailyTaskModel)?.editingTaskModel == null }
                .groupBy { it.beginTime.dDays.toInt() }
                .apply { _modelsGroupByDay.putAll(this) }
        }
        return _modelsGroupByDay.filter { it.key in startDay..endDay }
    }

    override fun onCreateComponent(model: IScheduleModel): IScheduleComponent<*> {
        return when (model) {
            is DailyTaskModel -> if (model.editingTaskModel != null) {
                editingTaskComponent ?: run {
                    DailyTaskComponent(model).apply { editingTaskComponent = this }
                }
            } else {
                _taskComponentCache.get(model.id.toInt()) ?: DailyTaskComponent(
                    model
                ).apply {
                    _taskComponentCache.append(model.id.toInt(), this)
                    Log.i("ScheduleView", "create component: $this, ${_taskComponentCache.size()}")
                }
            }
            else -> throw IllegalArgumentException("invalid model: $model")
        }
    }

    override fun notifyModelsChanged() {
        _taskComponentCache.clear()
        _modelsGroupByDay.clear()
        models.groupBy { it.beginTime.dDays.toInt() }
            .apply { _modelsGroupByDay.putAll(this) }
    }

    override fun notifyModelAdded(model: IScheduleModel) {
        val key = model.beginTime.dDays.toInt()
        _modelsGroupByDay[key] = _modelsGroupByDay
            .getOrPut(key) { emptyList() }
            .toMutableList()
            .onEach { // 删除当天所有component的缓存
                _taskComponentCache.remove(it.taskId.toInt())
            }
            .apply { add(model) }
            .toList()
    }

    override fun notifyModelRemoved(model: IScheduleModel) {
        val key = model.beginTime.dDays.toInt()
        _modelsGroupByDay[key] = _modelsGroupByDay
            .getOrPut(key) { emptyList() }
            .toMutableList()
            .onEach { // 删除当天所有component的缓存
                _taskComponentCache.remove(it.taskId.toInt())
            }
            .apply { remove(model) }
            .toList()
    }
}
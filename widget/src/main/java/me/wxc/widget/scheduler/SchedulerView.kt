package me.wxc.widget.scheduler

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
import me.wxc.widget.scheduler.components.*
import me.wxc.widget.tools.*

class SchedulerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), ISchedulerRender, ISchedulerTaskCreator {

    init {
        updatePadding(top = canvasPadding, bottom = canvasPadding)
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(
            ResourcesCompat.getFont(context, R.font.product_sans_regular2),
            Typeface.NORMAL
        )
    }
    override lateinit var widget: ISchedulerWidget
    override val calendarPosition: Point = Point()
    override val adapter: ISchedulerRenderAdapter = ThreeDayAdapter()

    override fun render(x: Int, y: Int) {
        calendarPosition.x = x
        calendarPosition.y = y + paddingTop
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val time = System.currentTimeMillis()
        adapter.fixedComponents.forEach {
            it.updateDrawingRect(calendarPosition)
            if (it.drawingRect.ifVisible(this) || it is CreateTaskComponent || it is DateLineComponent) {
                it.onDraw(canvas, paint)
            }
        }
        val start = -(calendarPosition.x / dayWidth).toInt() - 1
        val end = start + 1 + (width / dayWidth).toInt()
        val coincided = mutableListOf<CoincideModel>()
        (adapter as ThreeDayAdapter).modelsGroupByDay
            .filterKeys { it in start..end }.flatMap { it.value }
            .apply {
                this.sortedBy { it.startTime }.forEach {
                    if (coincided.isEmpty() || coincided.last().endTime <= it.startTime) {
                        coincided.add(CoincideModel(it.startTime, it.endTime, mutableListOf(it)))
                    } else {
                        coincided.last().endTime =
                            it.endTime.coerceAtLeast(coincided.last().endTime)
                        coincided.last().coincided.add(it)
                    }
                }
            }
            .map { adapter.onCreateComponent(it) }
            .apply {
                adapter.visibleComponent = this
            }.forEach { component ->
                component.setCoincidedScheduleModels(
                    coincided.find { it.coincided.size > 1 && it.coincided.contains(component.model) }?.coincided
                        ?: emptyList()
                )
                component.updateDrawingRect(calendarPosition)
                if (component.drawingRect.ifVisible(this) || component is CreateTaskComponent || component is DateLineComponent) {
                    component.onDraw(canvas, paint)
                }
            }
        adapter.createTaskComponent?.let {
            it.updateDrawingRect(calendarPosition)
            it.onDraw(canvas, paint)
        }
        Log.i(TAG, "onDraw cost time: ${System.currentTimeMillis() - time}")
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return widget.onTouchEvent(event)
    }

    override fun addCreateTask(motionEvent: MotionEvent): Boolean {
        if (adapter.createTaskComponent != null) return false
        val model = CreateTaskModel(
            startTime = PointF(motionEvent.x - dayWidth / 2, motionEvent.y)
                .toPoint()
                .positionToTime(-calendarPosition.x, -calendarPosition.y)
                .adjustTimeInDay(quarterMillis, true),
            duration = hourMillis / 2,
            title = "",
        ) { x, y ->
            if (!widget.isScrolling()) {
                widget.scrollTo(-calendarPosition.x + x, -calendarPosition.y + paddingTop + y)
            }
        }.apply {
            Log.i(
                TAG,
                "new task: ${sdf_yyyyMMddHHmmss.format(startTime)}"
            )
            adapter.models.filterIsInstance<ClockLineModel>().forEach {
                it.createTaskModel = this
            }
        }
        adapter.createTaskComponent = adapter.onCreateComponent(model) as CreateTaskComponent
        return true
    }

    override fun removeCreateTask(): Boolean {
        adapter.createTaskComponent = null
        return true
    }
}

data class CoincideModel(
    override var startTime: Long,
    override var endTime: Long,
    val coincided: MutableList<ISchedulerModel>
) : ISchedulerModel

class ThreeDayAdapter : ISchedulerRenderAdapter {
    override var models: MutableList<ISchedulerModel> = mutableListOf()
    override var modelsGroupByDay = mutableMapOf<Int, List<ISchedulerModel>>()
    override var visibleComponent: List<ISchedulerComponent<*>> = listOf()
    override var createTaskComponent: CreateTaskComponent? = null
    override val fixedComponents: List<ISchedulerComponent<*>> =
        mutableListOf<ISchedulerComponent<*>>().apply {
            for (i in 0..24) {
                add(ClockLineComponent(ClockLineModel(i)))
            }
            add(DateLineComponent(DateLineModel))
            add(WeekLineComponent(WeekLineModel))
            add(NowLineComponent(NowLineModel))
        }.toList()

    private val _taskComponentCache = SparseArray<ISchedulerComponent<*>>()

    override fun onCreateComponent(model: ISchedulerModel): ISchedulerComponent<*> {
        // TODO 引入对象缓存机制
        return when (model) {
            is CreateTaskModel -> createTaskComponent ?: run {
                CreateTaskComponent(model).apply { createTaskComponent = this }
            }
            is DailyTaskModel -> _taskComponentCache.get(model.id.toInt()) ?: DailyTaskComponent(
                model
            ).apply {
                _taskComponentCache.append(model.id.toInt(), this)
                Log.i("SchedulerView", "create component: $this")
            }
            else -> throw IllegalArgumentException("invalid model: $model")
        }
    }

    override fun notifyModelsChanged() {
        _taskComponentCache.clear()
        modelsGroupByDay = models.groupBy {
            it.startTime.dDays.toInt()
        }.toMutableMap()
    }

    override fun notifyModelAdded(model: ISchedulerModel) {
        val key = model.startTime.dDays.toInt()
        modelsGroupByDay[key] = modelsGroupByDay
            .getOrPut(key) { emptyList() }
            .toMutableList()
            .apply { add(model) }
            .toList()
    }

    private fun List<ISchedulerComponent<*>>.sortComponent(): List<ISchedulerComponent<*>> =
        sortedBy {
            when (it) {
                is NowLineComponent -> 1
                is DateLineComponent -> 2
                is WeekLineComponent -> 2
                else -> 0
            }
        }

    override fun notifyModelRemoved(model: ISchedulerModel) {
        val key = model.startTime.dDays.toInt()
        modelsGroupByDay[key] = modelsGroupByDay
            .getOrPut(key) { emptyList() }
            .toMutableList()
            .apply { remove(model) }
            .toList()
    }
}
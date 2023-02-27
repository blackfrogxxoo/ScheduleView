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
        typeface = Typeface.create(ResourcesCompat.getFont(context, R.font.product_sans_regular2), Typeface.NORMAL)
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
        adapter.visibleComponents.forEach {
            it.updateDrawingRect(calendarPosition)
            if (it.drawingRect.ifVisible(this) || it is CreateTaskComponent || it is DateLineComponent) {
                it.onDraw(canvas, paint)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return widget.onTouchEvent(event)
    }

    override fun addCreateTask(motionEvent: MotionEvent): Boolean {
        if (adapter.models.any { it is CreateTaskModel }) return false
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
        adapter.models.add(model)
        adapter.notifyModelAdded(model)
        return true
    }

    override fun removeCreateTask(): Boolean {
        val model = adapter.models.find { it is CreateTaskModel }
        model?.let {
            adapter.models.remove(it)
            adapter.models.filterIsInstance<ClockLineModel>().forEach {
                it.createTaskModel = null
            }
            adapter.notifyModelRemoved(it)
            return true
        }
        return false
    }
}

class ThreeDayAdapter : ISchedulerRenderAdapter {
    override var models: MutableList<ISchedulerModel> = mutableListOf<ISchedulerModel>().apply {
        for (i in 0..24) {
            add(ClockLineModel(i))
        }
        add(DateLineModel)
        add(NowLineModel)
    }
    override var modelsGroupByMonth: SparseArray<MutableList<ISchedulerModel>> = SparseArray()

    private var _visibleComponents: List<ISchedulerComponent<*>> = listOf()
    override val visibleComponents: List<ISchedulerComponent<*>>
        get() = _visibleComponents

    override fun onCreateComponent(model: ISchedulerModel): ISchedulerComponent<*>? {
        // TODO 引入对象缓存机制
        return when (model) {
            is ClockLineModel -> ClockLineComponent(model)
            is DateLineModel -> if (SchedulerWidget.isThreeDay) DateLineComponent(model) else WeekLineComponent(
                WeekLineModel
            )
            is CreateTaskModel -> CreateTaskComponent(model)
            is DailyTaskModel -> DailyTaskComponent(model)
            is NowLineModel -> NowLineComponent(model)
            else -> null
        }
    }

    override fun notifyModelsChanged() {
        _visibleComponents = models.mapNotNull { onCreateComponent(it) }.sortComponent()
    }

    override fun notifyModelAdded(model: ISchedulerModel) {
        _visibleComponents = _visibleComponents.toMutableList().apply {
            onCreateComponent(model)?.let { add(it) }
        }.sortComponent()
    }

    private fun List<ISchedulerComponent<*>>.sortComponent(): List<ISchedulerComponent<*>> = sortedBy {
        when (it) {
            is NowLineComponent -> 1
            is DateLineComponent -> 2
            is WeekLineComponent -> 2
            else -> 0
        }
    }

    override fun notifyModelRemoved(model: ISchedulerModel) {
        _visibleComponents = _visibleComponents.toMutableList().filterNot {
            it.model == model
        }.toList()
    }
}
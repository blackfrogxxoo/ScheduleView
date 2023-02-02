package me.wxc.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.graphics.PointF
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.toPoint
import androidx.core.view.updatePadding
import me.wxc.widget.components.*
import me.wxc.widget.tools.*

class ThreeDayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), ICalendarRender, ICalendarTaskCreator {

    init {
        updatePadding(top = canvasPadding, bottom = canvasPadding)
    }

    private lateinit var canvas: Canvas
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    override lateinit var widget: ICalendarWidget
    override val calendarPosition: Point = Point()
    override val adapter: ICalendarRenderAdapter = ThreeDayAdapter()
    override var onDailyTaskClickBlock: DailyTaskModel.() -> Unit = {}
    override var onCreateTaskClickBlock: CreateTaskModel.() -> Unit = {}

    override fun render(x: Int, y: Int) {
        calendarPosition.x = x
        calendarPosition.y = y + paddingTop
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        adapter.visibleComponents.forEach {
            it.updateDrawingRect(calendarPosition)
            if (it.drawingRect.ifVisible(this) || it is CreateTaskComponent) {
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
                .adjustTimeInDay(quarterMills, true),
            duration = hourMills / 2,
            title = "新建日程",
            onClickBlock = onCreateTaskClickBlock,
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

class ThreeDayAdapter : ICalendarRenderAdapter {
    override var models: MutableList<ICalendarModel> = mutableListOf<ICalendarModel>().apply {
        for (i in 0..24) {
            add(ClockLineModel(i))
        }
        // TODO 支持动态添加/删除
        for (i in -3650..3650) {
            add(DateLineModel(i))
        }
        add(NowLineModel)
        add(DateLineShadowModel)
    }

    private var _visibleComponents: List<ICalendarComponent<*>> = listOf()
    override val visibleComponents: List<ICalendarComponent<*>>
        get() = _visibleComponents

    override fun onCreateComponent(model: ICalendarModel): ICalendarComponent<*>? {
        // TODO 引入对象缓存机制
        return when (model) {
            is ClockLineModel -> ClockTextComponent(model)
            is DateLineModel -> DateLineComponent(model)
            is DateLineShadowModel -> DateLineShadowComponent()
            is CreateTaskModel -> CreateTaskComponent(model)
            is DailyTaskModel -> DailyTaskComponent(model)
            is NowLineModel -> NowLineComponent(model)
            else -> null
        }
    }

    override fun notifyModelsChanged() {
        _visibleComponents = models.mapNotNull { onCreateComponent(it) }.sortedBy {
            when (it) {
                is NowLineComponent -> 1
                is DateLineShadowComponent -> 2
                else -> 0
            }
        }
    }

    override fun notifyModelAdded(model: ICalendarModel) {
        _visibleComponents = _visibleComponents.toMutableList().apply {
            onCreateComponent(model)?.let { add(it) }
        }.sortedBy {
            when (it) {
                is NowLineComponent -> 1
                is DateLineShadowComponent -> 2
                else -> 0
            }
        }
    }

    override fun notifyModelRemoved(model: ICalendarModel) {
        _visibleComponents = _visibleComponents.toMutableList().filterNot {
            it.model == model
        }.toList()
    }
}
package me.wxc.widget

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.graphics.RectF

interface ICalendarScroller {
    var render: ICalendarRender
    fun onScroll(x: Int, y: Int)
    fun snapToPosition()

    companion object {
        val ICalendarScroller.TAG: String
            get() = this::class.java.simpleName
    }
}

interface ICalendarRender {
    val calendarPosition: Point
    val adapter: ICalendarRenderAdapter
    fun render(x: Int, y: Int)
}

interface ICalendarComponent<T : ICalendarModel> {
    var model: T
    val originRect: RectF
    val rect: RectF
    fun drawSelf(canvas: Canvas, paint: Paint, anchorPoint: Point)
    fun updateRect(anchorPoint: Point)
}

interface ICalendarRenderAdapter {
    var models: MutableList<ICalendarModel>
    val visibleComponents: List<ICalendarComponent<*>>
    fun onCreateComponent(model: ICalendarModel) : ICalendarComponent<*>?
    fun notifyDataChanged()
}

interface ICalendarEditable

interface ICalendarModel {
    val startTime: Long
    val endTime: Long
    val level: Int
}
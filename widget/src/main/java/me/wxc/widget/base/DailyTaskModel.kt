package me.wxc.widget.base

import android.graphics.RectF
import me.wxc.widget.tools.adjustTimestamp
import me.wxc.widget.tools.hourMillis
import me.wxc.widget.tools.nowMillis
import me.wxc.widget.tools.quarterMillis

data class DailyTaskModel(
    val id: Long = 0,
    override var beginTime: Long = nowMillis.adjustTimestamp(quarterMillis, true),
    var duration: Long = hourMillis / 2,
    var title: String = "",
    var repeatId: String = "",
    var repeatMode: RepeatMode = RepeatMode.Never,
    val calendarEventId: Long = 0,
    var remindMode: RemindMode = RemindMode.Never,
    val editingTaskModel: EditingTaskModel? = null,
) : IScheduleModel {
    override val endTime: Long
        get() = beginTime + duration

    fun changeBeginTime(time: Long) {
        val temp = beginTime
        beginTime = time
        duration -= time - temp
    }

    fun changeEndTime(time: Long) {
        duration += time - endTime
    }

    internal val expired: Boolean
        get() = nowMillis > endTime
}

enum class State {
    IDLE, DRAG_BODY, DRAG_TOP, DRAG_BOTTOM
}

data class EditingTaskModel(
    var draggingRect: RectF? = null,
    var state: State = State.IDLE,
    val onNeedScrollBlock: (x: Int, y: Int) -> Unit = { x, y -> }
)
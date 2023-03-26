package me.wxc.widget

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import me.wxc.widget.base.DailyTaskModel
import me.wxc.widget.base.IScheduleModel
import me.wxc.widget.tools.beginOfDay
import me.wxc.widget.tools.nowMillis
import java.util.*

object ScheduleConfig {
    lateinit var app: Application
    val colorWhite: Int
        get() = app.getColor(R.color.white)
    val colorBlack1: Int
        get() = app.getColor(R.color.black1)
    val colorBlack2: Int
        get() = app.getColor(R.color.black2)
    val colorBlack3: Int
        get() = app.getColor(R.color.black3)
    val colorBlack4: Int
        get() = app.getColor(R.color.black4)
    val colorBlack5: Int
        get() = app.getColor(R.color.black5)
    val colorBlack6: Int
        get() = app.getColor(R.color.black6)
    val colorBlue1: Int
        get() = app.getColor(R.color.blue1)
    val colorBlue2: Int
        get() = app.getColor(R.color.blue2)
    val colorBlue3: Int
        get() = app.getColor(R.color.blue3)
    val colorBlue4: Int
        get() = app.getColor(R.color.blue4)
    val colorBlue5: Int
        get() = app.getColor(R.color.blue5)
    val colorBlue6: Int
        get() = app.getColor(R.color.blue6)
    val colorTransparent1: Int
        get() = app.getColor(R.color.transparent1)
    val colorTransparent2: Int
        get() = app.getColor(R.color.transparent2)
    val colorTransparent3: Int
        get() = app.getColor(R.color.transparent3)
    var scheduleBeginTime: Long = 0L
    var scheduleEndTime: Long = beginOfDay().apply { add(Calendar.MONTH, 1200) }.timeInMillis
    var lifecycleScope: CoroutineScope = GlobalScope
    var selectedDayTime: Long = nowMillis
    var onDateSelectedListener: Calendar.() -> Unit = {}
    var scheduleModelsProvider: suspend (beginTime: Long, endTime: Long) -> List<IScheduleModel> =
        { _, _ ->
            emptyList()
        }
    var onDailyTaskClickBlock: (model: DailyTaskModel) -> Unit = {}
    var onCreateTaskClickBlock: (model: DailyTaskModel) -> Unit = {}
    var onTaskDraggedBlock: (model: DailyTaskModel) -> Unit = {}
}
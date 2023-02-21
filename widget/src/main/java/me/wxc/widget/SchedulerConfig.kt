package me.wxc.widget

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import me.wxc.widget.base.ISchedulerModel
import me.wxc.widget.scheduler.components.CreateTaskModel
import me.wxc.widget.scheduler.components.DailyTaskModel
import me.wxc.widget.tools.startOfDay
import java.util.*

object SchedulerConfig {
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
    val colorTransparent1: Int
        get() = app.getColor(R.color.transparent1)
    val colorTransparent2: Int
        get() = app.getColor(R.color.transparent2)
    val colorTransparent3: Int
        get() = app.getColor(R.color.transparent3)
    var schedulerStartTime: Long = 0L
    var schedulerEndTime: Long = startOfDay().apply { add(Calendar.MONTH, 1200) }.timeInMillis
    var lifecycleScope: CoroutineScope = GlobalScope
    var selectedTime: Long = System.currentTimeMillis()
    var onDateSelectedListener: Calendar.() -> Unit = {}
    var schedulerModelsProvider: suspend (startTime: Long, endTime: Long) -> List<ISchedulerModel> =
        { _, _ ->
            emptyList()
        }
    var onDailyTaskClickBlock: (model: DailyTaskModel) -> Unit = {}
    var onCreateTaskClickBlock: (model: CreateTaskModel) -> Unit = {}
}
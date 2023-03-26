package me.wxc.todolist.business

import android.util.Log
import androidx.fragment.app.FragmentManager
import me.wxc.todolist.data.DailyTaskLocal
import me.wxc.todolist.tools.CalendarEvents
import me.wxc.todolist.ui.*
import me.wxc.widget.ScheduleConfig
import me.wxc.widget.base.*
import me.wxc.widget.tools.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.wxc.todolist.data.DailyTaskRepository
import java.util.*
import kotlin.math.max
import kotlin.math.min

class DailyTaskBusinessImpl : DailyTaskBusiness {
    private val repo: DailyTaskRepository by lazy { DailyTaskRepository() }

    private fun refreshCache(beginTime: Long, endTime: Long, list: List<IScheduleModel>) {
        dailyTaskHolder = dailyTaskHolder.copy(
            beginTime = min(beginTime, dailyTaskHolder.beginTime),
            endTime = max(endTime, dailyTaskHolder.endTime),
            schedules = dailyTaskHolder.schedules.toMutableList().apply {
                val ids = list.map { it.taskId }
                removeAll { it.taskId in ids }
                addAll(list)
            }.toList()
        )
        Log.i(
            TAG,
            "refreshCache: ${dailyTaskHolder.beginTime}, ${dailyTaskHolder.endTime}, ${dailyTaskHolder.schedules.size}"
        )

    }

    override suspend fun saveCreateDailyTask(
        model: DailyTaskModel,
        block: (created: List<IScheduleModel>) -> Unit
    ) {
        withContext(Dispatchers.Default) {
            if (model.title.isBlank()) {
                model.title = "(无主题)"
            }
            when (model.repeatMode) {
                RepeatMode.Never -> {
                    createSingleTask(model, block)
                }
                else -> {
                    performSaveRepeatableTask(model, block)
                }
            }
        }
    }

    override suspend fun getDailyTasks(
        beginTime: Long,
        endTime: Long
    ): List<IScheduleModel> = withContext(Dispatchers.IO) {
        if (beginTime >= dailyTaskHolder.beginTime && endTime <= dailyTaskHolder.endTime) {
            Log.i(
                TAG,
                "read from cache: ($beginTime-$endTime) in (${dailyTaskHolder.beginTime}-${dailyTaskHolder.endTime}), ${dailyTaskHolder.schedules.size}"
            )
            dailyTaskHolder.schedules.filter { it.beginTime >= beginTime && it.endTime <= endTime }
        } else {
            repo.getRangeDailyTasks(beginTime, endTime).map {
                it.mapModel
            }.apply {
                refreshCache(beginTime, endTime, this)
            }
        }
    }

    override suspend fun removeDailyTasks(
        model: DailyTaskModel,
        option: DeleteOptionFragment.DeleteOption
    ): List<Long> {
        val deletedIds = mutableListOf<Long>()
        withContext(Dispatchers.IO) {
            when (option) {
                DeleteOptionFragment.DeleteOption.ONE -> {
                    repo.removeById(model.id)
                    deletedIds.add(model.id)
                    dailyTaskHolder = dailyTaskHolder.copy(
                        schedules = dailyTaskHolder.schedules.filter {
                            it.taskId != model.id
                        }
                    )
                    CalendarEvents.deleteCalendarEvent(
                        ScheduleConfig.app.applicationContext,
                        model.calendarEventId,
                    )
                }
                DeleteOptionFragment.DeleteOption.ALL -> {
                    repo.getRepeatDailyTasks(model.repeatId)
                        .onEach {
                            CalendarEvents.deleteCalendarEvent(
                                ScheduleConfig.app.applicationContext,
                                it.calendarEventId,
                            )
                        }
                        .map { it.id }
                        .apply {
                            deletedIds.addAll(this)
                        }
                    repo.removeByRepeatId(model.repeatId, -1L)
                    dailyTaskHolder = dailyTaskHolder.copy(
                        schedules = dailyTaskHolder.schedules.filter {
                            (it as? DailyTaskModel)?.repeatId != model.repeatId
                        }
                    )
                }
                else -> {
                    repo.getRepeatDailyTasks(model.repeatId)
                        .filter {
                            it.id >= model.id
                        }.onEach {
                            CalendarEvents.deleteCalendarEvent(
                                ScheduleConfig.app.applicationContext,
                                it.calendarEventId,
                            )
                        }
                        .map { it.id }
                        .apply {
                            deletedIds.addAll(this)
                        }
                    repo.removeByRepeatId(model.repeatId, model.id)
                    dailyTaskHolder = dailyTaskHolder.copy(
                        schedules = dailyTaskHolder.schedules.filter {
                            (it as? DailyTaskModel)?.repeatId != model.repeatId
                                    || it.taskId < model.id
                        }
                    )
                }
            }
        }
        return deletedIds
    }

    override suspend fun updateDailyTasks(
        model: DailyTaskModel,
        option: UpdateOptionFragment.UpdateOption,
    ): List<Long> {
        val updatedIds = mutableListOf<Long>()
        val dBeginTime = model.beginTime - beginOfDay(model.beginTime).timeInMillis
        val dEndTIme = model.endTime - beginOfDay(model.endTime).timeInMillis
        withContext(Dispatchers.IO) {
            when (option) {
                UpdateOptionFragment.UpdateOption.ONE -> {
                    val newEventId = CalendarEvents.updateCalendarEvent(
                        ScheduleConfig.app,
                        model.calendarEventId,
                        model.title.calendarScheduleTitle,
                        "",
                        model.beginTime,
                        model.endTime,
                        model.remindMode.remindMinutes
                    )
                    repo.putDailyTask(
                        DailyTaskLocal(
                            id = model.id,
                            beginTime = model.beginTime,
                            endTime = model.endTime,
                            title = model.title,
                            repeatType = model.repeatMode.repeatModeInt,
                            repeatInterval = model.repeatMode.repeatInterval,
                            repeatId = model.repeatId,
                            remindMinutes = model.remindMode.remindMinutes,
                            calendarEventId = newEventId
                        )
                    )
                    refreshCache(
                        model.beginTime,
                        model.endTime,
                        listOf(model.copy(editingTaskModel = null, calendarEventId = newEventId))
                    )
                    updatedIds.add(model.id)
                }
                UpdateOptionFragment.UpdateOption.FROM -> {
                    val locals = repo.getRepeatDailyTasks(model.repeatId)
                    repo.putDailyTasks(locals.filter { it.beginTime.dDays >= model.beginTime.dDays }
                        .map {
                            updatedIds.add(it.id)
                            it.copy(
                                beginTime = beginOfDay(it.beginTime).timeInMillis + dBeginTime,
                                endTime = beginOfDay(it.endTime).timeInMillis + dEndTIme,
                                title = model.title,
                            )
                        }.onEach {
                            updatedIds.add(it.id)
                            dailyTaskHolder
                            CalendarEvents.updateCalendarEvent(
                                ScheduleConfig.app,
                                it.calendarEventId,
                                it.title.calendarScheduleTitle,
                                "",
                                it.beginTime,
                                it.endTime,
                                it.remindMinutes
                            )
                        }.apply {
                            val list = map {
                                it.mapModel
                            }
                            refreshCache(list.first().beginTime, list.last().endTime, list)
                        })
                }
                UpdateOptionFragment.UpdateOption.ALL -> {
                    val locals = repo.getRepeatDailyTasks(model.repeatId)
                    repo.putDailyTasks(locals.map {
                        updatedIds.add(it.id)
                        it.copy(
                            beginTime = beginOfDay(it.beginTime).timeInMillis + dBeginTime,
                            endTime = beginOfDay(it.endTime).timeInMillis + dEndTIme,
                            title = model.title,
                        )
                    }.onEach {
                        updatedIds.add(it.id)
                        CalendarEvents.updateCalendarEvent(
                            ScheduleConfig.app,
                            it.calendarEventId,
                            it.title.calendarScheduleTitle,
                            "",
                            it.beginTime,
                            it.endTime,
                            it.remindMinutes
                        )
                    }.apply {
                        val list = map {
                            it.mapModel
                        }
                        refreshCache(list.first().beginTime, list.last().endTime, list)
                    })
                }
            }
        }
        return updatedIds
    }


    private fun createSingleTask(
        model: DailyTaskModel,
        block: (created: List<IScheduleModel>) -> Unit
    ) {
        val repeatId = UUID.randomUUID().toString()
        val calendarEventId = if (model.remindMode != RemindMode.Never) {
            CalendarEvents.addCalendarEvent(
                ScheduleConfig.app.applicationContext,
                model.title.calendarScheduleTitle,
                "",
                model.beginTime,
                model.endTime,
                model.remindMode.remindMinutes
            )
        } else {
            0
        }
        val id = repo.putDailyTask(
                DailyTaskLocal(
                    beginTime = model.beginTime,
                    endTime = model.endTime,
                    title = model.title,
                    repeatId = repeatId,
                    repeatInterval = model.repeatMode.repeatInterval,
                    repeatType = model.repeatMode.repeatModeInt,
                    remindMinutes = model.remindMode.remindMinutes,
                    calendarEventId = calendarEventId
                )
            )
        DailyTaskModel(
            id = id,
            beginTime = model.beginTime,
            duration = model.duration,
            title = model.title,
            repeatId = repeatId,
            repeatMode = model.repeatMode,
            calendarEventId = calendarEventId,
            remindMode = model.remindMode
        ).apply {
            block.invoke(listOf(this))
            refreshCache(model.beginTime, model.endTime, listOf(this))
        }
    }

    private fun performSaveRepeatableTask(
        model: DailyTaskModel,
        block: (created: List<IScheduleModel>) -> Unit
    ) {
        val repeatId = UUID.randomUUID().toString()
        var calendarEventCount = 0
        val locals = (0..2000).map {
            model.repeatMode.repeatBeginTimeByIndex(model.beginTime, it)
        }.filter {
            model.repeatMode.repeatedModelValid(it, model.beginTime)
        }.map {
            DailyTaskLocal(
                beginTime = it,
                endTime = it + model.duration,
                title = model.title,
                repeatId = repeatId,
                repeatInterval = model.repeatMode.repeatInterval,
                repeatType = model.repeatMode.repeatModeInt,
                remindMinutes = model.remindMode.remindMinutes,
                calendarEventId = if (calendarEventCount < 5) {
                    calendarEventCount++
                    CalendarEvents.addCalendarEvent(
                        ScheduleConfig.app.applicationContext,
                        model.title.calendarScheduleTitle,
                        "",
                        it,
                        it + model.duration,
                        model.remindMode.remindMinutes
                    )
                } else {
                    0
                }
            )
        }
        val ids = repo.putDailyTasks(locals)
        locals.mapIndexed { index, local ->
            DailyTaskModel(
                id = ids[index],
                beginTime = local.beginTime,
                duration = model.duration,
                title = model.title,
                repeatId = repeatId,
                repeatMode = model.repeatMode,
                calendarEventId = local.calendarEventId,
                remindMode = model.remindMode
            )
        }.apply {
            block.invoke(this)
            refreshCache(model.beginTime, model.endTime, this)
        }
    }

    override fun showCreateFragment(
        fragmentManager: FragmentManager,
        beginTime: Long?,
        duration: Long?,
        title: String?,
        repeatMode: RepeatMode,
        onCreated: (created: List<IScheduleModel>) -> Unit
    ) {
        DetailsFragment().apply {
            taskModel = DailyTaskModel(
                beginTime = beginTime ?: nowMillis
                    .adjustTimestamp(quarterMillis, true),
                duration = duration ?: (hourMillis / 2),
                title = title ?: "",
                repeatMode = repeatMode,
                editingTaskModel = EditingTaskModel()
            )
            onSaveBlock = {
                onCreated.invoke(it)
            }
        }.show(fragmentManager, "DetailsFragment")
    }


    private val String.calendarScheduleTitle
        get() = "HmSchedule: $this"

    companion object {
        private const val TAG = "DailyTaskBusinessImpl"
        private var dailyTaskHolder = DailyTaskHolder(Long.MAX_VALUE, Long.MIN_VALUE, emptyList())
    }
}

data class DailyTaskHolder(
    override val beginTime: Long,
    override val endTime: Long,
    override val schedules: List<IScheduleModel>
) : IScheduleModel, IScheduleModelHolder
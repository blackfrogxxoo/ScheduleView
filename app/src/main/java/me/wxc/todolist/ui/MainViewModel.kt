package me.wxc.todolist.ui

import androidx.lifecycle.ViewModel
import com.stonesx.datasource.repository.DailyTaskRepository
import com.stonesx.datasource.repository.RepositoryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.wxc.todolist.data.DailyTaskLocal
import me.wxc.widget.base.ISchedulerModel
import me.wxc.widget.base.RepeatMode
import me.wxc.widget.base.RepeatMode.Companion.parseLocalRepeatMode
import me.wxc.widget.scheduler.components.CreateTaskModel
import me.wxc.widget.scheduler.components.DailyTaskModel
import java.util.*

class MainViewModel : ViewModel() {
    suspend fun getDailyTasks(adapterModels: MutableList<ISchedulerModel>) {
        return withContext(Dispatchers.IO) {
            RepositoryManager.getInstance().findRepository(DailyTaskRepository::class.java)
                .getAllDailyTasks().map {
                    DailyTaskModel(
                        id = it.id,
                        startTime = it.startTime,
                        duration = it.endTime - it.startTime,
                        title = it.title,
                        repeatMode = Pair(it.repeatType, it.repeatInterval).parseLocalRepeatMode,
                        repeatId = it.repeatId
                    )
                }.run {
                    adapterModels.addAll(this)
                }
        }
    }

    suspend fun getRangeDailyTask(
        startTime: Long,
        endTime: Long
    ): List<ISchedulerModel> = withContext(Dispatchers.IO) {
        RepositoryManager.getInstance().findRepository(
            DailyTaskRepository::class.java
        )
            .getRangeDailyTasks(startTime, endTime).map {
                DailyTaskModel(
                    id = it.id,
                    startTime = it.startTime,
                    duration = it.endTime - it.startTime,
                    title = it.title,
                    repeatMode = Pair(it.repeatType, it.repeatInterval).parseLocalRepeatMode,
                    repeatId = it.repeatId
                )
            }
    }

    suspend fun removeDailyTask(
        model: DailyTaskModel,
        adapterModels: MutableList<ISchedulerModel>
    ) {
        withContext(Dispatchers.IO) {
            if (model.repeatMode == RepeatMode.Never) {
                RepositoryManager.getInstance().findRepository(DailyTaskRepository::class.java)
                    .removeById(model.id)
                adapterModels.remove(model)
            } else {
                RepositoryManager.getInstance().findRepository(DailyTaskRepository::class.java)
                    .removeByRepeatId(model.repeatId)
                adapterModels.removeAll(adapterModels.filter { (it as? DailyTaskModel)?.repeatId == model.repeatId })
            }
        }
    }

    suspend fun updateDailyTask(
        model: DailyTaskModel,
        adapterModels: MutableList<ISchedulerModel>
    ) {
        withContext(Dispatchers.IO) {
            RepositoryManager.getInstance().findRepository(DailyTaskRepository::class.java)
                .putDailyTask(
                    DailyTaskLocal(
                        id = model.id,
                        startTime = model.startTime,
                        endTime = model.endTime,
                        title = model.title,
                        repeatType = model.repeatMode.repeatModeInt,
                        repeatInterval = model.repeatMode.repeatInterval,
                        repeatId = model.repeatId
                    )
                )
            adapterModels.remove(model)
            adapterModels.add(model)
        }
    }

    suspend fun saveCreateDailyTask(
        model: CreateTaskModel,
        adapterModels: MutableList<ISchedulerModel>
    ) {
        withContext(Dispatchers.Default) {
            if (model.title.isBlank()) {
                model.title = "(无主题)"
            }
            when (model.repeatMode) {
                RepeatMode.Never -> {
                    createSingleTask(model, adapterModels)
                }
                else -> {
                    performSaveRepeatableTask(model, adapterModels)
                }
            }
        }
    }

    private fun createSingleTask(
        model: CreateTaskModel,
        adapterModels: MutableList<ISchedulerModel>,
    ) {
        val repeatId = UUID.randomUUID().toString()
        val id = RepositoryManager.getInstance()
            .findRepository(DailyTaskRepository::class.java)
            .putDailyTask(
                DailyTaskLocal(
                    startTime = model.startTime,
                    endTime = model.endTime,
                    title = model.title,
                    repeatId = repeatId,
                    repeatInterval = model.repeatMode.repeatInterval,
                    repeatType = model.repeatMode.repeatModeInt
                )
            )
        DailyTaskModel(
            id = id,
            startTime = model.startTime,
            duration = model.duration,
            title = model.title,
            repeatId = repeatId,
            repeatMode = model.repeatMode
        ).apply {
            adapterModels.add(this)
        }
    }

    private fun performSaveRepeatableTask(
        model: CreateTaskModel,
        adapterModels: MutableList<ISchedulerModel>
    ) {
        val repeatId = UUID.randomUUID().toString()
        val locals = (0..2000).map {
            model.repeatMode.repeatStartTimeByIndex(model.startTime, it)
        }.filter {
            model.repeatMode.repeatedModelValid(it, model.startTime)
        }.map {
            DailyTaskLocal(
                startTime = it,
                endTime = it + model.duration,
                title = model.title,
                repeatId = repeatId,
                repeatInterval = model.repeatMode.repeatInterval,
                repeatType = model.repeatMode.repeatModeInt
            )
        }
        val ids = RepositoryManager.getInstance()
            .findRepository(DailyTaskRepository::class.java)
            .putDailyTasks(locals)
        locals.mapIndexed { index, local ->
            DailyTaskModel(
                id = ids[index],
                startTime = local.startTime,
                duration = model.duration,
                title = model.title,
                repeatId = repeatId,
                repeatMode = model.repeatMode
            )
        }.apply {
            adapterModels.addAll(this)
        }
    }
}
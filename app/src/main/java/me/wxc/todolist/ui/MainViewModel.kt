package me.wxc.todolist.ui

import androidx.lifecycle.ViewModel
import com.stonesx.datasource.repository.DailyTaskRepository
import com.stonesx.datasource.repository.RepositoryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.wxc.todolist.data.DailyTaskLocal
import me.wxc.widget.ICalendarModel
import me.wxc.widget.components.ClockLineModel
import me.wxc.widget.components.CreateTaskModel
import me.wxc.widget.components.DailyTaskModel

class MainViewModel : ViewModel() {
    suspend fun getDailyTasks(adapterModels: MutableList<ICalendarModel>) {
        return withContext(Dispatchers.IO) {
            RepositoryManager.getInstance().findRepository(DailyTaskRepository::class.java)
                .getAllDailyTasks().map {
                    DailyTaskModel(
                        id = it.id,
                        startTime = it.startTime,
                        duration = it.endTime - it.startTime,
                        title = it.title
                    )
                }.run {
                    adapterModels.addAll(this)
                }
        }
    }

    suspend fun removeDailyTask(model: DailyTaskModel, adapterModels: MutableList<ICalendarModel>) {
        withContext(Dispatchers.IO) {
            RepositoryManager.getInstance().findRepository(DailyTaskRepository::class.java).removeById(model.id)
            adapterModels.remove(model)
        }
    }

    suspend fun saveCreateDailyTask(model: CreateTaskModel, adapterModels: MutableList<ICalendarModel>) {
        withContext(Dispatchers.IO) {
            val id = RepositoryManager.getInstance()
                .findRepository(DailyTaskRepository::class.java)
                .putDailyTask(
                    DailyTaskLocal(
                        startTime = model.startTime,
                        endTime = model.endTime,
                        title = model.title,
                    )
                )
            adapterModels.remove(model)
            adapterModels.filterIsInstance<ClockLineModel>().forEach {
                it.createTaskModel = null
            }
            DailyTaskModel(
                id = id,
                startTime = model.startTime,
                duration = model.duration,
                title = model.title
            ).apply {
                adapterModels.add(this)
            }
        }
    }
}
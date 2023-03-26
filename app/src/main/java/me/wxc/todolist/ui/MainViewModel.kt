package me.wxc.todolist.ui

import androidx.lifecycle.ViewModel
import me.wxc.todolist.business.DailyTaskBusinessImpl
import me.wxc.widget.base.DailyTaskModel
import me.wxc.widget.base.IScheduleModel

class MainViewModel : ViewModel() {
    private val business by lazy { DailyTaskBusinessImpl() }

    suspend fun getRangeDailyTask(
        beginTime: Long,
        endTime: Long
    ): List<IScheduleModel> = business.getDailyTasks(beginTime, endTime)

    suspend fun removeDailyTask(
        model: DailyTaskModel,
        deleteOption: DeleteOptionFragment.DeleteOption
    ): List<Long> = business.removeDailyTasks(model, deleteOption)

    suspend fun updateSingleDailyTask(
        model: DailyTaskModel
    ): List<Long> = business.updateDailyTasks(model, UpdateOptionFragment.UpdateOption.ONE)

    suspend fun saveCreateDailyTask(
        model: DailyTaskModel,
        adapterModels: MutableList<IScheduleModel>
    ) = business.saveCreateDailyTask(model) {
        adapterModels.addAll(it)
    }
}
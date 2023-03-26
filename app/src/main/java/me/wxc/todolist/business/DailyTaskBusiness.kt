package me.wxc.todolist.business

import androidx.fragment.app.FragmentManager
import me.wxc.todolist.ui.DeleteOptionFragment
import me.wxc.todolist.ui.UpdateOptionFragment
import me.wxc.widget.base.DailyTaskModel
import me.wxc.widget.base.IScheduleModel
import me.wxc.widget.base.RepeatMode

interface DailyTaskBusiness {
    suspend fun saveCreateDailyTask(
        model: DailyTaskModel,
        block: (created: List<IScheduleModel>) -> Unit
    )

    suspend fun getDailyTasks(beginTime: Long, endTime: Long): List<IScheduleModel>

    suspend fun removeDailyTasks(
        model: DailyTaskModel,
        option: DeleteOptionFragment.DeleteOption,
    ) : List<Long>

    suspend fun updateDailyTasks(
        model: DailyTaskModel,
        option: UpdateOptionFragment.UpdateOption,
    ) : List<Long>

    fun showCreateFragment(
        fragmentManager: FragmentManager,
        beginTime: Long? = null,
        duration: Long? = null,
        title: String? = null,
        repeatMode: RepeatMode = RepeatMode.Never,
        onCreated: (created: List<IScheduleModel>) -> Unit = {}
    )
}
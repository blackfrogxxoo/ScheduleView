package me.wxc.todolist.ui

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutParams
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.stonesx.datasource.repository.DailyTaskRepository
import com.stonesx.datasource.repository.RepositoryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.wxc.widget.base.ISchedulerModel
import me.wxc.widget.calender.MonthView
import me.wxc.widget.scheduler.components.DailyTaskModel
import me.wxc.widget.tools.TAG
import me.wxc.widget.tools.startOfDay
import java.util.Calendar

class MonthAdapter(private val lifecycleScope: LifecycleCoroutineScope) : RecyclerView.Adapter<VH>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(parent.context)
    }

    override fun getItemCount() = 1200

    override fun onBindViewHolder(holder: VH, position: Int) {
        val monthView = holder.itemView as MonthView
        monthView.calendar.timeInMillis = startOfDay(0L).apply {
            add(Calendar.MONTH, position)
        }.timeInMillis
        monthView.requestLayout()

        lifecycleScope.launch {
            val list = getDailyTasks(monthView.startTime, monthView.endTime)
            Log.i(monthView.TAG, "$list")
            monthView.schedulerModels = list
        }
    }


    private suspend fun getDailyTasks(startTime: Long, endTime: Long): List<ISchedulerModel> {
        return withContext(Dispatchers.IO) {
            RepositoryManager.getInstance().findRepository(DailyTaskRepository::class.java)
                .getRangeDailyTasks(startTime, endTime).map {
                    DailyTaskModel(
                        id = it.id,
                        startTime = it.startTime,
                        duration = it.endTime - it.startTime,
                        title = it.title
                    )
                }
        }
    }
}

class VH(context: Context) : ViewHolder(MonthView(context).apply {
    layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
}) {

}
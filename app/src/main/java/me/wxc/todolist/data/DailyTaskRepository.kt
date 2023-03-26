package me.wxc.todolist.data

import androidx.room.Room.databaseBuilder
import me.wxc.todolist.App

class DailyTaskRepository {
    private val dbDataSource by lazy {
        databaseBuilder(App.self, DailyTaskDatabase::class.java, "daily_task").build()
    }

    fun putDailyTask(task: DailyTaskLocal): Long =
        dbDataSource.getDailyTaskDao().putDailyTask(task)

    fun putDailyTasks(task: List<DailyTaskLocal>): List<Long> =
        dbDataSource.getDailyTaskDao().putDailyTasks(task)

    fun getAllDailyTasks(): List<DailyTaskLocal> =
        dbDataSource.getDailyTaskDao().getAllDailyTasks()

    fun getRangeDailyTasks(beginTime: Long, endTime: Long): List<DailyTaskLocal> =
        dbDataSource.getDailyTaskDao().getRangeDailyTasks(beginTime, endTime)

    fun removeById(id: Long) = dbDataSource.getDailyTaskDao().removeById(id)

    fun removeByRepeatId(repeatId: String, fromId: Long) =
        dbDataSource.getDailyTaskDao().removeByRepeatId(repeatId, fromId)

    fun getRepeatDailyTasks(repeatId: String): List<DailyTaskLocal> =
        dbDataSource.getDailyTaskDao().getRepeatDailyTasks(repeatId)

}

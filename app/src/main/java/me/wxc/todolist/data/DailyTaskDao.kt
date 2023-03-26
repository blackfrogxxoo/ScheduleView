package me.wxc.todolist.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DailyTaskDao {
    @Query("SELECT * FROM daily_task")
    fun getAllDailyTasks(): List<DailyTaskLocal>

    @Query("SELECT * FROM daily_task where beginTime >= :beginTime and endTime <= :endTime")
    fun getRangeDailyTasks(beginTime: Long, endTime: Long): List<DailyTaskLocal>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun putDailyTask(task: DailyTaskLocal) : Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun putDailyTasks(task: List<DailyTaskLocal>) : List<Long>

    @Query("DELETE FROM daily_task WHERE id = :id")
    fun removeById(id: Long)

    @Query("DELETE FROM daily_task WHERE repeatId = :repeatId AND id >= :fromId")
    fun removeByRepeatId(repeatId: String, fromId: Long)

    @Query("SELECT * FROM daily_task where repeatId = :repeatId")
    fun getRepeatDailyTasks(repeatId: String): List<DailyTaskLocal>
}
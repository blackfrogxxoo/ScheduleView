package me.wxc.todolist.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import me.wxc.widget.base.DailyTaskModel
import me.wxc.widget.base.RemindMode.Companion.parseLocalRemindMode
import me.wxc.widget.base.RepeatMode.Companion.parseLocalRepeatMode

@Entity(tableName = "daily_task")
data class DailyTaskLocal(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val beginTime: Long,
    val endTime: Long,
    val title: String,
    val repeatId: String,
    val repeatType: Int = 0,
    val repeatInterval: Int = 0,
    val calendarEventId: Long = 0,
    val remindMinutes: Int = -1
) {
    val mapModel: DailyTaskModel
        get() = DailyTaskModel(
            id = id,
            beginTime = beginTime,
            duration = endTime - beginTime,
            title = title,
            repeatMode = Pair(repeatType, repeatInterval).parseLocalRepeatMode,
            repeatId = repeatId,
            calendarEventId = calendarEventId,
            remindMode = remindMinutes.parseLocalRemindMode
        )
}
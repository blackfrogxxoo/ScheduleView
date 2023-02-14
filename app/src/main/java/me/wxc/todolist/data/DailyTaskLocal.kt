package me.wxc.todolist.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_task")
data class DailyTaskLocal(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: Long,
    val endTime: Long,
    val title: String,
)
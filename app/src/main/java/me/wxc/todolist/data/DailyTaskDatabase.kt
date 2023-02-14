package me.wxc.todolist.data

import androidx.room.Database
import androidx.room.RoomDatabase
import me.wxc.todolist.data.DailyTaskDatabase.Companion.VERSION

@Database(entities = [DailyTaskLocal::class], version = VERSION)
abstract class DailyTaskDatabase : RoomDatabase() {
    companion object {
        const val VERSION = 1
    }

    abstract fun getDailyTaskDao(): DailyTaskDao
}
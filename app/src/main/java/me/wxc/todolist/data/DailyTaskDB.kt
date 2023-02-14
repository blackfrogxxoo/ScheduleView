package me.wxc.todolist.data

import android.app.Application
import androidx.room.RoomDatabase
import com.stonesx.datasource.room.RoomDBServer
import com.stonesx.datasource.room.RoomServerConfig

class DailyTaskDB(name: String, application: Application) : RoomDBServer(name) {
    override val roomServerConfig: RoomServerConfig = DailyTaskServerConfig(application)
}

class DailyTaskServerConfig(app: Application) : RoomServerConfig() {
    override val application: Application = app
    override val name: String = "dailyTask"
    override val version: Int = DailyTaskDatabase.VERSION

    override fun <T : RoomDatabase> getTarget(): Class<T> {
        return DailyTaskDatabase::class.java as Class<T>
    }
}
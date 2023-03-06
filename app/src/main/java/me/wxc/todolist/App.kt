package me.wxc.todolist

import android.app.Application
import com.stonesx.datasource.db.SimpleDBServerManager
import com.stonesx.datasource.repository.RepositoryContext
import com.stonesx.datasource.room.RoomDataSourceFactory
import me.wxc.todolist.data.DailyTaskDB
import me.wxc.widget.ScheduleConfig

class App : Application() {


    override fun onCreate() {
        super.onCreate()
        self = this
        ScheduleConfig.app = self
        RepositoryContext.getInstance().capacityInitialize(0, 20)
        RepositoryContext.getInstance().dbInitialize(
            dbServerManager = {
                SimpleDBServerManager(
                    "dailyTask",
                    DailyTaskDB("dailyTask", this)
                )
            },
            dbDataSourceFactory = { RoomDataSourceFactory() }
        )
    }

    companion object {
        lateinit var self: Application
            private set
    }
}
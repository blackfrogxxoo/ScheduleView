package me.wxc.todolist

import android.app.Application
import me.wxc.widget.ScheduleConfig

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        self = this
        ScheduleConfig.app = self
    }

    companion object {
        lateinit var self: Application
            private set
    }
}
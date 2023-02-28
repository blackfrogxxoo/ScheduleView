package me.wxc.todolist.data

import android.app.Application
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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

    private val MIGRATION_1_2: Migration = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE daily_task ADD COLUMN repeatId TEXT NOT NULL")
            database.execSQL("ALTER TABLE daily_task ADD COLUMN repeatType INTEGER NOT NULL")
            database.execSQL("ALTER TABLE daily_task ADD COLUMN repeatInterval INTEGER NOT NULL")
        }
    }

    override val migration: Array<Migration> = arrayOf(
        MIGRATION_1_2
    )

}
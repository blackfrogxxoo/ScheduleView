package me.wxc.todolist.tools

import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import me.wxc.widget.ScheduleConfig

object Preferences {
    private const val namespace = "more"
    private val preferences: SharedPreferences
        get() = ScheduleConfig.app.getSharedPreferences(namespace, Context.MODE_PRIVATE)
    private val editor: Editor
        get() = preferences.edit()

    fun putBoolean(key: String, value: Boolean) {
        editor.putBoolean(key, value).apply()
    }

    fun getBoolean(key: String, default: Boolean = false) = preferences.getBoolean(key, default)
}
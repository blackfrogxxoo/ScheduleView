package me.wxc.todolist.tools

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.provider.CalendarContract
import android.provider.CalendarContract.Events
import android.provider.CalendarContract.Reminders
import android.util.Log
import java.util.*


object CalendarEvents {
    private const val TAG = "CalendarEvents"
    private const val CALENDER_URL = "content://com.android.calendar/calendars"
    private const val CALENDER_EVENT_URL = "content://com.android.calendar/events"
    private const val CALENDER_REMINDER_URL = "content://com.android.calendar/reminders"
    private const val CALENDARS_NAME = "Todolist"
    private const val CALENDARS_ACCOUNT_NAME = "tester"
    private const val CALENDARS_ACCOUNT_TYPE = "me.wxc.todolist"
    private const val CALENDARS_DISPLAY_NAME = "Todolist账户"

    /**
     * 检查是否已经添加了日历账户，如果没有添加先添加一个日历账户再查询
     * 获取账户成功返回账户id，否则返回-1
     */
    private fun checkAndAddCalendarAccount(context: Context): Int {
        val oldId = checkCalendarAccount(context)
        return if (oldId >= 0) {
            oldId
        } else {
            val addId = addCalendarAccount(context)
            if (addId >= 0) {
                checkCalendarAccount(context)
            } else {
                -1
            }
        }
    }

    /**
     * 检查是否存在现有账户，存在则返回账户id，否则返回-1
     */
    private fun checkCalendarAccount(context: Context): Int {
        val userCursor =
            context.contentResolver.query(Uri.parse(CALENDER_URL), null, null, null, null)
        return userCursor?.use { userCursor ->
            val count: Int = userCursor.count
            if (count > 0) { //存在现有账户，取第一个账户的id返回
                userCursor.moveToFirst()
                userCursor.getInt(userCursor.getColumnIndex(CalendarContract.Calendars._ID))
            } else {
                -1
            }
        } ?: -1
    }

    /**
     * 添加日历账户，账户创建成功则返回账户id，否则返回-1
     */
    private fun addCalendarAccount(context: Context): Long {
        val timeZone: TimeZone = TimeZone.getDefault()
        val value = ContentValues()
        value.put(CalendarContract.Calendars.NAME, CALENDARS_NAME)
        value.put(
            CalendarContract.Calendars.ACCOUNT_NAME,
            CALENDARS_ACCOUNT_NAME
        )
        value.put(
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CALENDARS_ACCOUNT_TYPE
        )
        value.put(
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CALENDARS_DISPLAY_NAME
        )
        value.put(CalendarContract.Calendars.VISIBLE, 1)
        value.put(CalendarContract.Calendars.CALENDAR_COLOR, Color.BLUE)
        value.put(
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
            CalendarContract.Calendars.CAL_ACCESS_OWNER
        )
        value.put(CalendarContract.Calendars.SYNC_EVENTS, 1)
        value.put(CalendarContract.Calendars.CALENDAR_TIME_ZONE, timeZone.getID())
        value.put(
            CalendarContract.Calendars.OWNER_ACCOUNT,
            CALENDARS_ACCOUNT_NAME
        )
        value.put(CalendarContract.Calendars.CAN_ORGANIZER_RESPOND, 0)
        var calendarUri: Uri = Uri.parse(CALENDER_URL)
        calendarUri = calendarUri.buildUpon()
            .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
            .appendQueryParameter(
                CalendarContract.Calendars.ACCOUNT_NAME,
                CALENDARS_ACCOUNT_NAME
            )
            .appendQueryParameter(
                CalendarContract.Calendars.ACCOUNT_TYPE,
                CALENDARS_ACCOUNT_TYPE
            )
            .build()
        val result = context.contentResolver.insert(calendarUri, value)
        return if (result == null) -1 else ContentUris.parseId(result)
    }

    /**
     * 添加日历事件
     */
    fun addCalendarEvent(
        context: Context,
        title: String,
        description: String?,
        beginTime: Long,
        endTime: Long,
        aheadMinutes: Int
    ): Long {
        val calId = checkAndAddCalendarAccount(context) //获取日历账户的id
        if (calId < 0) { //获取账户id失败直接返回，添加日历事件失败
            return -1
        }
        if (aheadMinutes < 0) {
            return -1
        }

        //添加日历事件
        val event = ContentValues()
        event.put(Events.TITLE, title)
        event.put(Events.DESCRIPTION, description)
        event.put(Events.CALENDAR_ID, calId) //插入账户的id
        event.put(Events.DTSTART, beginTime)
        event.put(Events.DTEND, endTime)
        event.put(Events.HAS_ALARM, 1) //设置有闹钟提醒
        event.put(Events.EVENT_TIMEZONE, "Asia/Shanghai") //这个是时区，必须有
        val newEvent =
            context.contentResolver.insert(Uri.parse(CALENDER_EVENT_URL), event) ?: return -1

        val eventId = ContentUris.parseId(newEvent)
        //事件提醒的设定
        val values = ContentValues()
        values.put(Reminders.EVENT_ID, eventId)
        values.put(Reminders.MINUTES, aheadMinutes) // 提前N分钟
        values.put(Reminders.METHOD, Reminders.METHOD_ALERT)
        val uri =
            context.contentResolver.insert(Uri.parse(CALENDER_REMINDER_URL), values) ?: return -1
        Log.i(TAG, "addCalendarEvent: ${ContentUris.parseId(newEvent)}, $uri, $newEvent")
        return eventId
    }

    /**
     * 删除日历事件
     */
    fun deleteCalendarEvent(context: Context, eventId: Long): Boolean {
        if (eventId <= 0) return false
        val eventCursor = context.contentResolver
            .query(Uri.parse(CALENDER_EVENT_URL), null, null, null, null)
        eventCursor?.use {
            val deleteUri = ContentUris.withAppendedId(Uri.parse(CALENDER_EVENT_URL), eventId)
            Log.i(TAG, "deleteCalendarEvent: $eventId, $deleteUri")
            val row = context.contentResolver.delete(deleteUri, null, null)
            it.close()
            return row != -1
        }
        return false
    }

    /**
     * 更新日历事件
     */
    fun updateCalendarEvent(
        context: Context,
        eventId: Long,
        title: String,
        description: String?,
        beginTime: Long,
        endTime: Long,
        aheadMinutes: Int,
    ): Long {
        deleteCalendarEvent(context, eventId)
        return addCalendarEvent(context, title, description, beginTime, endTime, aheadMinutes)
    }
}
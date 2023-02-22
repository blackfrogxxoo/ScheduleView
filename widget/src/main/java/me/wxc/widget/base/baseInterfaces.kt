package me.wxc.widget.base

interface ITimeRangeHolder {
    val startTime: Long
    val endTime: Long
}

interface ISelectedDayTimeHolder {
    var selectedDayTime: Long
}
package me.wxc.widget.tools

val dayWidth: Float
    get() = if (CalendarWidget.isThreeDay) (screenWidth - clockWidth) / 3 else (screenWidth - clockWidth)
package me.wxc.widget.base

interface ISelectedTimeObserver {
    fun onSelectedTime(time: Long = System.currentTimeMillis())
}
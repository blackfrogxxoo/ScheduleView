package me.wxc.widget.tools

import android.graphics.Canvas
import android.graphics.Paint

fun Canvas.drawText(text: String, x: Float, y: Float, paint: Paint, maxWidth: Float) {
    val start = 0
    var end = text.length
    while (end >= start && paint.measureText(text, start, end) > maxWidth) {
        end --
    }
    drawText(text, 0, end, x, y, paint)
}
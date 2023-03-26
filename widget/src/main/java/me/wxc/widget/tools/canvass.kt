package me.wxc.widget.tools

import android.graphics.Canvas
import android.graphics.Paint

fun Canvas.drawText(text: String, x: Float, y: Float, paint: Paint, maxWidth: Float, autoFeed: Boolean = true) {
    val start = 0
    var end = text.length
    while (end >= start && paint.measureText(text, start, end) > maxWidth) {
        end --
    }
    drawText(text, 0, end, x, y, paint)
    if (autoFeed && end < text.length) {
        drawText(text.subSequence(end, text.length).toString(), x, y + paint.textSize + 2f.dp, paint, maxWidth)
    }
}
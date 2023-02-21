package me.wxc.widget.calender

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import me.wxc.widget.tools.dp
import me.wxc.widget.tools.screenWidth

class NowLineView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val paint by lazy { Paint(Paint.ANTI_ALIAS_FLAG) }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(screenWidth, 8.dp)
    }

    override fun onDraw(canvas: Canvas) {
        paint.color = Color.RED
        paint.strokeWidth = 1f.dp
        canvas.drawLine(
            20f.dp,
            measuredHeight / 2f,
            measuredWidth - 12f.dp,
            measuredHeight / 2f,
            paint
        )
        canvas.drawCircle(16f.dp, measuredHeight / 2f, 3f.dp, paint)
    }
}
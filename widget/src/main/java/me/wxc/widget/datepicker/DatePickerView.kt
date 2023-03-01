package me.wxc.widget.datepicker

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup

class DatePickerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr), IDatePicker {
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {

    }
}
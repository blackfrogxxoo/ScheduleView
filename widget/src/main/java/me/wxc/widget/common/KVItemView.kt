package me.wxc.widget.common

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.children
import me.wxc.widget.R

class KVItemView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : ConstraintLayout(context, attrs) {
    private val key: TextView
    private val value: TextView
    private val rightArrow: ImageView
    private val disabled: View

    init {
        inflate(context, R.layout.view_kv_item, this)
        key = findViewById(R.id.key)
        value = findViewById(R.id.value)
        rightArrow = findViewById(R.id.rightArrow)
        disabled = findViewById(R.id.disabled)
        disabled.setOnClickListener { /* do nothing */ }
    }

    fun setKV(key: String, value: String) {
        this@KVItemView.key.text = key
        this@KVItemView.value.text = value
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        disabled.visibility = if (enabled) GONE else VISIBLE
        rightArrow.visibility = if (enabled) VISIBLE else GONE
        children.forEach {
            it.alpha = if (enabled) 1f else 0.5f
        }
    }
}
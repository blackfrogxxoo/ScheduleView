package me.wxc.todolist.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup.LayoutParams
import androidx.core.view.ViewCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import me.wxc.widget.R
import me.wxc.todolist.databinding.FragmentRepeatModeBinding
import me.wxc.todolist.tools.argument
import me.wxc.widget.base.RemindMode
import me.wxc.widget.base.RepeatMode
import me.wxc.widget.tools.dp

class RemindModeFragment : DialogFragment() {
    private lateinit var binding: FragmentRepeatModeBinding
    private var remindMode: RemindMode by argument()
    private var onSelected: (mode: RemindMode) -> Unit = {}
    private val remindModeList = listOf(
        RemindMode.Never,
        RemindMode.OnTime,
        RemindMode.FiveMinutes,
        RemindMode.HalfHour,
        RemindMode.OneHour,
        RemindMode.OneDay,
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        if (!::binding.isInitialized) {
            binding = FragmentRepeatModeBinding.inflate(inflater)
            remindModeList.map { it.generateRadioButton() }.forEach {
                binding.radioGroup.addView(it)
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.title.text = "设置提醒"
        binding.radioGroup.setOnCheckedChangeListener { group, checkedId ->
            val mode = group.findViewById<RadioButton>(checkedId).tag as RemindMode
            onSelected.invoke(mode)
            dismissAllowingStateLoss()
        }
        binding.cancel.setOnClickListener {
            dismissAllowingStateLoss()
        }
    }

    private fun RemindMode.generateRadioButton(): RadioButton {
        return RadioButton(requireContext()).apply {
            layoutParams = LayoutParams(
                300.dp,
                LayoutParams.WRAP_CONTENT
            )
            text = showText
            id = ViewCompat.generateViewId()
            tag = this@generateRadioButton
            isChecked = remindMode == this@generateRadioButton
            setTextColor(context.getColor(R.color.black1))
        }
    }

    companion object {
        private const val TAG = "RemindModeFragment"
        fun show(fragment: Fragment, mode: RemindMode, onSelected: (mode: RemindMode) -> Unit) {
            RemindModeFragment().apply {
                remindMode = mode
                this.onSelected = onSelected
            }.show(fragment.childFragmentManager, TAG)
        }
    }
}
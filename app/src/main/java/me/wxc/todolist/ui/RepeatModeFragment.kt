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
import me.wxc.widget.base.RepeatMode
import me.wxc.widget.tools.dp

class RepeatModeFragment : DialogFragment() {
    private lateinit var binding: FragmentRepeatModeBinding
    private var repeatMode: RepeatMode by argument()
    private var onSelected: (mode: RepeatMode) -> Unit = {}
    private val repeatModeList = listOf(
        RepeatMode.Never,
        RepeatMode.Day,
        RepeatMode.WorkDay,
        RepeatMode.Week,
        RepeatMode.Month,
        RepeatMode.Year,
        RepeatMode.Custom(3),
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        if (!::binding.isInitialized) {
            binding = FragmentRepeatModeBinding.inflate(inflater)
            repeatModeList.map { it.generateRadioButton() }.forEach {
                binding.radioGroup.addView(it)
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.title.text = "设置重复"
        binding.radioGroup.setOnCheckedChangeListener { group, checkedId ->
            val mode = group.findViewById<RadioButton>(checkedId).tag as RepeatMode
            onSelected.invoke(mode)
            // TODO 自定义重复需要设置间隔天数
            dismissAllowingStateLoss()
        }
        binding.cancel.setOnClickListener {
            dismissAllowingStateLoss()
        }
    }

    private fun RepeatMode.generateRadioButton(): RadioButton {
        return RadioButton(requireContext()).apply {
            layoutParams = LayoutParams(
                300.dp,
                LayoutParams.WRAP_CONTENT
            )
            text = name
            id = ViewCompat.generateViewId()
            tag = this@generateRadioButton
            isChecked = repeatMode.repeatModeInt == repeatModeInt
            setTextColor(context.getColor(R.color.black1))
        }
    }

    companion object {
        private const val TAG = "RepeatModeFragment"
        fun show(fragment: Fragment, mode: RepeatMode, onSelected: (mode: RepeatMode) -> Unit) {
            RepeatModeFragment().apply {
                repeatMode = mode
                this.onSelected = onSelected
            }.show(fragment.childFragmentManager, TAG)
        }
    }
}
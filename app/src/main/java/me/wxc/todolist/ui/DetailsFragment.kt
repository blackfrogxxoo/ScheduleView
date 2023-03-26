package me.wxc.todolist.ui

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.bigkoo.pickerview.R
import com.bigkoo.pickerview.builder.TimePickerBuilder
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import me.wxc.todolist.ui.RemindModeFragment
import me.wxc.todolist.ui.RepeatModeFragment
import me.wxc.todolist.ui.UpdateOptionFragment
import me.wxc.todolist.databinding.FragmentDetailsBinding
import me.wxc.todolist.tools.argument
import me.wxc.widget.base.IScheduleModel
import me.wxc.widget.base.RepeatMode
import me.wxc.widget.base.DailyTaskModel
import me.wxc.widget.tools.*
import kotlinx.coroutines.launch


class DetailsFragment : BottomSheetDialogFragment() {
    private val mainViewModel by viewModels<MainViewModel>()
    private lateinit var binding: FragmentDetailsBinding
    var taskModel: IScheduleModel by argument()
    var onSaveBlock: (model: List<IScheduleModel>) -> Unit = {}
    var onDeleteBlock: (ids: List<Long>) -> Unit = { ids -> }
    private lateinit var lastRepeatMode: RepeatMode
    private val timePicker by lazy {
        val picker = TimePickerBuilder(context) { date, v ->
            Log.i("pvTime", "onTimeSelect")
            if (v == binding.beginTime) {
                (taskModel as? DailyTaskModel)?.changeBeginTime(date.time)
                binding.beginTime.setKV("开始时间", taskModel.beginTime.HHmm)
                binding.endTime.setKV("结束时间", taskModel.endTime.HHmm)
            } else {
                (taskModel as? DailyTaskModel)?.changeEndTime(date.time)
                binding.beginTime.setKV("开始时间", taskModel.beginTime.HHmm)
                binding.endTime.setKV("结束时间", taskModel.endTime.HHmm)
            }
        }
            .setType(booleanArrayOf(false, false, false, true, true, false))
            .isDialog(true) //默认设置false ，内部实现将DecorView 作为它的父控件。
            .addOnCancelClickListener { Log.i("pvTime", "onCancelClickListener") }
            .setItemVisibleCount(5) //若设置偶数，实际值会加1（比如设置6，则最大可见条目为7）
            .setLineSpacingMultiplier(4.0f)
            .isAlphaGradient(false)
            .build()

        val mDialog: Dialog = picker.dialog
        val params: FrameLayout.LayoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM
        )
        params.leftMargin = 0
        params.rightMargin = 0
        picker.dialogContainerLayout.layoutParams = params
        mDialog.window?.apply {
            setWindowAnimations(R.style.picker_view_slide_anim) //修改动画样式
            setGravity(Gravity.BOTTOM) //改成Bottom,底部显示
            setDimAmount(0.3f)
        }
        picker
    }

    private val datePicker by lazy {
        val picker = TimePickerBuilder(context) { date, v ->
            Log.i("pvTime", "onTimeSelect")
            val dBeginTime = taskModel.beginTime - beginOfDay(taskModel.beginTime).timeInMillis
            val dEndTime = taskModel.endTime - beginOfDay(taskModel.endTime).timeInMillis
            (taskModel as? DailyTaskModel)?.changeBeginTime(
                beginOfDay(date.time).timeInMillis + dBeginTime
            )
            (taskModel as? DailyTaskModel)?.changeEndTime(
                beginOfDay(date.time).timeInMillis + dEndTime
            )
            binding.date.setKV("日程日期", taskModel.beginTime.yyyyMd)
        }
            .setType(booleanArrayOf(true, true, true, false, false, false))
            .isDialog(true) //默认设置false ，内部实现将DecorView 作为它的父控件。
            .addOnCancelClickListener { Log.i("pvTime", "onCancelClickListener") }
            .setItemVisibleCount(5) //若设置偶数，实际值会加1（比如设置6，则最大可见条目为7）
            .setLineSpacingMultiplier(4.0f)
            .isAlphaGradient(false)
            .build()

        val mDialog: Dialog = picker.dialog
        val params: FrameLayout.LayoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM
        )
        params.leftMargin = 0
        params.rightMargin = 0
        picker.dialogContainerLayout.layoutParams = params
        mDialog.window?.apply {
            setWindowAnimations(R.style.picker_view_slide_anim) //修改动画样式
            setGravity(Gravity.BOTTOM) //改成Bottom,底部显示
            setDimAmount(0.3f)
        }
        picker
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        if (!::binding.isInitialized) {
            binding = FragmentDetailsBinding.inflate(inflater)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(TAG, "onViewCreated: $taskModel")
        val taskModel = taskModel as DailyTaskModel
        lastRepeatMode = taskModel.repeatMode
        binding.delete.visibility =
            if (taskModel.editingTaskModel != null) View.GONE else View.VISIBLE
        binding.title.setText((taskModel as? DailyTaskModel)?.title ?: "")
        binding.title.doOnTextChanged { text, _, _, _ ->
            taskModel.title = text?.toString() ?: "(无主题)"
        }
        binding.repeatMode.setKV("设置重复", taskModel.repeatMode.name)
        binding.remindMode.setKV("设置提醒", taskModel.remindMode.showText)
        binding.date.setKV("设置日期", taskModel.beginTime.yyyyMd)
        binding.beginTime.setKV("开始时间", taskModel.beginTime.HHmm)
        binding.endTime.setKV("结束时间", taskModel.endTime.HHmm)
        binding.date.setOnClickListener {
            datePicker.setDate(taskModel.beginTime.calendar)
            datePicker.show(it)
        }
        binding.beginTime.setOnClickListener {
            timePicker.setDate(taskModel.beginTime.calendar)
            timePicker.show(it)
        }
        binding.endTime.setOnClickListener {
            timePicker.setDate(taskModel.endTime.calendar)
            timePicker.show(it)
        }
        binding.save.setOnClickListener {
            lifecycleScope.launch {
                if (taskModel.duration <= 0) {
                    Toast.makeText(requireContext(), "结束时间必须大于开始时间", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                if (taskModel.editingTaskModel == null) {
                    if (lastRepeatMode !is RepeatMode.Never) {
                        UpdateOptionFragment.show(this@DetailsFragment) {
                            lifecycleScope.launch {
                                var repeatMode = taskModel.repeatMode
                                mainViewModel.removeDailyTask(
                                    taskModel, when (it) {
                                        UpdateOptionFragment.UpdateOption.ONE -> {
                                            repeatMode = RepeatMode.Never
                                            DeleteOptionFragment.DeleteOption.ONE
                                        }
                                        UpdateOptionFragment.UpdateOption.FROM -> DeleteOptionFragment.DeleteOption.FROM
                                        UpdateOptionFragment.UpdateOption.ALL -> DeleteOptionFragment.DeleteOption.ALL
                                    }
                                )
                                val list = mutableListOf<IScheduleModel>()
                                mainViewModel.saveCreateDailyTask(taskModel.copy(repeatMode = repeatMode), list)
                                onSaveBlock.invoke(list)
                                dismissAllowingStateLoss()
                            }

                        }
                    } else {
                        lifecycleScope.launch {
                            mainViewModel.removeDailyTask(
                                taskModel,
                                DeleteOptionFragment.DeleteOption.ONE
                            )
                            val list = mutableListOf<IScheduleModel>()
                            mainViewModel.saveCreateDailyTask(taskModel, list)
                            onSaveBlock.invoke(list)
                            dismissAllowingStateLoss()
                        }
                    }
                    return@launch
                }
                val list = mutableListOf<IScheduleModel>()
                mainViewModel.saveCreateDailyTask(taskModel, list)
                onSaveBlock.invoke(list)
                dismissAllowingStateLoss()
                return@launch
            }
        }
        binding.delete.setOnClickListener {
            if (taskModel.repeatMode !is RepeatMode.Never) {
                DeleteOptionFragment.show(this@DetailsFragment) {
                    (taskModel as? DailyTaskModel)?.let { dailyTaskModel ->
                        lifecycleScope.launch {
                            val ids = mainViewModel.removeDailyTask(dailyTaskModel, it)
                            onDeleteBlock.invoke(ids)
                            dismissAllowingStateLoss()
                        }
                    }
                }
            } else {
                (taskModel as? DailyTaskModel)?.let { dailyTaskModel ->
                    lifecycleScope.launch {
                        val ids = mainViewModel.removeDailyTask(
                            dailyTaskModel,
                            DeleteOptionFragment.DeleteOption.ONE
                        )
                        onDeleteBlock.invoke(ids)
                        dismissAllowingStateLoss()
                    }
                }
            }
        }
        binding.repeatMode.setOnClickListener {
            RepeatModeFragment.show(
                this,
                taskModel.repeatMode,
            ) {
                taskModel.repeatMode = it
                binding.repeatMode.setKV("重复方式：", taskModel.repeatMode.name)
            }
        }
        binding.remindMode.setOnClickListener {
            RemindModeFragment.show(
                this,
                taskModel.remindMode,
            ) {
                taskModel.remindMode = it
                binding.remindMode.setKV("设置提醒", it.showText)
            }
        }
    }

    companion object {
        private const val TAG = "DetailsFragment"
    }
}
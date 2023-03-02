package me.wxc.todolist.ui

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.widget.doOnTextChanged
import com.bigkoo.pickerview.R
import com.bigkoo.pickerview.builder.TimePickerBuilder
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import me.wxc.todolist.databinding.FragmentDetailsBinding
import me.wxc.todolist.tools.argument
import me.wxc.widget.base.IScheduleModel
import me.wxc.widget.base.RepeatMode
import me.wxc.widget.schedule.components.CreateTaskModel
import me.wxc.widget.schedule.components.DailyTaskModel
import me.wxc.widget.tools.TAG
import java.text.SimpleDateFormat
import java.util.*


class DetailsFragment : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentDetailsBinding
    internal var taskModel: IScheduleModel by argument()
    internal var onSaveBlock: (model: IScheduleModel) -> Unit = {}
    internal var onDeleteBlock: (model: IScheduleModel, deleteOption: DeleteOptionFragment.DeleteOption) -> Unit =
        { model, deleteOption -> }
    private val sdf by lazy { SimpleDateFormat("yyyy-MM-dd\nHH:mm") }
    private val timePicker by lazy {
        val picker = TimePickerBuilder(context) { date, v ->
            Log.i("pvTime", "onTimeSelect")
            if (v == binding.startTime) {
                (taskModel as? CreateTaskModel)?.changeStartTime(date.time)
                (taskModel as? DailyTaskModel)?.changeStartTime(date.time)
                binding.startTime.text = sdf.format(taskModel.startTime)
                binding.endTime.text = sdf.format(taskModel.endTime)
            } else {
                (taskModel as? CreateTaskModel)?.changeEndTime(date.time)
                (taskModel as? DailyTaskModel)?.changeEndTime(date.time)
                binding.startTime.text = sdf.format(taskModel.startTime)
                binding.endTime.text = sdf.format(taskModel.endTime)
            }
        }
            .setType(booleanArrayOf(true, true, true, true, true, false))
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
        binding.delete.visibility = if (taskModel is CreateTaskModel) View.GONE else View.VISIBLE
        binding.title.setText(
            (taskModel as? CreateTaskModel)?.title
                ?: (taskModel as? DailyTaskModel)?.title ?: ""
        )
        binding.title.doOnTextChanged { text, _, _, _ ->
            (taskModel as? CreateTaskModel)?.title = text?.toString() ?: "(无主题)"
            (taskModel as? DailyTaskModel)?.title = text?.toString() ?: "(无主题)"
        }
        binding.repeatMode.text = "重复：${taskModel.repeatMode.name}"
        binding.startTime.text = sdf.format(taskModel.startTime)
        binding.endTime.text = sdf.format(taskModel.endTime)
        binding.startTime.setOnClickListener {
            timePicker.setDate(Calendar.getInstance().apply {
                timeInMillis = taskModel.startTime
            })
            timePicker.show(it)
        }
        binding.endTime.setOnClickListener {
            timePicker.setDate(Calendar.getInstance().apply {
                timeInMillis = taskModel.endTime
            })
            timePicker.show(it)
        }
        binding.save.setOnClickListener {
            onSaveBlock.invoke(taskModel)
            dismissAllowingStateLoss()
        }
        binding.delete.setOnClickListener {
            if (taskModel.repeatMode !is RepeatMode.Never) {
                DeleteOptionFragment.show(this@DetailsFragment) {
                    onDeleteBlock.invoke(taskModel, it)
                    dismissAllowingStateLoss()
                }
            } else {
                onDeleteBlock.invoke(taskModel, DeleteOptionFragment.DeleteOption.ONE)
                dismissAllowingStateLoss()
            }
        }
        binding.repeatMode.setOnClickListener {
            RepeatModeFragment.show(
                this,
                taskModel.repeatMode,
            ) {
                (taskModel as? CreateTaskModel)?.repeatMode = it
                binding.repeatMode.text = "重复：${taskModel.repeatMode.name}"
            }
        }
    }

    private val IScheduleModel.repeatMode: RepeatMode
        get() = (this as? DailyTaskModel)?.repeatMode ?: (this as? CreateTaskModel)?.repeatMode
        ?: RepeatMode.Never
}
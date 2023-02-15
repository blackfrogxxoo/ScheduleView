package me.wxc.todolist.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import me.wxc.todolist.databinding.ActivityMainBinding
import me.wxc.widget.ICalendarWidget
import me.wxc.widget.tools.CalendarWidget
import me.wxc.widget.tools.TAG
import me.wxc.widget.tools.dDays
import me.wxc.widget.tools.sdf_yyyyMMddHHmmss
import java.text.SimpleDateFormat

class MainActivity : AppCompatActivity() {
    private val mainViewModel by viewModels<MainViewModel>()
    private lateinit var binding: ActivityMainBinding
    private val sdf_yyyyM = SimpleDateFormat("yyyy年M月")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val calendarWidget = CalendarWidget(binding.container)
        binding.yyyyM.text = sdf_yyyyM.format(System.currentTimeMillis())
        calendarWidget.onDateSelectedListener = {
            Log.i(TAG, "date select: ${sdf_yyyyMMddHHmmss.format(timeInMillis)}")
            binding.yyyyM.text = sdf_yyyyM.format(timeInMillis)
            binding.fab.rotation = if (timeInMillis.dDays > System.currentTimeMillis().dDays) {
                0f
            } else if (timeInMillis.dDays < System.currentTimeMillis().dDays) {
                180f
            } else {
                90f
            }
        }
        lifecycleScope.launch {
            mainViewModel.getDailyTasks(binding.container.adapter.models)
            Log.i(TAG, "notify models changed")
            binding.container.adapter.notifyModelsChanged()
            binding.container.invalidate()
        }

        binding.container.onDailyTaskClickBlock = { model ->
            lifecycleScope.launch {
                mainViewModel.removeDailyTask(model, binding.container.adapter.models)
                binding.container.adapter.notifyModelRemoved(model)
                binding.container.invalidate()
            }
        }
        binding.container.onCreateTaskClickBlock = { model ->
            lifecycleScope.launch {
                mainViewModel.saveCreateDailyTask(model, binding.container.adapter.models)
                binding.container.adapter.notifyModelsChanged()
                Log.i(TAG, "daily task added: ${model.title}")
                binding.container.invalidate()
            }
        }

        binding.fab.setOnClickListener { view ->
            calendarWidget.resetScrollState()
        }
        binding.switchRange.run {
            textOff = "日"
            textOn = "三日"
            isChecked = true
            setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    calendarWidget.renderRange = ICalendarWidget.RenderRange.ThreeDayRange
                } else {
                    calendarWidget.renderRange = ICalendarWidget.RenderRange.SingleDayRange
                }
            }
        }
    }
}
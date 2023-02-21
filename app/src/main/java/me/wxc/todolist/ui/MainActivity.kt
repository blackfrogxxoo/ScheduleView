package me.wxc.todolist.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.PopupMenu
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.PagerSnapHelper
import kotlinx.coroutines.launch
import me.wxc.todolist.App
import me.wxc.todolist.R
import me.wxc.todolist.databinding.ActivityMainBinding
import me.wxc.widget.SchedulerConfig
import me.wxc.widget.base.ISchedulerWidget
import me.wxc.widget.calender.MonthAdapter
import me.wxc.widget.scheduler.SchedulerWidget
import me.wxc.widget.scheduler.components.CreateTaskModel
import me.wxc.widget.scheduler.components.DailyTaskModel
import me.wxc.widget.tools.TAG
import me.wxc.widget.tools.dDays
import me.wxc.widget.tools.sdf_yyyyM
import me.wxc.widget.tools.sdf_yyyyMMddHHmmss

class MainActivity : AppCompatActivity() {
    private val mainViewModel by viewModels<MainViewModel>()
    private lateinit var binding: ActivityMainBinding
    private val monthAdapter by lazy { MonthAdapter(binding.monthViewList) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeSchedulerConfig()
        val calendarWidget = SchedulerWidget(binding.schedulerView)
        binding.yyyyM.text = sdf_yyyyM.format(System.currentTimeMillis())

        binding.monthViewList.run {
            PagerSnapHelper().attachToRecyclerView(this)
            adapter = monthAdapter
        }

        binding.fab.setOnClickListener {
            if (binding.schedulerView.isVisible) {
                calendarWidget.onSelectedTime()
            } else {
                monthAdapter.onSelectedTime()
            }
        }
        binding.more.setOnClickListener {
            PopupMenu(this, it).run {
                menuInflater.inflate(R.menu.main, menu)
                setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.threeDay -> {
                            val fromMonth = binding.monthViewList.visibility == View.VISIBLE
                            binding.schedulerView.visibility = View.VISIBLE
                            binding.monthViewList.visibility = View.GONE
                            calendarWidget.renderRange = ISchedulerWidget.RenderRange.ThreeDayRange
                            if (fromMonth) {
                                calendarWidget.onSelectedTime(SchedulerConfig.selectedTime)
                            }
                        }
                        R.id.singleDay -> {
                            val fromMonth = binding.monthViewList.visibility == View.VISIBLE
                            binding.schedulerView.visibility = View.VISIBLE
                            binding.monthViewList.visibility = View.GONE
                            calendarWidget.renderRange = ISchedulerWidget.RenderRange.SingleDayRange
                            if (fromMonth) {
                                calendarWidget.onSelectedTime(SchedulerConfig.selectedTime)
                            }
                        }
                        R.id.month -> {
                            val fromDay = binding.schedulerView.visibility == View.VISIBLE
                            binding.schedulerView.visibility = View.GONE
                            binding.monthViewList.visibility = View.VISIBLE
                            if (fromDay) {
                                monthAdapter.onSelectedTime(SchedulerConfig.selectedTime)
                            }
                        }
                        else -> {}
                    }
                    true
                }
                show()
            }
        }
    }

    private fun initializeSchedulerConfig() {
        SchedulerConfig.run {
            app = App.self
//        schedulerStartTime = System.currentTimeMillis() - 30 * dayMills
//        schedulerEndTime = System.currentTimeMillis() + 30 * dayMills
            onDateSelectedListener = {
                Log.i(TAG, "date select: ${sdf_yyyyMMddHHmmss.format(timeInMillis)}")
                binding.yyyyM.text = sdf_yyyyM.format(timeInMillis)
                binding.fab.rotation = if (timeInMillis.dDays > System.currentTimeMillis().dDays) {
                    0f
                } else if (timeInMillis.dDays < System.currentTimeMillis().dDays) {
                    180f
                } else {
                    90f
                }
                selectedTime = timeInMillis
            }
            schedulerModelsProvider = { startTime, endTime ->
                mainViewModel.getRangeDailyTask(startTime, endTime)
            }
            lifecycleScope = this@MainActivity.lifecycleScope
            onDailyTaskClickBlock = { model ->
                DetailsFragment().apply {
                    taskModel = model
                    onSaveBlock = {
                        SchedulerConfig.lifecycleScope.launch {
                            mainViewModel.updateDailyTask(
                                it as DailyTaskModel,
                                binding.schedulerView.adapter.models
                            )
                            binding.schedulerView.adapter.notifyModelsChanged()
                            binding.schedulerView.invalidate()
                            monthAdapter.refreshCurrentItem()
                        }
                    }
                    onDeleteBlock = {
                        SchedulerConfig.lifecycleScope.launch {
                            mainViewModel.removeDailyTask(
                                it as DailyTaskModel,
                                binding.schedulerView.adapter.models
                            )
                            binding.schedulerView.adapter.notifyModelRemoved(model)
                            binding.schedulerView.invalidate()
                            monthAdapter.refreshCurrentItem()
                        }
                    }
                }.show(supportFragmentManager, "DetailsFragment")
            }
            onCreateTaskClickBlock = { model ->
                DetailsFragment().apply {
                    taskModel = model
                    onSaveBlock = {
                        SchedulerConfig.lifecycleScope.launch {
                            mainViewModel.saveCreateDailyTask(
                                it as CreateTaskModel,
                                binding.schedulerView.adapter.models
                            )
                            binding.schedulerView.adapter.notifyModelsChanged()
                            Log.i(TAG, "daily task added: ${model.title}")
                            binding.schedulerView.invalidate()
                            monthAdapter.refreshCurrentItem()
                        }
                    }
                }.show(supportFragmentManager, "DetailsFragment")

            }
        }
    }
}
package me.wxc.todolist.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.PopupMenu
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.PagerSnapHelper
import kotlinx.coroutines.launch
import me.wxc.todolist.App
import me.wxc.todolist.R
import me.wxc.todolist.databinding.ActivityMainBinding
import me.wxc.widget.ScheduleConfig
import me.wxc.widget.base.CalendarMode
import me.wxc.widget.base.ICalendarRender
import me.wxc.widget.base.IScheduleWidget
import me.wxc.widget.base.RepeatMode
import me.wxc.widget.calender.MonthAdapter
import me.wxc.widget.schedule.ScheduleWidget
import me.wxc.widget.schedule.components.CreateTaskModel
import me.wxc.widget.schedule.components.DailyTaskModel
import me.wxc.widget.tools.*

class MainActivity : AppCompatActivity() {
    private val mainViewModel by viewModels<MainViewModel>()
    private lateinit var binding: ActivityMainBinding
    private val monthAdapter by lazy { MonthAdapter(binding.monthViewList) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeScheduleConfig()
        val calendarWidget = ScheduleWidget(binding.scheduleView)
        binding.yyyyM.text = sdf_yyyyM.format(System.currentTimeMillis())

        binding.monthViewList.run {
            PagerSnapHelper().attachToRecyclerView(this)
            adapter = monthAdapter
        }

        binding.today.setOnClickListener {
            if (binding.scheduleView.isVisible) {
                calendarWidget.selectedDayTime = startOfDay().timeInMillis
            } else if (binding.monthViewList.isVisible) {
                monthAdapter.selectedDayTime = startOfDay().timeInMillis
            } else {
                binding.flowGroup.children.filterIsInstance<ICalendarRender>().forEach {
                   it.focusedDayTime = -1
                }
            }
        }
        binding.fab.setOnClickListener {
            ScheduleConfig.onCreateTaskClickBlock(CreateTaskModel(
                startTime = ScheduleConfig.selectedDayTime + 10 * hourMillis,
                duration = quarterMillis * 2,
                onNeedScrollBlock = { _, _ -> }
            ))
        }
        binding.more.setOnClickListener {
            PopupMenu(this, it).run {
                menuInflater.inflate(R.menu.main, menu)
                setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.threeDay -> {
                            val fromMonth = binding.monthViewList.visibility == View.VISIBLE
                            binding.scheduleView.visibility = View.VISIBLE
                            binding.monthViewList.visibility = View.GONE
                            binding.flowGroup.visibility = View.GONE
                            calendarWidget.renderRange = IScheduleWidget.RenderRange.ThreeDayRange
                            if (fromMonth) {
                                calendarWidget.selectedDayTime =
                                    startOfDay(ScheduleConfig.selectedDayTime).timeInMillis
                            }
                        }
                        R.id.singleDay -> {
                            val fromMonth = binding.monthViewList.visibility == View.VISIBLE
                            binding.scheduleView.visibility = View.VISIBLE
                            binding.monthViewList.visibility = View.GONE
                            binding.flowGroup.visibility = View.GONE
                            calendarWidget.renderRange = IScheduleWidget.RenderRange.SingleDayRange
                            if (fromMonth) {
                                calendarWidget.selectedDayTime =
                                    startOfDay(ScheduleConfig.selectedDayTime).timeInMillis
                            }
                        }
                        R.id.month -> {
                            val fromDay = binding.scheduleView.visibility == View.VISIBLE
                            binding.scheduleView.visibility = View.GONE
                            binding.monthViewList.visibility = View.VISIBLE
                            binding.flowGroup.visibility = View.GONE
                            if (fromDay) {
                                monthAdapter.selectedDayTime =
                                    startOfDay(ScheduleConfig.selectedDayTime).timeInMillis
                            }
                        }
                        else -> {
                            binding.scheduleView.visibility = View.GONE
                            binding.monthViewList.visibility = View.GONE
                            binding.flowGroup.visibility = View.VISIBLE
                        }
                    }
                    true
                }
                show()
            }
        }
    }

    private fun initializeScheduleConfig() {
        ScheduleConfig.run {
            app = App.self
//        scheduleStartTime = System.currentTimeMillis() - 30 * dayMillis
//        scheduleEndTime = System.currentTimeMillis() + 30 * dayMillis
            onDateSelectedListener = {
                Log.i(TAG, "date select: ${sdf_yyyyMMddHHmmss.format(timeInMillis)}")
                binding.yyyyM.text = sdf_yyyyM.format(timeInMillis)
                selectedDayTime = timeInMillis
            }
            scheduleModelsProvider = { startTime, endTime ->
                mainViewModel.getRangeDailyTask(startTime, endTime)
            }
            lifecycleScope = this@MainActivity.lifecycleScope
            onDailyTaskClickBlock = { model ->
                DetailsFragment().apply {
                    taskModel = model
                    onSaveBlock = {
                        ScheduleConfig.lifecycleScope.launch {
                            mainViewModel.updateDailyTask(
                                it as DailyTaskModel,
                                binding.scheduleView.adapter.models
                            )
                            binding.scheduleView.adapter.notifyModelsChanged()
                            binding.scheduleView.invalidate()
                            monthAdapter.refreshCurrentItem()
                        }
                    }
                    onDeleteBlock = { deletedModel, deleteOption ->
                        ScheduleConfig.lifecycleScope.launch {
                            mainViewModel.removeDailyTask(
                                deletedModel as DailyTaskModel,
                                binding.scheduleView.adapter.models,
                                deleteOption
                            )
                            if (model.repeatMode == RepeatMode.Never) {
                                binding.scheduleView.adapter.notifyModelRemoved(model)
                            } else {
                                binding.scheduleView.adapter.notifyModelsChanged()
                            }
                            binding.scheduleView.invalidate()
                            monthAdapter.refreshCurrentItem()
                        }
                    }
                }.show(supportFragmentManager, "DetailsFragment")
            }
            onCreateTaskClickBlock = { model ->
                DetailsFragment().apply {
                    taskModel = model
                    onSaveBlock = {
                        ScheduleConfig.lifecycleScope.launch {
                            mainViewModel.saveCreateDailyTask(
                                it as CreateTaskModel,
                                binding.scheduleView.adapter.models
                            )
                            binding.scheduleView.removeCreateTask()
                            binding.scheduleView.adapter.notifyModelsChanged()
                            Log.i(TAG, "daily task added: ${model.title}")
                            binding.scheduleView.invalidate()
                            monthAdapter.refreshCurrentItem()
                        }
                    }
                }.show(supportFragmentManager, "DetailsFragment")

            }
        }
    }
}
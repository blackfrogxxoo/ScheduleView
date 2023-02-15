package me.wxc.todolist.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.PopupMenu
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import me.wxc.todolist.R
import me.wxc.todolist.databinding.ActivityMainBinding
import me.wxc.widget.base.ISchedulerWidget
import me.wxc.widget.scheduler.SchedulerWidget
import me.wxc.widget.tools.*
import java.util.*

class MainActivity : AppCompatActivity() {
    private val mainViewModel by viewModels<MainViewModel>()
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val calendarWidget = SchedulerWidget(binding.container)
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
        Log.i(TAG, "set text")
        binding.monthViewList.run {
            PagerSnapHelper().attachToRecyclerView(this)
            adapter = MonthAdapter(lifecycleScope)
            post {
                resetMonthViewList()
            }
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                private var lastPosition = -1
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val llm = recyclerView.layoutManager as LinearLayoutManager
                    val position = llm.findFirstCompletelyVisibleItemPosition()
                    if (position != -1 && lastPosition != position) {
                        lastPosition = position
                        binding.yyyyM.text = startOfDay(0L).apply {
                            add(Calendar.MONTH, position)
                        }.timeInMillis.run {
                            sdf_yyyyM.format(this)
                        }
                    }
                }
            })
        }

        binding.fab.setOnClickListener {
            if (binding.container.isVisible) {
                calendarWidget.resetScrollState()
            } else {
                resetMonthViewList()
            }
        }
        binding.more.setOnClickListener {
            val popupMenu = PopupMenu(this, it)
            popupMenu.menuInflater.inflate(R.menu.main, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.threeDay -> {
                        binding.container.visibility = View.VISIBLE
                        binding.monthViewList.visibility = View.GONE
                        calendarWidget.renderRange = ISchedulerWidget.RenderRange.ThreeDayRange
                    }
                    R.id.singleDay -> {
                        binding.container.visibility = View.VISIBLE
                        binding.monthViewList.visibility = View.GONE
                        calendarWidget.renderRange = ISchedulerWidget.RenderRange.SingleDayRange
                    }
                    R.id.month -> {
                        binding.container.visibility = View.GONE
                        binding.monthViewList.visibility = View.VISIBLE
                    }
                    else -> {}
                }
                true
            }
            popupMenu.show()
        }
    }

    private fun resetMonthViewList() {
        val today = startOfDay()
        val firstDay = startOfDay(0L)
        val position =
            12 * (today.get(Calendar.YEAR) - firstDay.get(Calendar.YEAR)) + (today.get(
                Calendar.MONTH
            ) - firstDay.get(Calendar.MONTH))
        binding.monthViewList.scrollToPosition(position)
    }
}
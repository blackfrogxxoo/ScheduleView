package me.wxc.todolist.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.app.ActivityCompat
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import me.wxc.todolist.tools.Preferences
import me.wxc.todolist.tools.newUserTasks
import me.wxc.todolist.R
import me.wxc.widget.ScheduleConfig
import me.wxc.widget.base.*
import me.wxc.widget.base.DailyTaskModel
import me.wxc.widget.tools.*
import kotlinx.coroutines.launch
import me.wxc.todolist.databinding.FragmentMainBinding
import me.wxc.widget.base.ICalendarParent

class MainFragment : Fragment(), ICalendarParent {
    private val moreViewModel by viewModels<MainViewModel>()
    private lateinit var binding: FragmentMainBinding

    override val childRenders: List<ICalendarRender> by lazy {
        listOf(
            binding.flowGroup,
            binding.scheduleGroup,
            binding.monthGroup
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeScheduleConfig()
        initializeNewUser()
        checkPermissions()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        if (!::binding.isInitialized) {
            binding = FragmentMainBinding.inflate(layoutInflater)
            onBindingInit()
        }
        return binding.root
    }

    private fun onBindingInit() {
        binding.yyyyM.text = beginOfDay().yyyyM
        binding.today.setOnClickListener {
            ScheduleConfig.selectedDayTime = nowMillis
            binding.yyyyM.text = nowMillis.yyyyM
            childRenders.filter { it.isVisible() }.onEach {
                it.selectedDayTime = nowMillis
            }
        }
        binding.fab.setOnClickListener {
            ScheduleConfig.onCreateTaskClickBlock(
                DailyTaskModel(
                    beginTime = beginOfDay(ScheduleConfig.selectedDayTime).timeInMillis + 10 * hourMillis,
                    duration = quarterMillis * 2,
                    editingTaskModel = EditingTaskModel(),
                )
            )
        }
        binding.more.setOnClickListener {
            PopupMenu(requireContext(), it).run {
                menuInflater.inflate(R.menu.main, menu)
                setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.threeDay -> {
                            val needRefresh =
                                !binding.scheduleGroup.isVisible || binding.scheduleGroup.renderRange != IScheduleWidget.RenderRange.ThreeDayRange
                            if (needRefresh) {
                                binding.scheduleGroup.visibility = View.VISIBLE
                                binding.monthGroup.visibility = View.GONE
                                binding.flowGroup.visibility = View.GONE
                                binding.scheduleGroup.renderRange =
                                    IScheduleWidget.RenderRange.ThreeDayRange
                                binding.scheduleGroup.selectedDayTime =
                                    beginOfDay(ScheduleConfig.selectedDayTime).timeInMillis
                            }
                        }
                        R.id.singleDay -> {
                            val needRefresh =
                                !binding.scheduleGroup.isVisible || binding.scheduleGroup.renderRange is IScheduleWidget.RenderRange.ThreeDayRange
                            if (needRefresh) {
                                binding.scheduleGroup.visibility = View.VISIBLE
                                binding.monthGroup.visibility = View.GONE
                                binding.flowGroup.visibility = View.GONE
                                binding.scheduleGroup.renderRange =
                                    IScheduleWidget.RenderRange.SingleDayRange
                                binding.scheduleGroup.selectedDayTime =
                                    beginOfDay(ScheduleConfig.selectedDayTime).timeInMillis
                            }
                        }
                        R.id.month -> {
                            val needRefresh = !binding.monthGroup.isVisible
                            if (needRefresh) {
                                binding.scheduleGroup.visibility = View.GONE
                                binding.monthGroup.visibility = View.VISIBLE
                                binding.flowGroup.visibility = View.GONE
                                binding.monthGroup.selectedDayTime =
                                    beginOfDay(ScheduleConfig.selectedDayTime).timeInMillis
                            }
                        }
                        else -> {
                            val needRefresh = !binding.flowGroup.isVisible
                            if (needRefresh) {
                                binding.scheduleGroup.visibility = View.GONE
                                binding.monthGroup.visibility = View.GONE
                                binding.flowGroup.visibility = View.VISIBLE
                                binding.flowGroup.selectedDayTime =
                                    beginOfDay(ScheduleConfig.selectedDayTime).timeInMillis
                            }
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
//        scheduleBeginTime = nowMillis - 30 * dayMillis
//        scheduleEndTime = nowMillis + 30 * dayMillis
            onDateSelectedListener = {
                Log.i(TAG, "date select: $yyyyMMddHHmmss")
                binding.yyyyM.text = yyyyM
                selectedDayTime = timeInMillis
            }
            scheduleModelsProvider = { beginTime, endTime ->
                moreViewModel.getRangeDailyTask(beginTime, endTime)
            }
            lifecycleScope = this@MainFragment.lifecycleScope
            onDailyTaskClickBlock = { model ->
                DetailsFragment().apply {
                    taskModel = model.copy()
                    onDeleteBlock = { ids ->
                        childRenders.onEach { it.reloadSchedulesFromProvider() }
                    }
                    onSaveBlock = {
                        Log.i(TAG, "daily task added: ${model.title}")
                        childRenders.onEach { it.reloadSchedulesFromProvider() }
                    }
                }.show(childFragmentManager, "DetailsFragment")
            }
            onCreateTaskClickBlock = { model ->
                if (binding.scheduleGroup.isVisible()) {
                    binding.scheduleGroup.children.filterIsInstance<IScheduleRender>().toList().onEach {
                        it.finishEditing()
                        (it as View).invalidate()
                    }
                }
                DetailsFragment().apply {
                    taskModel = model.copy()
                    onSaveBlock = {
                        Log.i(TAG, "daily task added: ${model.title}")
                        childRenders.onEach { it.reloadSchedulesFromProvider() }
                    }
                }.show(childFragmentManager, "DetailsFragment")

            }
            onTaskDraggedBlock = { model ->
                lifecycleScope.launch {
                    moreViewModel.updateSingleDailyTask(model)
                    childRenders.onEach { it.reloadSchedulesFromProvider() }
                }
            }
        }
    }

    private fun initializeNewUser() {
        if (Preferences.getBoolean("newUser", true)) {
            lifecycleScope.launch {
                val list = mutableListOf<IScheduleModel>()
                newUserTasks.forEach {
                    moreViewModel.saveCreateDailyTask(it, list)
                }
                childRenders.onEach { it.reloadSchedulesFromProvider() }
            }
            Preferences.putBoolean("newUser", false)
        }
    }

    //检查定位权限
    private fun checkPermissions() {
        val granted = ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.READ_CALENDAR,
                    Manifest.permission.WRITE_CALENDAR
                ), 1
            )
        }
    }

    companion object {
        private const val TAG = "MoreFragment"
    }
}
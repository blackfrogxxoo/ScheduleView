package me.wxc.todolist

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import me.wxc.todolist.databinding.ActivityMainBinding
import me.wxc.widget.components.DailyTaskModel
import me.wxc.widget.tools.*
import java.text.SimpleDateFormat

class MainActivity : AppCompatActivity() {

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
        binding.container.adapter.models.apply {
            add(
                DailyTaskModel(
                    startTime = startOfDay().timeInMillis + 5 * hourMills,
                    duration = hourMills,
                    title = "抓💰"
                )
            )
            add(
                DailyTaskModel(
                    startTime = startOfDay().timeInMillis - 40 * hourMills,
                    duration = hourMills,
                    title = "摸🐠🐟"
                )
            )
            add(
                DailyTaskModel(
                    startTime = System.currentTimeMillis() - 2 * hourMills,
                    duration = hourMills,
                    title = "打麻将"
                )
            )
            add(
                DailyTaskModel(
                    startTime = System.currentTimeMillis() + 21 * hourMills,
                    duration = (2.5f * hourMills).toLong(),
                    title = "吃火锅"
                )
            )
            add(
                DailyTaskModel(
                    startTime = System.currentTimeMillis() + 41 * hourMills,
                    duration = (2.5f * hourMills).toLong(),
                    title = "打游戏"
                )
            )
        }
        binding.container.adapter.notifyModelsChanged()
        binding.container.onDailyTaskClickBlock = {
            Toast.makeText(this@MainActivity, title, Toast.LENGTH_SHORT).show()
        }
        binding.container.onCreateTaskClickBlock = {
            Toast.makeText(this@MainActivity, title, Toast.LENGTH_SHORT).show()
        }

        binding.fab.setOnClickListener { view ->
            calendarWidget.resetScrollState()
        }
    }
}
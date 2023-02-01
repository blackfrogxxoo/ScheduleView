package me.wxc.todolist

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import me.wxc.todolist.databinding.ActivityMainBinding
import me.wxc.widget.ICalendarModel
import me.wxc.widget.components.ClockLineModel
import me.wxc.widget.components.DailyTaskModel
import me.wxc.widget.components.DateLineBgModel
import me.wxc.widget.components.DateLineModel
import me.wxc.widget.tools.CalendarScroller
import me.wxc.widget.tools.hourMills
import me.wxc.widget.tools.startOfDay

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        CalendarScroller(binding.container)
        binding.container.adapter.models.apply {
            add(DateLineBgModel)
            add(
                DailyTaskModel(
                    startTime = startOfDay().timeInMillis,
                    duration = hourMills,
                    title = "打麻将"
                )
            )
            add(
                DailyTaskModel(
                    startTime = System.currentTimeMillis(),
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
        }
        binding.container.adapter.notifyDataChanged()


        binding.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }
    }
}
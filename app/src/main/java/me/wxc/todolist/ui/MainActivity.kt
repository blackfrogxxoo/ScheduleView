package me.wxc.todolist.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import me.wxc.todolist.R
import me.wxc.todolist.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportFragmentManager.beginTransaction().apply {
            add(R.id.container, MainFragment())
            commit()
        }
    }
}
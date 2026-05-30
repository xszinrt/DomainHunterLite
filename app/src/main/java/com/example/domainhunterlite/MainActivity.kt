package com.example.domainhunterlite

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.domainhunterlite.databinding.ActivityMainBinding
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var updateJob: Job? = null

    private val notifPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        binding.btnStart.setOnClickListener {
            startTask()
        }

        binding.btnStop.setOnClickListener {
            stopTask()
        }

        startUpdater()
    }

    private fun startTask() {
        val intent = Intent(this, HeavyTaskService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        binding.btnStart.isEnabled = false
        binding.btnStop.isEnabled = true
    }

    private fun stopTask() {
        val intent = Intent(this, HeavyTaskService::class.java).apply {
            action = HeavyTaskService.ACTION_STOP
        }
        startService(intent)
        binding.btnStart.isEnabled = true
        binding.btnStop.isEnabled = false
    }

    private fun startUpdater() {
        updateJob = CoroutineScope(Dispatchers.Main).launch {
            while (true) {
                val running = HeavyTaskService.isRunning
                val progress = HeavyTaskService.progress
                
                binding.tvStatus.text = if (running) "🟢 Running..." else if (progress >= 100) "✅ Completed" else "⏹ Stopped"
                binding.tvProgress.text = "$progress / 100"
                
                binding.btnStart.isEnabled = !running
                binding.btnStop.isEnabled = running
                
                delay(500)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        updateJob?.cancel()
    }
}

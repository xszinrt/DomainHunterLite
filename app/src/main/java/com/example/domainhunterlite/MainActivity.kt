package com.example.domainhunterlite

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.domainhunterlite.databinding.ActivityMainBinding
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val adapter = DomainAdapter()
    private var filePath: String? = null
    private val resultsList = mutableListOf<ClassifiedDomain>()

    private val filePicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@registerForActivityResult
        try {
            val fileName = getFileName(uri)
            val file = File(cacheDir, fileName)
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output -> input.copyTo(output) }
            }
            filePath = file.absolutePath
            binding.tvFileName.text = fileName  // ✅ إصلاح: String وليس File
            binding.btnStart.isEnabled = true
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private val updateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val progress = intent.getIntExtra("progress", 0)
            val total = intent.getIntExtra("total", 0)
            val empty = intent.getIntExtra("empty", 0)
            val parked = intent.getIntExtra("parked", 0)
            val active = intent.getIntExtra("active", 0)
            
            binding.tvProgress.text = "$progress / $total"
            binding.tvEmpty.text = "📄 $empty"
            binding.tvParked.text = "💰 $parked"
            binding.tvActive.text = "🌐 $active"
            binding.tvResultCount.text = "${resultsList.size} results"
            binding.progressBar.max = total
            binding.progressBar.progress = progress
            
            @Suppress("UNCHECKED_CAST")
            val results = intent.getSerializableExtra("results") as? ArrayList<ClassifiedDomain>
            results?.let {
                resultsList.clear()
                resultsList.addAll(it)
                adapter.submitList(resultsList.toList())
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.btnImport.setOnClickListener {
            filePicker.launch(arrayOf("text/plain", "text/csv", "*/*"))
        }

        binding.btnStart.setOnClickListener {
            if (filePath == null) {
                Toast.makeText(this, "Select a file first!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent(this, ScanService::class.java).apply {
                putExtra(ScanService.EXTRA_FILE_PATH, filePath)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            binding.btnStart.isEnabled = false
            binding.btnStop.isEnabled = true
        }

        binding.btnStop.setOnClickListener {
            startService(Intent(this, ScanService::class.java).apply {
                action = ScanService.ACTION_STOP
            })
            binding.btnStart.isEnabled = true
            binding.btnStop.isEnabled = false
        }

        binding.btnExport.setOnClickListener {
            if (resultsList.isEmpty()) {
                Toast.makeText(this, "No results to export!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            exportResults()
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(updateReceiver, IntentFilter("SCAN_UPDATE"))
    }

    private fun getFileName(uri: Uri): String {
        var name = "domains.txt"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) name = cursor.getString(idx)
            }
        }
        return name
    }

    private fun exportResults() {
        val file = File(cacheDir, "results.csv")
        file.bufferedWriter().use { writer ->
            writer.write("Domain,Type\n")
            resultsList.forEach {
                writer.write("${it.domain},${it.type}\n")
            }
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, androidx.core.content.FileProvider.getUriForFile(
                this@MainActivity,
                "${packageName}.provider",
                file
            ))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Export Results"))
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(updateReceiver)
    }
}

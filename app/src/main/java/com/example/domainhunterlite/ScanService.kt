package com.example.domainhunterlite

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.domainhunterlite.utils.RdapFetcher
import kotlinx.coroutines.*
import java.io.File

class ScanService : Service() {

    private var scanJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val results = mutableListOf<ClassifiedDomain>()
    private var totalDomains = 0
    private var currentProgress = 0
    private var emptyCount = 0
    private var parkedCount = 0
    private var activeCount = 0
    private var filePath: String? = null

    companion object {
        const val CHANNEL_ID = "scan_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val EXTRA_FILE_PATH = "EXTRA_FILE_PATH"
        
        var isRunning = false
        var isPaused = false
        var progress = 0
        var total = 0
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopScan()
                return START_NOT_STICKY
            }
            ACTION_PAUSE -> {
                isPaused = !isPaused
                updateNotification()
                return START_STICKY
            }
        }

        if (isRunning) {
            updateNotification()
            return START_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification())
        filePath = intent?.getStringExtra(EXTRA_FILE_PATH) ?: return START_NOT_STICKY
        
        startScan()
        return START_STICKY
    }

    private fun startScan() {
        scanJob = scope.launch {
            val domains = File(filePath).readLines().filter { it.isNotBlank() }.take(1000)
            totalDomains = domains.size
            total = totalDomains
            
            val chunkSize = 5
            val chunks = domains.chunked(chunkSize)
            
            for (chunk in chunks) {
                if (!isRunning) break
                while (isPaused) { delay(100) }
                
                val deferred = chunk.map { domain ->
                    async {
                        val rdapResult = RdapFetcher.check(domain)
                        DomainClassifier.classify(rdapResult)
                    }
                }
                
                val chunkResults = deferred.awaitAll()
                
                chunkResults.forEach { result ->
                    results.add(result)
                    currentProgress++
                    progress = currentProgress
                    
                    when (result.type) {
                        DomainType.EMPTY -> emptyCount++
                        DomainType.PARKED -> parkedCount++
                        DomainType.ACTIVE -> activeCount++
                    }
                    
                    updateNotification()
                    
                    // إرسال تحديث إلى MainActivity
                    sendBroadcast(Intent("SCAN_UPDATE").apply {
                        putExtra("progress", currentProgress)
                        putExtra("total", totalDomains)
                        putExtra("empty", emptyCount)
                        putExtra("parked", parkedCount)
                        putExtra("active", activeCount)
                        putExtra("results", ArrayList(results))
                    })
                }
            }
            
            isRunning = false
            stopForeground(STOP_FOREGROUND_DETACH)
            stopSelf()
        }
    }

    private fun stopScan() {
        isRunning = false
        isPaused = false
        scanJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun updateNotification() {
        val percent = if (total > 0) (progress * 100 / total) else 0
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Domain Hunter")
            .setContentText("$progress / $total ($percent%)")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Empty: $emptyCount | Parked: $parkedCount | Active: $activeCount"))
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .setProgress(total, progress, false)
            .setOngoing(true)
        
        val stopIntent = PendingIntent.getService(this, 1,
            Intent(this, ScanService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        
        builder.addAction(android.R.drawable.ic_delete, "Stop", stopIntent)
        
        startForeground(NOTIFICATION_ID, builder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Domain Scan", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Domain Hunter")
            .setContentText("Preparing scan...")
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        scanJob?.cancel()
        scope.cancel()
    }
}

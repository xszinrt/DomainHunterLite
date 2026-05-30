package com.example.domainhunterlite

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class HeavyTaskService : Service() {

    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var startTime = 0L
    private var currentRequest = 0
    private val TOTAL_REQUESTS = 100

    companion object {
        const val CHANNEL_ID = "heavy_task_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_STOP = "ACTION_STOP"
        
        var isRunning = false
        var progress = 0
        var elapsedSeconds = 0
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopTask()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification())
        startTask()
        return START_STICKY
    }

    private fun startTask() {
        isRunning = true
        progress = 0
        startTime = System.currentTimeMillis()
        currentRequest = 0

        job = scope.launch {
            for (i in 1..TOTAL_REQUESTS) {
                if (!isRunning) break
                
                currentRequest = i
                progress = i
                
                try {
                    val request = Request.Builder()
                        .url("https://httpbin.org/delay/0.5")
                        .build()
                    
                    withContext(Dispatchers.IO) {
                        client.newCall(request).execute().close()
                    }
                    
                } catch (e: IOException) {
                    // نستمر حتى لو فشل الطلب
                }
                
                withContext(Dispatchers.Main) {
                    updateNotification()
                }
                
                delay(100)
            }
            
            isRunning = false
            withContext(Dispatchers.Main) {
                updateNotification()
            }
            stopForeground(STOP_FOREGROUND_DETACH)
            stopSelf()
        }
    }

    private fun stopTask() {
        isRunning = false
        job?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(): Notification {
        val elapsed = (System.currentTimeMillis() - startTime) / 1000
        val percent = if (TOTAL_REQUESTS > 0) (progress * 100 / TOTAL_REQUESTS) else 0
        val statusText = if (isRunning) "Running" else if (progress >= TOTAL_REQUESTS) "Completed" else "Stopped"
        
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, HeavyTaskService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("📡 Heavy Task - $statusText")
            .setContentText("$progress / $TOTAL_REQUESTS ($percent%)")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Requests: $progress / $TOTAL_REQUESTS\n" +
                    "Elapsed: ${elapsed}s\n" +
                    "ETA: ${((TOTAL_REQUESTS - progress) * 0.6).toInt()}s"
                )
            )
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .setProgress(TOTAL_REQUESTS, progress, false)
            .setContentIntent(openIntent)
            .addAction(android.R.drawable.ic_delete, "Stop", stopIntent)
            .setOngoing(isRunning)
            .setOnlyAlertOnce(false)
            .build()
    }

    private fun updateNotification() {
        try {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIFICATION_ID, buildNotification())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Heavy Task",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress of heavy network task"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        job?.cancel()
        scope.cancel()
    }
}

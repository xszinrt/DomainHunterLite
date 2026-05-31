package com.example.domainhunterlite;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.example.domainhunterlite.utils.RdapFetcher;
import com.example.domainhunterlite.utils.RdapResult;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;

public class ScanService extends Service {
    
    private Thread scanThread;
    private boolean isRunning = false;
    private boolean isPaused = false;
    private String filePath;
    private List<ClassifiedDomain> results = new ArrayList<>();
    private int totalDomains = 0;
    private int currentProgress = 0;
    private int emptyCount = 0;
    private int parkedCount = 0;
    private int activeCount = 0;
    
    public static final String CHANNEL_ID = "scan_channel";
    public static final int NOTIFICATION_ID = 1;
    public static final String ACTION_STOP = "ACTION_STOP";
    public static final String EXTRA_FILE_PATH = "EXTRA_FILE_PATH";
    
    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopScan();
            return START_NOT_STICKY;
        }
        
        if (isRunning) {
            return START_STICKY;
        }
        
        startForeground(NOTIFICATION_ID, buildNotification());
        filePath = intent.getStringExtra(EXTRA_FILE_PATH);
        if (filePath == null) return START_NOT_STICKY;
        
        startScan();
        return START_STICKY;
    }
    
    private void startScan() {
        isRunning = true;
        scanThread = new Thread(() -> {
            try {
                // Read domains from file
                List<String> domains = new ArrayList<>();
                try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (!line.trim().isEmpty()) {
                            domains.add(line.trim());
                        }
                    }
                }
                
                totalDomains = Math.min(domains.size(), 1000);
                List<String> limitedDomains = domains.subList(0, totalDomains);
                
                int chunkSize = 5;
                for (int i = 0; i < limitedDomains.size(); i += chunkSize) {
                    if (!isRunning) break;
                    while (isPaused) {
                        try { Thread.sleep(100); } catch (InterruptedException e) { break; }
                    }
                    
                    int end = Math.min(i + chunkSize, limitedDomains.size());
                    List<String> chunk = limitedDomains.subList(i, end);
                    
                    List<ClassifiedDomain> chunkResults = new ArrayList<>();
                    for (String domain : chunk) {
                        RdapResult rdapResult = RdapFetcher.check(domain);
                        ClassifiedDomain classified = DomainClassifier.classify(rdapResult);
                        chunkResults.add(classified);
                        results.add(classified);
                        currentProgress++;
                        
                        switch (classified.type) {
                            case EMPTY: emptyCount++; break;
                            case PARKED: parkedCount++; break;
                            case ACTIVE: activeCount++; break;
                        }
                        
                        updateNotification();
                        
                        Intent updateIntent = new Intent("SCAN_UPDATE");
                        updateIntent.putExtra("progress", currentProgress);
                        updateIntent.putExtra("total", totalDomains);
                        updateIntent.putExtra("empty", emptyCount);
                        updateIntent.putExtra("parked", parkedCount);
                        updateIntent.putExtra("active", activeCount);
                        updateIntent.putExtra("results", new ArrayList<>(results));
                        LocalBroadcastManager.getInstance(ScanService.this).sendBroadcast(updateIntent);
                    }
                }
                
                isRunning = false;
                stopForeground(true);
                stopSelf();
            } catch (IOException e) {
                e.printStackTrace();
                isRunning = false;
                stopSelf();
            }
        });
        scanThread.start();
    }
    
    private void stopScan() {
        isRunning = false;
        isPaused = false;
        if (scanThread != null) {
            scanThread.interrupt();
        }
        stopForeground(true);
        stopSelf();
    }
    
    private void updateNotification() {
        int percent = totalDomains > 0 ? (currentProgress * 100 / totalDomains) : 0;
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Domain Hunter")
            .setContentText(currentProgress + " / " + totalDomains + " (" + percent + "%)")
            .setStyle(new NotificationCompat.BigTextStyle().bigText("Empty: " + emptyCount + " | Parked: " + parkedCount + " | Active: " + activeCount))
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .setProgress(totalDomains, currentProgress, false)
            .setOngoing(true)
            .build();
        
        startForeground(NOTIFICATION_ID, notification);
    }
    
    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Domain Hunter")
            .setContentText("Preparing scan...")
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .setOngoing(true)
            .build();
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Domain Scan",
                NotificationManager.IMPORTANCE_LOW
            );
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

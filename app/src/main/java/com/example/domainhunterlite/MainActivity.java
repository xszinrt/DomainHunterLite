package com.example.domainhunterlite;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    
    private ActivityMainBinding binding;
    private DomainAdapter adapter;
    private String filePath = null;
    private List<ClassifiedDomain> resultsList = new ArrayList<>();
    
    private ActivityResultLauncher<String[]> filePicker = registerForActivityResult(
        new ActivityResultContracts.OpenDocument(),
        uri -> {
            if (uri == null) return;
            try {
                String fileName = getFileName(uri);
                File file = new File(getCacheDir(), fileName);
                getContentResolver().openInputStream(uri).use(input -> {
                    new FileOutputStream(file).use(output -> {
                        byte[] buffer = new byte[8192];
                        int length;
                        while ((length = input.read(buffer)) > 0) {
                            output.write(buffer, 0, length);
                        }
                    });
                });
                filePath = file.getAbsolutePath();
                binding.tvFileName.setText(fileName);
                binding.btnStart.setEnabled(true);
            } catch (Exception e) {
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    );
    
    private BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int progress = intent.getIntExtra("progress", 0);
            int total = intent.getIntExtra("total", 0);
            int empty = intent.getIntExtra("empty", 0);
            int parked = intent.getIntExtra("parked", 0);
            int active = intent.getIntExtra("active", 0);
            
            binding.tvProgress.setText(progress + " / " + total);
            binding.tvEmpty.setText("📄 " + empty);
            binding.tvParked.setText("💰 " + parked);
            binding.tvActive.setText("🌐 " + active);
            binding.tvResultCount.setText(resultsList.size() + " results");
            binding.progressBar.setMax(total);
            binding.progressBar.setProgress(progress);
            
            ArrayList<ClassifiedDomain> results = (ArrayList<ClassifiedDomain>) intent.getSerializableExtra("results");
            if (results != null) {
                resultsList.clear();
                resultsList.addAll(results);
                adapter.submitList(new ArrayList<>(resultsList));
            }
        }
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
        
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DomainAdapter();
        binding.recyclerView.setAdapter(adapter);
        
        binding.btnImport.setOnClickListener(v -> {
            filePicker.launch(new String[]{"text/plain", "text/csv", "*/*"});
        });
        
        binding.btnStart.setOnClickListener(v -> {
            if (filePath == null) {
                Toast.makeText(this, "Select a file first!", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, ScanService.class);
            intent.putExtra(ScanService.EXTRA_FILE_PATH, filePath);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
            binding.btnStart.setEnabled(false);
            binding.btnStop.setEnabled(true);
        });
        
        binding.btnStop.setOnClickListener(v -> {
            startService(new Intent(this, ScanService.class).setAction(ScanService.ACTION_STOP));
            binding.btnStart.setEnabled(true);
            binding.btnStop.setEnabled(false);
        });
        
        binding.btnExport.setOnClickListener(v -> {
            if (resultsList.isEmpty()) {
                Toast.makeText(this, "No results to export!", Toast.LENGTH_SHORT).show();
                return;
            }
            exportResults();
        });
        
        LocalBroadcastManager.getInstance(this).registerReceiver(updateReceiver, new IntentFilter("SCAN_UPDATE"));
    }
    
    private String getFileName(Uri uri) {
        String name = "domains.txt";
        try (var cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) name = cursor.getString(idx);
            }
        }
        return name;
    }
    
    private void exportResults() {
        File file = new File(getCacheDir(), "results.csv");
        try (java.io.FileWriter writer = new java.io.FileWriter(file)) {
            writer.write("Domain,Type\n");
            for (ClassifiedDomain domain : resultsList) {
                writer.write(domain.domain + "," + domain.type + "\n");
            }
        } catch (IOException e) {
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }
        
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_STREAM, androidx.core.content.FileProvider.getUriForFile(
            this,
            getPackageName() + ".provider",
            file
        ));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "Export Results"));
    }
    
    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
        new ActivityResultContracts.RequestPermission(),
        isGranted -> {}
    );
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(updateReceiver);
    }
}

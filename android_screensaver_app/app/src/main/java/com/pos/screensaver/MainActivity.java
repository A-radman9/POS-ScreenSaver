package com.pos.screensaver;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

/**
 * Ultra-simple, zero-clutter POS Screensaver Setup Activity.
 * Fixed 2-minute timer.
 * Automatically loads from /sdcard/Ads/.
 */
public class MainActivity extends Activity {

    private static final int REQUEST_OVERLAY_CODE = 2001;
    private static final int REQUEST_STORAGE_CODE = 2002;

    private TextView statusText;
    private TextView mediaCountText;
    private TextView scannerStatusText;

    private Button btnGrantOverlay;
    private Button btnGrantStorage;
    private Button btnGrantAccessibility;
    private Button btnStartService;
    private Button btnTestNow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.tvStatus);
        mediaCountText = findViewById(R.id.tvMediaCount);
        scannerStatusText = findViewById(R.id.tvScannerStatus);

        btnGrantOverlay = findViewById(R.id.btnPermission);
        btnGrantStorage = findViewById(R.id.btnStoragePermission);
        btnGrantAccessibility = findViewById(R.id.btnAccessibilityPermission);
        btnStartService = findViewById(R.id.btnStart);
        btnTestNow = findViewById(R.id.btnTest);

        createAdsFolder();
        updateUI();

        // 1. Overlay Permission
        btnGrantOverlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Intent intent = new Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName())
                    );
                    startActivityForResult(intent, REQUEST_OVERLAY_CODE);
                }
            }
        });

        // 2. Storage / All Files Access Permission
        btnGrantStorage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    try {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivityForResult(intent, REQUEST_STORAGE_CODE);
                    } catch (Exception e) {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                        startActivityForResult(intent, REQUEST_STORAGE_CODE);
                    }
                }
            }
        });

        // 3. Barcode Scanner Detection (Accessibility) with Android 15 Restricted Setting Helper
        btnGrantAccessibility.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAndroid15RestrictedHelperDialog();
            }
        });

        // 4. Start Inactivity Monitor (closes this app so POS is in front)
        btnStartService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startMonitoringService();
            }
        });

        // 5. Test Screensaver (minimizes first so dismissing returns to POS)
        btnTestNow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Displaying ad in 1 second...", Toast.LENGTH_SHORT).show();
                moveTaskToBack(true);

                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(MainActivity.this, AdActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                }, 1000);
            }
        });
    }

    /**
     * Explains how to unlock "Restricted setting" on Android 13/14/15 Samsung phones
     */
    private void showAndroid15RestrictedHelperDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enable Barcode Scanner Watcher");
        builder.setMessage(
                "If your phone shows 'Restricted setting' (الإعدادات المقيدة):\n\n" +
                "1. Tap 'Open App Settings' below\n" +
                "2. Tap the 3 dots (⋮) in the top-right corner\n" +
                "3. Tap 'Allow restricted settings' (السماح بالإعدادات المقيدة)\n" +
                "4. Return to Accessibility and toggle POS Ad Screensaver ON!\n\n" +
                "(Note: On your kiosk terminal, this security restriction is usually NOT enabled)."
        );
        builder.setPositiveButton("Open Accessibility", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivity(intent);
            }
        });
        builder.setNeutralButton("Open App Info (3 Dots)", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private boolean isAccessibilityServiceEnabled() {
        String prefString = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (prefString != null) {
            return prefString.contains(getPackageName() + "/" + KioskAccessibilityService.class.getName())
                    || prefString.contains(getPackageName() + "/.KioskAccessibilityService");
        }
        return false;
    }

    private void createAdsFolder() {
        File adsDir = new File(Environment.getExternalStorageDirectory(), "Ads");
        if (!adsDir.exists()) {
            adsDir.mkdirs();
        }
    }

    private int countLoadedAds() {
        int count = 0;
        File adsDir = new File(Environment.getExternalStorageDirectory(), "Ads");
        if (adsDir.exists()) {
            File[] files = adsDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    String name = f.getName().toLowerCase();
                    if (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png")
                            || name.endsWith(".webp") || name.endsWith(".mp4") || name.endsWith(".webm")
                            || name.endsWith(".mkv") || name.endsWith(".mov")) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private void updateUI() {
        boolean hasOverlay = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            hasOverlay = Settings.canDrawOverlays(this);
        }

        boolean hasStorage = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            hasStorage = Environment.isExternalStorageManager();
        }

        boolean hasAccessibility = isAccessibilityServiceEnabled();

        int adsFound = countLoadedAds();
        if (mediaCountText != null) {
            mediaCountText.setText("Ads in /sdcard/Ads/: " + adsFound + " files detected");
        }

        if (scannerStatusText != null) {
            if (hasAccessibility) {
                scannerStatusText.setText("✓ Barcode Scanner Watcher: ACTIVE (Timer resets on every scanned item!)");
                scannerStatusText.setTextColor(0xFF10B981); // Green
                btnGrantAccessibility.setVisibility(View.GONE);
            } else {
                scannerStatusText.setText("⚠️ Barcode Scanner Watcher: Optional (Tap below to enable)");
                scannerStatusText.setTextColor(0xFFFFB74D); // Orange
                btnGrantAccessibility.setVisibility(View.VISIBLE);
            }
        }

        if (!hasOverlay) {
            statusText.setText("Step 1: Grant 'Display over other apps' so screensaver can appear over POS.");
            btnGrantOverlay.setVisibility(View.VISIBLE);
            btnGrantStorage.setVisibility(View.GONE);
            btnStartService.setEnabled(false);
        } else if (!hasStorage && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            statusText.setText("Step 2: Grant 'All files access' so app can read /sdcard/Ads/.");
            btnGrantOverlay.setVisibility(View.GONE);
            btnGrantStorage.setVisibility(View.VISIBLE);
            btnStartService.setEnabled(false);
        } else {
            statusText.setText("Status: Ready! Screensaver will activate after 1 minute of idle time.");
            btnGrantOverlay.setVisibility(View.GONE);
            btnGrantStorage.setVisibility(View.GONE);
            btnStartService.setEnabled(true);
        }
    }

    private void startMonitoringService() {
        Intent intent = new Intent(this, IdleOverlayService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }

        Toast.makeText(this, "Inactivity monitor active! Returning to POS...", Toast.LENGTH_LONG).show();

        // Close and remove MainActivity so it never appears when screensaver dismisses!
        moveTaskToBack(true);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }
}

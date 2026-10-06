package com.pos.screensaver;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

/**
 * Background Service that monitors touch activity across the entire POS screen.
 * Uses an invisible touch-watcher overlay with FLAG_WATCH_OUTSIDE_TOUCH so it does NOT
 * block or interfere with the existing POS app.
 * When no touch is detected for 60 seconds, it launches AdActivity.
 */
public class IdleOverlayService extends Service {

    private static final String TAG = "IdleOverlayService";
    private static final String CHANNEL_ID = "pos_screensaver_channel";
    private static final int NOTIFICATION_ID = 1001;

    // Default 60 seconds (1 minute) of inactivity
    public static long IDLE_TIMEOUT_MS = 60 * 1000L;

    private static IdleOverlayService instance;
    private WindowManager windowManager;
    private View touchObserverView;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable idleRunnable;
    private boolean isScreensaverShowing = false;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // Load configured timeout from preferences (default 1 minute = 60s)
        android.content.SharedPreferences prefs = getSharedPreferences("pos_screensaver_prefs", Context.MODE_PRIVATE);
        int seconds = prefs.getInt("idle_timeout_seconds", 60);
        IDLE_TIMEOUT_MS = seconds * 1000L;

        startForegroundNotification();
        setupTouchObserver();
        resetIdleTimer();
    }

    private void startForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "POS Screensaver Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Monitors POS inactivity to display advertisements");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }

            Notification notification = new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle("POS Screensaver Active")
                    .setContentText("Monitoring kiosk inactivity (1 minute timer)")
                    .setSmallIcon(android.R.drawable.ic_menu_slideshow)
                    .build();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }
        }
    }

    /**
     * Attaches an invisible 1x1 view with FLAG_WATCH_OUTSIDE_TOUCH.
     * This passively receives MotionEvent.ACTION_OUTSIDE whenever the user touches the POS app!
     */
    private void setupTouchObserver() {
        if (!Settings.canDrawOverlays(this)) {
            Log.w(TAG, "SYSTEM_ALERT_WINDOW permission not granted yet");
            return;
        }

        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        touchObserverView = new View(this);

        int layoutType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutType = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        }

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                1, 1, // Minimal 1x1 pixel footprint
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;

        touchObserverView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // Customer touched the screen (on the POS app or anywhere)
                resetIdleTimer();
                return false; // Allow touch to pass through to POS unchanged
            }
        });

        try {
            windowManager.addView(touchObserverView, params);
        } catch (Exception e) {
            Log.e(TAG, "Error adding touch observer view", e);
        }
    }

    /**
     * Reset the 60s idle countdown
     */
    public synchronized void resetIdleTimer() {
        handler.removeCallbacks(idleRunnable);

        idleRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isScreensaverShowing && !AdActivity.isActive) {
                    launchScreensaver();
                }
            }
        };

        handler.postDelayed(idleRunnable, IDLE_TIMEOUT_MS);
    }

    /**
     * Launch the Fullscreen Ad Activity on top of the POS app
     */
    private void launchScreensaver() {
        if (AdActivity.isActive) {
            isScreensaverShowing = true;
            return;
        }
        isScreensaverShowing = true;
        Intent intent = new Intent(this, AdActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    /**
     * Called when the screensaver is dismissed or touch occurs
     */
    public static void notifyActivity() {
        if (instance != null) {
            instance.isScreensaverShowing = false;
            instance.resetIdleTimer();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        android.content.SharedPreferences prefs = getSharedPreferences("pos_screensaver_prefs", Context.MODE_PRIVATE);
        int seconds = prefs.getInt("idle_timeout_seconds", 60);
        IDLE_TIMEOUT_MS = seconds * 1000L;
        resetIdleTimer();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        if (windowManager != null && touchObserverView != null) {
            try {
                windowManager.removeView(touchObserverView);
            } catch (Exception ignored) {}
        }
        instance = null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

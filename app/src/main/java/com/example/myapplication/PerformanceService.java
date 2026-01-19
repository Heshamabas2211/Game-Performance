package com.example.myapplication;

import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class PerformanceService extends Service {
    private static final String TAG = "PerformanceService";
    private static final int NOTIFICATION_ID = 101;
    private static final String CHANNEL_ID = "GameUltraChannel";

    private Timer optimizationTimer;
    private String selectedAppPackage;
    private boolean maxPerformanceEnabled;
    private UsageStatsManager usageStatsManager;
    private PowerManager.WakeLock wakeLock;

    @Override
    public void onCreate() {
        super.onCreate();
        usageStatsManager = (UsageStatsManager) getSystemService(USAGE_STATS_SERVICE);
        createNotificationChannel();

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "GameUltra::WakeLock"
            );
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            selectedAppPackage = intent.getStringExtra("selected_app");
            maxPerformanceEnabled = intent.getBooleanExtra("max_performance", false);

            // بدء الإشعار الدائم فوراً
            startForeground(NOTIFICATION_ID, createNotification());

            // منع الجهاز من النوم
            if (wakeLock != null && !wakeLock.isHeld()) {
                try {
                    wakeLock.acquire(10 * 60 * 1000L /*10 minutes*/);
                } catch (Exception e) {
                    Log.e(TAG, "Error acquiring wakelock: " + e.getMessage());
                }
            }

            startOptimization();
        }

        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                NotificationChannel serviceChannel = new NotificationChannel(
                        CHANNEL_ID,
                        "Game Ultra Optimizer",
                        NotificationManager.IMPORTANCE_LOW
                );
                serviceChannel.setDescription("Optimization service running");
                serviceChannel.enableLights(true);
                serviceChannel.setLightColor(Color.CYAN);
                serviceChannel.enableVibration(false);

                NotificationManager manager = getSystemService(NotificationManager.class);
                if (manager != null) {
                    manager.createNotificationChannel(serviceChannel);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error creating notification channel: " + e.getMessage());
            }
        }
    }

    private Notification createNotification() {
        try {
            Intent notificationIntent = new Intent(this, PerformanceActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this,
                    0, notificationIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

            String contentText = "Game Ultra is optimizing performance";
            if (selectedAppPackage != null && !selectedAppPackage.isEmpty()) {
                contentText = "Optimizing selected app performance";
            }

            return new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("🚀 Game Ultra")
                    .setContentText(contentText)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .setColor(Color.CYAN)
                    .build();
        } catch (Exception e) {
            Log.e(TAG, "Error creating notification: " + e.getMessage());
            return null;
        }
    }

    private void startOptimization() {
        stopOptimization();

        optimizationTimer = new Timer();
        optimizationTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                optimizeInBackground();
            }
        }, 5000, 60000); // بدء بعد 5 ثوانٍ ثم كل 60 ثانية
    }

    private void optimizeInBackground() {
        try {
            System.gc();

            if (canOptimizeApps()) {
                closeBackgroundApps();
            }

            Log.d(TAG, "Optimization at: " + System.currentTimeMillis());

            // تحديث الإشعار
            updateNotification();
        } catch (Exception e) {
            Log.e(TAG, "Error in optimization: " + e.getMessage());
        }
    }

    private boolean canOptimizeApps() {
        // التحقق من الأذونات
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
                if (appOps != null) {
                    int mode = appOps.checkOpNoThrow(
                            AppOpsManager.OPSTR_GET_USAGE_STATS,
                            android.os.Process.myUid(),
                            getPackageName()
                    );
                    return mode == AppOpsManager.MODE_ALLOWED;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking permissions: " + e.getMessage());
            }
        }
        return true;
    }

    private void updateNotification() {
        try {
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                Notification notification = createNotification();
                if (notification != null) {
                    manager.notify(NOTIFICATION_ID, notification);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating notification: " + e.getMessage());
        }
    }

    private void closeBackgroundApps() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                long currentTime = System.currentTimeMillis();
                List<UsageStats> stats = usageStatsManager.queryUsageStats(
                        UsageStatsManager.INTERVAL_DAILY, currentTime - 1000 * 60 * 60, currentTime);

                if (stats != null) {
                    for (UsageStats usageStats : stats) {
                        String packageName = usageStats.getPackageName();

                        if (shouldSkipPackage(packageName)) {
                            continue;
                        }

                        safelyCloseApp(packageName);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error closing apps: " + e.getMessage());
        }
    }

    private boolean shouldSkipPackage(String packageName) {
        return packageName.equals(selectedAppPackage) ||
                packageName.equals(getPackageName()) ||
                isSystemPackage(packageName) ||
                packageName.contains("launcher") ||
                packageName.contains("systemui") ||
                packageName.contains("settings");
    }

    private void safelyCloseApp(String packageName) {
        try {
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                am.killBackgroundProcesses(packageName);
                Log.d(TAG, "Attempted to close: " + packageName);
            }
        } catch (SecurityException e) {
            Log.w(TAG, "No permission to close: " + packageName);
        } catch (Exception e) {
            Log.e(TAG, "Error closing app: " + packageName, e);
        }
    }

    private boolean isSystemPackage(String packageName) {
        try {
            PackageManager pm = getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
            return (ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
        } catch (PackageManager.NameNotFoundException e) {
            return true;
        }
    }

    private void stopOptimization() {
        if (optimizationTimer != null) {
            optimizationTimer.cancel();
            optimizationTimer = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopOptimization();

        if (wakeLock != null && wakeLock.isHeld()) {
            try {
                wakeLock.release();
            } catch (Exception e) {
                Log.e(TAG, "Error releasing wakelock: " + e.getMessage());
            }
        }

        stopForeground(true);
        Log.d(TAG, "Service destroyed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
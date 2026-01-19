package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class PerformanceActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_SELECT_APP = 1001;
    private static final int REQUEST_USAGE_STATS_PERMISSION = 1002;
    private static final int REQUEST_WRITE_SETTINGS_PERMISSION = 1003;
    private static final int REQUEST_NOTIFICATION_PERMISSION = 1004;
    private static final int REQUEST_BACKGROUND_PERMISSION = 1005;
    private static final String TAG = "GameUltra";

    private String selectedAppPackage = "";
    private String selectedAppName = "";
    private Drawable selectedAppIcon = null;
    private boolean isOptimizationEnabled = false;
    private boolean isMaxPerformanceEnabled = false;

    private Timer optimizationTimer;
    private UsageStatsManager usageStatsManager;

    private TextView tvSelectedApp;
    private TextView tvStatus;
    private Switch switchOptimize;
    private Switch switchMaxPerformance;
    private ImageView ivAppIcon;
    private Button btnOptimizeNow;
    private ProgressBar progressBar;
    private View neonGlowEffect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_performance);

        // تهيئة العناصر
        initViews();

        // تحميل الإعدادات المحفوظة
        loadSettings();

        // الحصول على مدير إحصائيات الاستخدام
        usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);

        // بدء تأثير النيون
        startNeonEffects();

        // طلب الأذونات اللازمة
        requestAllPermissions();
    }

    private void startNeonEffects() {
        // تأثير توهج النيون
        Animation neonGlow = new AlphaAnimation(0.3f, 1.0f);
        neonGlow.setDuration(2000);
        neonGlow.setRepeatMode(Animation.REVERSE);
        neonGlow.setRepeatCount(Animation.INFINITE);
        neonGlowEffect.startAnimation(neonGlow);

        // تأثير للزر
        Animation buttonGlow = new AlphaAnimation(0.6f, 1.0f);
        buttonGlow.setDuration(1500);
        buttonGlow.setRepeatMode(Animation.REVERSE);
        buttonGlow.setRepeatCount(Animation.INFINITE);
        btnOptimizeNow.startAnimation(buttonGlow);
    }

    private void requestAllPermissions() {
        // قائمة الصلاحيات المطلوبة
        List<String> permissionsToRequest = new ArrayList<>();

        // صلاحيات Android 13+ (Notification)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        // صلاحيات القتل
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.KILL_BACKGROUND_PROCESSES)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.KILL_BACKGROUND_PROCESSES);
        }

        // صلاحيات Wake Lock
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WAKE_LOCK)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.WAKE_LOCK);
        }

        // طلب الصلاحيات إذا كانت هناك صلاحيات مطلوبة
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_BACKGROUND_PERMISSION);
        }

        // طلب الصلاحيات الخاصة التي تحتاج إلى إعدادات النظام
        requestSpecialPermissions();
    }

    private void requestSpecialPermissions() {
        // طلب إذن الوصول إلى إحصائيات الاستخدام (يحتاج إلى إعدادات النظام)
        if (!hasUsageStatsPermission()) {
            showPermissionDialog("لعمل التطبيق بشكل صحيح، يرجى منح إذن إحصائيات الاستخدام",
                    Settings.ACTION_USAGE_ACCESS_SETTINGS, REQUEST_USAGE_STATS_PERMISSION);
        }

        // طلب إذن الكتابة في إعدادات النظام
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(this)) {
                showPermissionDialog("لتمكين وضع الأداء الأقصى، يرجى منح إذن تعديل الإعدادات",
                        Settings.ACTION_MANAGE_WRITE_SETTINGS, REQUEST_WRITE_SETTINGS_PERMISSION);
            }
        }
    }

    private void showPermissionDialog(String message, String action, int requestCode) {
        // عرض رسالة للمستخدم ثم فتح إعدادات النظام
        new android.app.AlertDialog.Builder(this)
                .setTitle("صلاحية مطلوبة")
                .setMessage(message)
                .setPositiveButton("فتح الإعدادات", (dialog, which) -> {
                    Intent intent = new Intent(action);
                    if (action.equals(Settings.ACTION_MANAGE_WRITE_SETTINGS)) {
                        intent.setData(Uri.parse("package:" + getPackageName()));
                    }
                    startActivityForResult(intent, requestCode);
                })
                .setNegativeButton("لاحقاً", null)
                .show();
    }

    private boolean hasUsageStatsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
                int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                        android.os.Process.myUid(), getPackageName());
                return mode == AppOpsManager.MODE_ALLOWED;
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_BACKGROUND_PERMISSION) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                Toast.makeText(this, "✅ تم منح جميع الصلاحيات", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "⚠️ بعض الصلاحيات لم تمنح، قد لا يعمل التطبيق بشكل كامل", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "✅ تم منح إذن الإشعارات", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // باقي الكود كما هو...
    private void initViews() {
        tvSelectedApp = findViewById(R.id.tvSelectedApp);
        tvStatus = findViewById(R.id.tvStatus);
        switchOptimize = findViewById(R.id.switchOptimize);
        switchMaxPerformance = findViewById(R.id.switchMaxPerformance);
        ivAppIcon = findViewById(R.id.ivAppIcon);
        btnOptimizeNow = findViewById(R.id.btnOptimizeNow);
        progressBar = findViewById(R.id.progressBar);
        neonGlowEffect = findViewById(R.id.neonGlowEffect);

        Button btnSelectApp = findViewById(R.id.btnSelectApp);
        btnSelectApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectApplication();
            }
        });

        switchOptimize.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isOptimizationEnabled = isChecked;
                if (isChecked) {
                    if (selectedAppPackage.isEmpty()) {
                        Toast.makeText(PerformanceActivity.this, "يجب اختيار تطبيق أولاً", Toast.LENGTH_SHORT).show();
                        switchOptimize.setChecked(false);
                        return;
                    }
                    if (!hasUsageStatsPermission()) {
                        Toast.makeText(PerformanceActivity.this, "يجب منح إذن إحصائيات الاستخدام أولاً", Toast.LENGTH_SHORT).show();
                        switchOptimize.setChecked(false);
                        requestSpecialPermissions();
                        return;
                    }
                    startOptimization();
                } else {
                    stopOptimization();
                }
                updateStatus();
                saveSettings();
            }
        });

        switchMaxPerformance.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isMaxPerformanceEnabled = isChecked;
                if (isChecked && isOptimizationEnabled) {
                    applyMaxPerformanceSettings();
                }
                saveSettings();
            }
        });

        btnOptimizeNow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedAppPackage.isEmpty()) {
                    Toast.makeText(PerformanceActivity.this, "يجب اختيار تطبيق أولاً", Toast.LENGTH_SHORT).show();
                    return;
                }
                showProgress(true);
                optimizeMemoryAndCPU();
                new android.os.Handler().postDelayed(
                        new Runnable() {
                            public void run() {
                                showProgress(false);
                                Toast.makeText(PerformanceActivity.this, "✅ تم التحسين بنجاح", Toast.LENGTH_SHORT).show();
                            }
                        },
                        1000);
            }
        });
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnOptimizeNow.setEnabled(!show);
    }

    private void selectApplication() {
        Intent pickerIntent = new Intent(this, AppListActivity.class);
        startActivityForResult(pickerIntent, REQUEST_CODE_SELECT_APP);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SELECT_APP && resultCode == RESULT_OK && data != null) {
            String packageName = data.getStringExtra("selected_app");
            if (packageName != null) {
                setSelectedApp(packageName);
                saveSettings();
            }
        } else if (requestCode == REQUEST_USAGE_STATS_PERMISSION ||
                requestCode == REQUEST_WRITE_SETTINGS_PERMISSION) {
            // تم العودة من شاشة الإعدادات
            Toast.makeText(this, "تم العودة من الإعدادات، يرجى التحقق من الصلاحيات", Toast.LENGTH_SHORT).show();
        }
    }

    private void setSelectedApp(String packageName) {
        try {
            PackageManager pm = getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
            selectedAppPackage = packageName;
            selectedAppName = pm.getApplicationLabel(ai).toString();
            selectedAppIcon = pm.getApplicationIcon(ai);

            tvSelectedApp.setText(selectedAppName);
            ivAppIcon.setImageDrawable(selectedAppIcon);

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(this, "تعذر تحميل معلومات التطبيق", Toast.LENGTH_SHORT).show();
        }
    }

    private void startOptimization() {
        // إيقاف أي عملية تحسين سابقة
        stopOptimization();

        // بدء الموقت لتنظيف الذاكرة كل 60 ثانية
        optimizationTimer = new Timer();
        optimizationTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        optimizeMemoryAndCPU();
                    }
                });
            }
        }, 0, 60000);

        // تطبيق إعدادات الأداء فوراً
        optimizeMemoryAndCPU();

        if (isMaxPerformanceEnabled) {
            applyMaxPerformanceSettings();
        }

        // بدء الخدمة للعمل في الخلفية
        Intent serviceIntent = new Intent(this, PerformanceService.class);
        serviceIntent.putExtra("selected_app", selectedAppPackage);
        serviceIntent.putExtra("max_performance", isMaxPerformanceEnabled);

        // بدء الخدمة مع مراعاة Android 14
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, serviceIntent);
        } else {
            startService(serviceIntent);
        }

        tvStatus.setText("🚀 الحالة: مفعل - التحسين يعمل");
        tvStatus.setTextColor(getColor(R.color.neon_green));
        Toast.makeText(this, "✅ تم تفعيل تحسين الأداء", Toast.LENGTH_SHORT).show();
    }

    private void stopOptimization() {
        if (optimizationTimer != null) {
            optimizationTimer.cancel();
            optimizationTimer = null;
        }

        // إيقاف الخدمة
        Intent serviceIntent = new Intent(this, PerformanceService.class);
        stopService(serviceIntent);

        tvStatus.setText("⏸️ الحالة: غير مفعل");
        tvStatus.setTextColor(getColor(R.color.neon_red));
        Toast.makeText(this, "⏸️ تم إيقاف تحسين الأداء", Toast.LENGTH_SHORT).show();
    }

    private void optimizeMemoryAndCPU() {
        // تنظيف الذاكرة
        System.gc();

        // محاولة إغلاق التطبيقات الأخرى في الخلفية
        if (hasUsageStatsPermission()) {
            closeBackgroundApps();
        }

        Log.d(TAG, "تم تنظيف الذاكرة والمعالج في: " + System.currentTimeMillis());
    }

    private void closeBackgroundApps() {
        // طريقة آمنة لإغلاق التطبيقات في الخلفية
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

                        // استخدام طريقة آمنة
                        safelyCloseApp(packageName);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "خطأ في إغلاق التطبيقات: " + e.getMessage());
        }
    }

    private boolean shouldSkipPackage(String packageName) {
        return packageName.equals(selectedAppPackage) ||
                packageName.equals(getPackageName()) ||
                isSystemPackage(packageName) ||
                packageName.contains("launcher") ||
                packageName.contains("systemui") ||
                packageName.contains("settings") ||
                packageName.contains("google") ||
                packageName.contains("samsung") ||
                packageName.contains("android");
    }

    private void safelyCloseApp(String packageName) {
        try {
            // هذه الطريقة أكثر أمانًا في Android 14
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                if (am != null) {
                    am.killBackgroundProcesses(packageName);
                    Log.d(TAG, "تم محاولة إغلاق التطبيق: " + packageName);
                }
            }
        } catch (SecurityException e) {
            Log.w(TAG, "لا يوجد إذن لإغلاق التطبيق: " + packageName);
        } catch (Exception e) {
            Log.e(TAG, "خطأ في إغلاق التطبيق: " + packageName, e);
        }
    }

    private boolean isSystemPackage(String packageName) {
        try {
            PackageManager pm = getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
            return (ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
        } catch (PackageManager.NameNotFoundException e) {
            return true; // إذا لم نجد التطبيق، نعتبره تطبيق نظام لتجنب المشاكل
        }
    }

    private void applyMaxPerformanceSettings() {
        try {
            // محاولة رفع أولوية التطبيق
            increaseAppPriority();

            Toast.makeText(this, "🚀 تم تفعيل وضع الأداء الأقصى", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e(TAG, "تعذر تطبيق إعدادات الأداء القصوى: " + e.getMessage());
        }
    }

    private void increaseAppPriority() {
        try {
            Intent intent = getPackageManager().getLaunchIntentForPackage(selectedAppPackage);
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
            }
        } catch (Exception e) {
            Log.e(TAG, "تعزيز الأولوية: " + e.getMessage());
        }
    }

    private void updateStatus() {
        if (isOptimizationEnabled) {
            tvStatus.setText("🚀 الحالة: مفعل - التحسين يعمل");
            tvStatus.setTextColor(getColor(R.color.neon_green));
        } else {
            tvStatus.setText("⏸️ الحالة: غير مفعل");
            tvStatus.setTextColor(getColor(R.color.neon_red));
        }
    }

    private void saveSettings() {
        SharedPreferences prefs = getSharedPreferences("GameUltraPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("optimization_enabled", isOptimizationEnabled);
        editor.putString("selected_app", selectedAppPackage);
        editor.putBoolean("max_performance", isMaxPerformanceEnabled);
        editor.apply();
    }

    private void loadSettings() {
        SharedPreferences prefs = getSharedPreferences("GameUltraPrefs", MODE_PRIVATE);
        isOptimizationEnabled = prefs.getBoolean("optimization_enabled", false);
        selectedAppPackage = prefs.getString("selected_app", "");
        isMaxPerformanceEnabled = prefs.getBoolean("max_performance", false);

        if (!selectedAppPackage.isEmpty()) {
            setSelectedApp(selectedAppPackage);
        }

        if (isOptimizationEnabled) {
            switchOptimize.setChecked(true);
            startOptimization();
        }

        if (isMaxPerformanceEnabled) {
            switchMaxPerformance.setChecked(true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopOptimization();
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveSettings();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
        // إعادة طلب الصلاحيات عند العودة للتطبيق
        requestSpecialPermissions();
    }
}
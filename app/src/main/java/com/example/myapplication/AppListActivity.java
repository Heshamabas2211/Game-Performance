package com.example.myapplication;


import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AppListActivity extends AppCompatActivity implements AppListAdapter.OnAppSelectedListener {

    private RecyclerView recyclerView;
    private AppListAdapter adapter;
    private List<AppInfo> appList = new ArrayList<>();
    private List<AppInfo> filteredAppList = new ArrayList<>();
    private SearchView searchView;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private PackageManager packageManager;
    private static AppListActivity instance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_list);
        instance = this;

        // تهيئة العناصر
        recyclerView = findViewById(R.id.recyclerView);
        searchView = findViewById(R.id.searchView);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);

        packageManager = getPackageManager();

        // إعداد RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        adapter = new AppListAdapter(this, filteredAppList, this);
        recyclerView.setAdapter(adapter);

        // تحميل قائمة التطبيقات
        new LoadAppsTask().execute();

        // إعداد SearchView للبحث
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterApps(newText);
                return true;
            }
        });

        // إعداد زر الرجوع
        findViewById(R.id.btnBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    // طريقة للحصول على السياق
    public static AppListActivity getInstance() {
        return instance;
    }

    private class LoadAppsTask extends AsyncTask<Void, Void, List<AppInfo>> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
            tvEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.GONE);
        }

        @Override
        protected List<AppInfo> doInBackground(Void... voids) {
            List<AppInfo> apps = new ArrayList<>();

            try {
                List<ApplicationInfo> packages = packageManager.getInstalledApplications(
                        PackageManager.GET_META_DATA);

                for (ApplicationInfo packageInfo : packages) {
                    try {
                        String appName = packageManager.getApplicationLabel(packageInfo).toString();
                        String packageName = packageInfo.packageName;

                        // تحقق مما إذا كان التطبيق قابل للتنفيذ (ليس تطبيق نظام مهم)
                        Intent launchIntent = packageManager.getLaunchIntentForPackage(packageName);
                        if (launchIntent != null && (packageInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                            Drawable icon = packageManager.getApplicationIcon(packageInfo);
                            AppInfo appInfo = new AppInfo(appName, packageName, icon);
                            apps.add(appInfo);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                // فرز القائمة أبجدياً
                Collections.sort(apps, new Comparator<AppInfo>() {
                    @Override
                    public int compare(AppInfo app1, AppInfo app2) {
                        return app1.getAppName().compareToIgnoreCase(app2.getAppName());
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }

            return apps;
        }

        @Override
        protected void onPostExecute(List<AppInfo> apps) {
            super.onPostExecute(apps);
            progressBar.setVisibility(View.GONE);

            if (apps != null && !apps.isEmpty()) {
                appList.clear();
                appList.addAll(apps);
                filteredAppList.clear();
                filteredAppList.addAll(apps);
                adapter.notifyDataSetChanged();

                recyclerView.setVisibility(View.VISIBLE);
                tvEmpty.setVisibility(View.GONE);
            } else {
                tvEmpty.setText("لم يتم العثور على تطبيقات");
                tvEmpty.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            }
        }
    }

    private void filterApps(String query) {
        filteredAppList.clear();

        if (query == null || query.isEmpty()) {
            filteredAppList.addAll(appList);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (AppInfo app : appList) {
                if (app.getAppName().toLowerCase().contains(lowerCaseQuery) ||
                        app.getPackageName().toLowerCase().contains(lowerCaseQuery)) {
                    filteredAppList.add(app);
                }
            }
        }

        adapter.notifyDataSetChanged();

        if (filteredAppList.isEmpty()) {
            tvEmpty.setText("لا توجد تطبيقات تطابق البحث: " + query);
            tvEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onAppSelected(AppInfo appInfo) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("selected_app", appInfo.getPackageName());
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        instance = null;
    }
}
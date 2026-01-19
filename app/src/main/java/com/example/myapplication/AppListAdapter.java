package com.example.myapplication;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AppListAdapter extends RecyclerView.Adapter<AppListAdapter.AppViewHolder> {

    private Context context;
    private List<AppInfo> appList;
    private OnAppSelectedListener listener;

    public interface OnAppSelectedListener {
        void onAppSelected(AppInfo appInfo);
    }

    public AppListAdapter(Context context, List<AppInfo> appList, OnAppSelectedListener listener) {
        this.context = context;
        this.appList = appList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app, parent, false);
        return new AppViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
        AppInfo appInfo = appList.get(position);

        holder.appName.setText(appInfo.getAppName());
        holder.packageName.setText(appInfo.getPackageName());
        holder.appIcon.setImageDrawable(appInfo.getIcon());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAppSelected(appInfo);
            }
        });
    }

    @Override
    public int getItemCount() {
        return appList.size();
    }

    public void updateList(List<AppInfo> newList) {
        appList = newList;
        notifyDataSetChanged();
    }

    public static class AppViewHolder extends RecyclerView.ViewHolder {
        ImageView appIcon;
        TextView appName;
        TextView packageName;

        public AppViewHolder(@NonNull View itemView) {
            super(itemView);
            appIcon = itemView.findViewById(R.id.appIcon);
            appName = itemView.findViewById(R.id.appName);
            packageName = itemView.findViewById(R.id.packageName);
        }
    }
}

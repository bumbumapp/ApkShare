package com.bumbumapps.sharing.appsapk;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/** App rows drawn as one grouped white card: first/last rows get the rounded corners. */
public class AppAdapter extends RecyclerView.Adapter<AppAdapter.AppViewHolder> {

    public interface OnAppClick {
        void onAppClick(App app);
    }

    private final Context context;
    private final OnAppClick listener;
    List<App> apps;

    public AppAdapter(Context context, OnAppClick listener) {
        this.context = context;
        this.listener = listener;
    }

    void setApps(List<App> apps) {
        this.apps = apps;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.app_row, parent, false);
        return new AppViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
        App app = apps.get(position);
        holder.appName.setText(app.getName());
        String size = sizeText(context, app.getApkSize());
        holder.apkSize.setText(app.getVersionName() == null || app.getVersionName().isEmpty()
                ? size : context.getString(R.string.row_meta, size, app.getVersionName()));
        holder.appIcon.setImageDrawable(app.getIcon());
        holder.row.setBackgroundResource(rowBackground(position, getItemCount()));
        holder.row.setOnClickListener(view -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && apps != null && pos < apps.size()) {
                listener.onAppClick(apps.get(pos));
            }
        });
    }

    static int rowBackground(int position, int count) {
        if (count == 1) {
            return R.drawable.row_single;
        } else if (position == 0) {
            return R.drawable.row_top;
        } else if (position == count - 1) {
            return R.drawable.row_bottom;
        }
        return R.drawable.row_middle;
    }

    static String sizeText(Context context, long apkSize) {
        return AppUtils.formatSize(apkSize,
                context.getString(R.string.app_size_b),
                context.getString(R.string.app_size_kib),
                context.getString(R.string.app_size_mib),
                context.getString(R.string.app_size_gib));
    }

    @Override
    public int getItemCount() {
        return apps == null ? 0 : apps.size();
    }

    public static class AppViewHolder extends RecyclerView.ViewHolder {

        final View row;
        final ImageView appIcon;
        final TextView appName, apkSize;

        public AppViewHolder(@NonNull View itemView) {
            super(itemView);
            row = itemView.findViewById(R.id.app_row);
            appIcon = itemView.findViewById(R.id.app_icon);
            appName = itemView.findViewById(R.id.app_name);
            apkSize = itemView.findViewById(R.id.apk_size);
        }
    }
}

package com.bumbumapps.sharing.appsapk;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;


import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

import java.io.File;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.AppViewHolder> {

    Context context;
    List<App> apps;


    public AppAdapter(Context context) {
        this.context = context;
    }

    void setApps(List<App> apps){
        this.apps = apps;
        notifyDataSetChanged();

    }


    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.app_row,parent,false);
        return new AppViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
        holder.appName.setText(apps.get(position).getName());

        long apkSize = apps.get(position).getApkSize();

        holder.apkSize.setText(getHumanReadableSize(apkSize));
        holder.appIcon.setImageDrawable(apps.get(position).getIcon());




        holder.cardView.setOnClickListener(view -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION || apps == null || pos >= apps.size()) {
                return;
            }
            final String apkPath = apps.get(pos).getApkPath();
            if (context instanceof Activity) {
                // Natural break point: frequency-capped interstitial, then share.
                InterstitialHelper.showIfReady((Activity) context, () -> shareApk(apkPath));
            } else {
                shareApk(apkPath);
            }
        });

    }

    private void shareApk(String apkPath) {
        if (apkPath == null) {
            return;
        }
        try {
            Intent shareAPkIntent = new Intent(Intent.ACTION_SEND);
            shareAPkIntent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(
                    context, BuildConfig.APPLICATION_ID + ".provider", new File(apkPath)));
            shareAPkIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            shareAPkIntent.setType("application/vnd.android.package-archive");
            context.startActivity(Intent.createChooser(shareAPkIntent, "Share APK"));
        } catch (IllegalArgumentException | ActivityNotFoundException e) {
            Log.w("AppAdapter", "Unable to share " + apkPath, e);
            Toast.makeText(context, "Unable to share this app", Toast.LENGTH_SHORT).show();
        }
    }


    private String getHumanReadableSize(long apkSize) {
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

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    public static class AppViewHolder extends RecyclerView.ViewHolder{

        CardView cardView;
        ImageView appIcon;
        TextView appName,apkSize;

        public AppViewHolder(@NonNull View itemView) {
            super(itemView);


            cardView = itemView.findViewById(R.id.app_row);
            appIcon = itemView.findViewById(R.id.app_icon);
            appName = itemView.findViewById(R.id.app_name);
            apkSize = itemView.findViewById(R.id.apk_size);
        }
    }
}

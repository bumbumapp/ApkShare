package com.bumbumapps.sharing.appsapk;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

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
    InterstitialAd mInterstitialAd;


    public AppAdapter(Context context, List<App> apps) {
        this.context = context;
        this.apps = apps;
    }

    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.app_row,parent,false);
        return new AppViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, @SuppressLint("RecyclerView") final int position) {

        holder.appName.setText(apps.get(position).getName());

        long apkSize = apps.get(position).getApkSize();

        holder.apkSize.setText(getHumanReadableSize(apkSize));
        holder.appIcon.setImageDrawable(apps.get(position).getIcon());

        AdRequest adRequest = new AdRequest.Builder().build();

        InterstitialAd.load(context, "ca-app-pub-8444865753152507/8324597058", adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        // The mInterstitialAd reference will be null until
                        // an ad is loaded.
                        mInterstitialAd = interstitialAd;
                        Log.i("TAG", "onAdLoaded");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        // Handle the error
                        Log.i("TAG", loadAdError.getMessage());
                        mInterstitialAd = null;
                    }
                });

        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mInterstitialAd != null) {
                    mInterstitialAd.show((Activity) context);
                    mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback(){
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            Intent shareAPkIntent = new Intent();
                            shareAPkIntent.setAction(Intent.ACTION_SEND);

                            shareAPkIntent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(
                                    context, BuildConfig.APPLICATION_ID + ".provider", new File(apps.get(position).getApkPath())
                            ));

                            shareAPkIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            shareAPkIntent.setType("application/vnd.android.package-archive");

                            context.startActivity(Intent.createChooser(shareAPkIntent, "Share APK"));

                            Log.d("TAG", "The ad was dismissed.");
                        }

                        @Override
                        public void onAdFailedToShowFullScreenContent(AdError adError) {
                            // Called when fullscreen content failed to show.
                            Log.d("TAG", "The ad failed to show.");
                        }

                        @Override
                        public void onAdShowedFullScreenContent() {
                            // Called when fullscreen content is shown.
                            // Make sure to set your reference to null so you don't
                            // show it a second time.
                            mInterstitialAd = null;
                            Log.d("TAG", "The ad was shown.");
                        }
                    });
                }
                else {
                    Intent shareAPkIntent = new Intent();
                    shareAPkIntent.setAction(Intent.ACTION_SEND);

                    shareAPkIntent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(
                            context, BuildConfig.APPLICATION_ID + ".provider", new File(apps.get(position).getApkPath())
                    ));

                    shareAPkIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    shareAPkIntent.setType("application/vnd.android.package-archive");

                    context.startActivity(Intent.createChooser(shareAPkIntent, "Share APK"));

                }
            }
        });

    }


    private String getHumanReadableSize(long apkSize) {

        String humanReadableSize;
        if(apkSize<1024){
            humanReadableSize = String.format(
                    context.getString(R.string.app_size_b),
                    (double) apkSize
            );
        } else if(apkSize < Math.pow(1024,2)){
            humanReadableSize = String.format(
                    context.getString(R.string.app_size_kib),
                    (double) (apkSize/1024)
            );
        }else if(apkSize < Math.pow(1024,3)){
            humanReadableSize = String.format(
                    context.getString(R.string.app_size_mib),
                    (double) (apkSize/Math.pow(1024,2))
            );
        } else{
            humanReadableSize = String.format(
                    context.getString(R.string.app_size_gib),
                    (double) (apkSize/Math.pow(1024,3))
            );
        }
        return humanReadableSize;
    }

    @Override
    public int getItemCount() {
        return apps.size();
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

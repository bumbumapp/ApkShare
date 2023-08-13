package com.bumbumapps.sharing.appsapk;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;


import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    List<App> apps = new ArrayList<>();
    RecyclerView recyclerView;
    AdView adView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true);
        recyclerView = findViewById(R.id.app_list);
        adView=findViewById(R.id.adView);
        loadbannerads();
        AdsLoader.loadInterstitial(this);
         //Get App List
        PackageManager pm  = getApplicationContext().getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        for(ApplicationInfo packageInfo: packages){

            String name;

            if((name = String.valueOf(pm.getApplicationLabel(packageInfo))).isEmpty()){
                name = packageInfo.packageName;
            }

            Drawable icon  = pm.getApplicationIcon(packageInfo);
            String apkPath = packageInfo.sourceDir;
            long apkSize = new File(packageInfo.sourceDir).length();


            apps.add(new App(name,icon,apkPath,apkSize));


        }

        Collections.sort(apps, new Comparator<App>() {
                    @Override
                    public int compare(App app, App t1) {
                        return app.getName().toLowerCase().compareTo(t1.getName().toLowerCase());
                    }
                }
        );


        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setHasFixedSize(true);

        AppAdapter appAdapter = new AppAdapter(this,apps);
        recyclerView.setAdapter(appAdapter);



    }
    private void loadbannerads() {
        adView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
    }

}

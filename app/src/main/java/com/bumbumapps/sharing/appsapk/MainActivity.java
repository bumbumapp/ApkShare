package com.bumbumapps.sharing.appsapk;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;


import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {
    List<App> apps = new ArrayList<>();
    RecyclerView recyclerView;
    AdView adView;
    private PackageManager pm;
    private AppAdapter appAdapter;
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private SearchView searchView;
    private final List<App> filteredApps = new ArrayList<>() ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true);
        recyclerView = findViewById(R.id.app_list);
        adView=findViewById(R.id.adView);
        searchView = findViewById(R.id.searchView);
        appAdapter = new AppAdapter(this);
        recyclerView.setAdapter(appAdapter);
        appAdapter.setApps(apps);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setHasFixedSize(true);
        findViewById(R.id.search).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
             findViewById(R.id.toolbar).setVisibility(View.GONE);
             searchView.setVisibility(View.VISIBLE);
             searchView.onActionViewExpanded();
             searchView.setQueryHint("Search");
            }
        });
        searchApps();
        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                searchView.setVisibility(View.GONE);
                findViewById(R.id.toolbar).setVisibility(View.VISIBLE);
                appAdapter.setApps(apps);
                appAdapter.notifyDataSetChanged();
                return false;
            }
        });

         //Get App List
        pm  = getApplicationContext().getPackageManager();
        Observable<App> applicationInfoObservable = Observable
                .fromIterable(pm.getInstalledApplications(PackageManager.GET_META_DATA))
                .sorted(new Comparator<ApplicationInfo>() {
                    @Override
                    public int compare(ApplicationInfo applicationInfo, ApplicationInfo t1) {
                        return applicationInfo.packageName.toLowerCase().compareTo(t1.packageName.toLowerCase());
                    }
                })
                .map(new Function<ApplicationInfo, App>() {
                    @Override
                    public App apply(ApplicationInfo applicationInfo) throws Exception {
                        String name = String.valueOf(pm.getApplicationLabel(applicationInfo));
                        if (name.isEmpty()) {
                            name = applicationInfo.packageName;
                        }

                        Drawable icon = pm.getApplicationIcon(applicationInfo);
                        String apkPath = applicationInfo.sourceDir;
                        long apkSize = new File(applicationInfo.sourceDir).length();
                        return new App(name, icon, apkPath, apkSize);
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());

        applicationInfoObservable.subscribe(new Observer<App>() {
            @Override
            public void onSubscribe(Disposable d) {
                compositeDisposable.add(d);
            }

            @Override
            public void onNext(App app) {
                apps.add(app);
                appAdapter.setApps(apps);
                appAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        });




        AdsLoader.loadInterstitial(this);
        loadbannerads();


    }

    private void searchApps() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filteredApps.clear();
                if (newText != null){
                     Observable.
                            fromIterable(apps)
                             .filter(new Predicate<App>() {
                                @Override
                                public boolean test(App app) throws Exception {
                                    return app.getName().contains(newText);
                                }
                            })
                            .subscribeOn(Schedulers.io())
                             .observeOn(AndroidSchedulers.mainThread())
                             .subscribe(new Observer<App>() {
                                 @Override
                                 public void onSubscribe(Disposable d) {
                                     compositeDisposable.add(d);
                                     Log.d("onSubscribe","onSubscribe");
                                 }

                                 @Override
                                 public void onNext(App app) {
                                     filteredApps.add(app);
                                     appAdapter.setApps(filteredApps);
                                     appAdapter.notifyDataSetChanged();
                                     Log.d("onSubscribe","onNext");
                                 }

                                 @Override
                                 public void onError(Throwable e) {
                                     Log.d("onSubscribe","Throwable");
                                 }

                                 @Override
                                 public void onComplete() {
                                     Log.d("onSubscribe","onComplete");
                                     if (filteredApps.isEmpty()){
                                         appAdapter.setApps(apps);
                                         appAdapter.notifyDataSetChanged();
                                     }

                                 }
                             });
                }

                return false;
            }
        });
    }

    private void loadbannerads() {
        adView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (compositeDisposable!=null)
            compositeDisposable.dispose();
    }
}

package com.bumbumapps.sharing.appsapk;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;


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
import io.reactivex.plugins.RxJavaPlugins;
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
        EdgeToEdge.enable(this, SystemBarStyle.dark(Color.TRANSPARENT), SystemBarStyle.dark(Color.TRANSPARENT));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        EdgeToEdgeInsets.applySystemBarPadding(findViewById(R.id.root));

        // Errors emitted after the subscription is disposed (e.g. activity closed while loading)
        // must not crash the app with UndeliverableException.
        RxJavaPlugins.setErrorHandler(e -> Log.w("MainActivity", "Undeliverable Rx error", e));
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true);
        recyclerView = findViewById(R.id.app_list);
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
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (searchView.getVisibility() == View.VISIBLE) {
                    searchView.setQuery("", false);
                    searchView.setVisibility(View.GONE);
                    findViewById(R.id.toolbar).setVisibility(View.VISIBLE);
                    appAdapter.setApps(apps);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
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
                Log.e("MainActivity", "Failed to load installed apps", e);
            }

            @Override
            public void onComplete() {

            }
        });




        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.main_menu);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_privacy) {
                AdsConsent.showPrivacyOptions(this);
                return true;
            }
            return false;
        });
        AdsConsent.gather(this, () -> {
            toolbar.getMenu().findItem(R.id.action_privacy)
                    .setVisible(AdsConsent.isPrivacyOptionsRequired(this));
            if (adView == null) {
                adView = BannerHelper.load(this, findViewById(R.id.adContainer));
            }
            InterstitialHelper.preload(this);
        });


    }

    private void searchApps() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Filter synchronously on the main thread: the list is small and this avoids
                // concurrent modification of filteredApps by overlapping background queries.
                filteredApps.clear();
                filteredApps.addAll(AppUtils.filterByName(apps, newText));
                appAdapter.setApps(filteredApps);
                return false;
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (adView != null) {
            adView.destroy();
        }
        super.onDestroy();
        if (compositeDisposable!=null)
            compositeDisposable.dispose();
    }
}

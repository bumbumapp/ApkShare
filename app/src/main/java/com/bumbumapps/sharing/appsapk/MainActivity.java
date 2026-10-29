package com.bumbumapps.sharing.appsapk;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.SystemBarStyle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.ads.AdView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {
    private final List<App> apps = new ArrayList<>();
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private AdView adView;
    private AppAdapter appAdapter;
    private View header;
    private boolean loaded;
    private String query = "";
    private AppUtils.Filter filter = AppUtils.Filter.USER;
    private boolean largestFirst;
    private App pendingSave;

    private final ActivityResultLauncher<String> saveApk = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/vnd.android.package-archive"), this::onSaveTarget);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        EdgeToEdgeInsets.applySystemBarPadding(findViewById(R.id.root));

        // Errors emitted after the subscription is disposed (e.g. activity closed while loading)
        // must not crash the app with UndeliverableException.
        RxJavaPlugins.setErrorHandler(e -> Log.w("MainActivity", "Undeliverable Rx error", e));
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true);

        RecyclerView recyclerView = findViewById(R.id.app_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        header = LayoutInflater.from(this).inflate(R.layout.item_header, recyclerView, false);
        appAdapter = new AppAdapter(this, this::showAppSheet);
        recyclerView.setAdapter(new ConcatAdapter(new HeaderAdapter(header), appAdapter));
        bindHeader();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                View box = header.findViewById(R.id.search_box);
                if (box.getVisibility() == View.VISIBLE) {
                    closeSearch();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        loadApps();

        AdsConsent.gather(this, () -> {
            header.findViewById(R.id.btn_more)
                    .setVisibility(AdsConsent.isPrivacyOptionsRequired(this) ? View.VISIBLE : View.GONE);
            if (adView == null) {
                adView = BannerHelper.load(this, findViewById(R.id.adContainer));
            }
            InterstitialHelper.preload(this);
        });
    }

    private void bindHeader() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        ((TextView) header.findViewById(R.id.greeting)).setText(hour < 12 ? R.string.greeting_morning
                : hour < 18 ? R.string.greeting_afternoon : R.string.greeting_evening);

        header.findViewById(R.id.btn_search).setOnClickListener(v -> {
            if (header.findViewById(R.id.search_box).getVisibility() == View.VISIBLE) {
                closeSearch();
            } else {
                openSearch();
            }
        });
        header.findViewById(R.id.hero).setOnClickListener(v -> openSearch());
        header.findViewById(R.id.btn_more).setOnClickListener(v -> {
            PopupMenu menu = new PopupMenu(this, v);
            menu.getMenu().add(R.string.privacy_options);
            menu.setOnMenuItemClickListener(item -> {
                AdsConsent.showPrivacyOptions(this);
                return true;
            });
            menu.show();
        });

        TextInputEditText input = header.findViewById(R.id.search_input);
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                query = s.toString();
                refreshList();
            }
        });

        ChipGroup group = header.findViewById(R.id.filter_group);
        group.setOnCheckedStateChangeListener((g, ids) -> {
            int id = ids.isEmpty() ? R.id.chip_user : ids.get(0);
            filter = id == R.id.chip_system ? AppUtils.Filter.SYSTEM
                    : id == R.id.chip_all ? AppUtils.Filter.ALL : AppUtils.Filter.USER;
            refreshList();
        });
        ((Chip) header.findViewById(R.id.chip_size)).setOnCheckedChangeListener((c, checked) -> {
            largestFirst = checked;
            refreshList();
        });
    }

    private void openSearch() {
        View box = header.findViewById(R.id.search_box);
        box.setVisibility(View.VISIBLE);
        // While searching, results go straight under the field and the banner steps aside for the keyboard.
        header.findViewById(R.id.hero).setVisibility(View.GONE);
        findViewById(R.id.adContainer).setVisibility(View.GONE);
        View input = header.findViewById(R.id.search_input);
        input.requestFocus();
        InputMethodManager imm = getSystemService(InputMethodManager.class);
        if (imm != null) {
            imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void closeSearch() {
        TextInputEditText input = header.findViewById(R.id.search_input);
        input.setText("");
        InputMethodManager imm = getSystemService(InputMethodManager.class);
        if (imm != null) {
            imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
        }
        header.findViewById(R.id.search_box).setVisibility(View.GONE);
        header.findViewById(R.id.hero).setVisibility(View.VISIBLE);
        findViewById(R.id.adContainer).setVisibility(View.VISIBLE);
    }

    private void loadApps() {
        final PackageManager pm = getApplicationContext().getPackageManager();
        compositeDisposable.add(Single.fromCallable(() -> {
                    List<App> list = new ArrayList<>();
                    for (ApplicationInfo info : pm.getInstalledApplications(PackageManager.GET_META_DATA)) {
                        String name = String.valueOf(pm.getApplicationLabel(info));
                        if (name.isEmpty()) {
                            name = info.packageName;
                        }
                        String version = null;
                        try {
                            PackageInfo pi = pm.getPackageInfo(info.packageName, 0);
                            version = pi.versionName;
                        } catch (PackageManager.NameNotFoundException ignored) {
                        }
                        boolean system = (info.flags & ApplicationInfo.FLAG_SYSTEM) != 0
                                && (info.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0;
                        boolean split = info.splitSourceDirs != null && info.splitSourceDirs.length > 0;
                        list.add(new App(name, pm.getApplicationIcon(info), info.sourceDir,
                                new File(info.sourceDir).length(), info.packageName, version, system, split));
                    }
                    return list;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(list -> {
                    apps.clear();
                    apps.addAll(list);
                    loaded = true;
                    int mine = AppUtils.countUserApps(apps);
                    ((TextView) header.findViewById(R.id.hero_sub)).setText(
                            getResources().getQuantityString(R.plurals.hero_sub, mine, mine));
                    refreshList();
                }, e -> Log.e("MainActivity", "Failed to load installed apps", e)));
    }

    private void refreshList() {
        List<App> shown = AppUtils.select(apps, query, filter, largestFirst);
        appAdapter.setApps(shown);
        ((TextView) header.findViewById(R.id.section_title)).setText(filter == AppUtils.Filter.SYSTEM
                ? R.string.section_system : filter == AppUtils.Filter.ALL ? R.string.section_all : R.string.section_user);
        ((TextView) header.findViewById(R.id.section_count)).setText(
                loaded ? getResources().getQuantityString(R.plurals.section_count, shown.size(), shown.size()) : "");
        header.findViewById(R.id.empty).setVisibility(loaded && shown.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showAppSheet(App app) {
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        View v = LayoutInflater.from(this).inflate(R.layout.sheet_app, null, false);
        ((ImageView) v.findViewById(R.id.sheet_icon)).setImageDrawable(app.getIcon());
        ((TextView) v.findViewById(R.id.sheet_name)).setText(app.getName());
        String size = AppAdapter.sizeText(this, app.getApkSize());
        ((TextView) v.findViewById(R.id.sheet_meta)).setText(app.getVersionName() == null ? size
                : getString(R.string.row_meta, size, app.getVersionName()));
        v.findViewById(R.id.sheet_split_note).setVisibility(app.isSplit() ? View.VISIBLE : View.GONE);
        v.findViewById(R.id.sheet_share).setOnClickListener(b -> {
            sheet.dismiss();
            // Natural break point: frequency-capped interstitial, then share.
            InterstitialHelper.showIfReady(this, () -> shareApk(app.getApkPath()));
        });
        v.findViewById(R.id.sheet_save).setOnClickListener(b -> {
            sheet.dismiss();
            pendingSave = app;
            try {
                saveApk.launch(AppUtils.apkFileName(app.getName(), app.getVersionName()));
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, R.string.save_failed, Toast.LENGTH_SHORT).show();
            }
        });
        v.findViewById(R.id.sheet_info).setOnClickListener(b -> {
            sheet.dismiss();
            try {
                startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", app.getPackageName(), null)));
            } catch (ActivityNotFoundException e) {
                Log.w("MainActivity", "No app details screen", e);
            }
        });
        sheet.setContentView(v);
        sheet.show();
    }

    private void onSaveTarget(Uri target) {
        final App app = pendingSave;
        pendingSave = null;
        if (target == null || app == null) {
            return;
        }
        compositeDisposable.add(Single.fromCallable(() -> {
                    try (InputStream in = new FileInputStream(app.getApkPath());
                         OutputStream out = getContentResolver().openOutputStream(target)) {
                        if (out == null) {
                            throw new java.io.IOException("no output stream");
                        }
                        byte[] buf = new byte[64 * 1024];
                        int n;
                        while ((n = in.read(buf)) > 0) {
                            out.write(buf, 0, n);
                        }
                    }
                    return true;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(ok -> InterstitialHelper.showIfReady(this, () -> Toast.makeText(this,
                                getString(R.string.saved_ok, AppUtils.apkFileName(app.getName(), app.getVersionName())),
                                Toast.LENGTH_SHORT).show()),
                        e -> {
                            Log.w("MainActivity", "Save failed", e);
                            Toast.makeText(this, R.string.save_failed, Toast.LENGTH_SHORT).show();
                        }));
    }

    private void shareApk(String apkPath) {
        if (apkPath == null) {
            return;
        }
        try {
            Intent share = new Intent(Intent.ACTION_SEND);
            share.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(
                    this, BuildConfig.APPLICATION_ID + ".provider", new File(apkPath)));
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            share.setType("application/vnd.android.package-archive");
            startActivity(Intent.createChooser(share, getString(R.string.share_chooser)));
        } catch (IllegalArgumentException | ActivityNotFoundException e) {
            Log.w("MainActivity", "Unable to share " + apkPath, e);
            Toast.makeText(this, R.string.share_failed, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        if (adView != null) {
            adView.destroy();
        }
        super.onDestroy();
        compositeDisposable.dispose();
    }

    /** Single-item adapter that hosts the scrolling header view. */
    private static final class HeaderAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final View view;

        HeaderAdapter(View view) {
            this.view = view;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new RecyclerView.ViewHolder(view) {
            };
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        }

        @Override
        public int getItemCount() {
            return 1;
        }

        @Override
        public int getItemViewType(int position) {
            return R.layout.item_header;
        }
    }
}

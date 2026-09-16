package com.juniorvariasi.control;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final String PREFS = "junior_variasi_prefs";
    private static final String KEY_IPS = "module_ips";
    private static final String OLD_KEY_IP = "module_ip";
    private static final String DEFAULT_IP = "10.206.200.160";
    private static final long SPLASH_MS = 1100;

    private WebView webView;
    private SharedPreferences prefs;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        hideSystemUi();
        prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        migrateOldIp();
        showSplash();
        main.postDelayed(this::showHome, SPLASH_MS);
    }

    private void migrateOldIp() {
        String old = normalizeIp(prefs.getString(OLD_KEY_IP, ""));
        if (!old.isEmpty()) addIp(old);
    }

    private void hideSystemUi() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    private TextView text(String s, float size, boolean bold) {
        TextView v = new TextView(this); v.setText(s); v.setTextColor(Color.WHITE); v.setTextSize(size);
        v.setGravity(Gravity.CENTER); v.setPadding(dp(12), dp(8), dp(12), dp(8));
        if (bold) v.setTypeface(null, android.graphics.Typeface.BOLD); return v;
    }

    private void showSplash() {
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setGravity(Gravity.CENTER);
        box.setBackgroundColor(Color.BLACK);
        TextView logo = text("JV", 54, true); logo.setBackgroundColor(Color.rgb(110, 0, 55));
        LinearLayout.LayoutParams lpLogo = new LinearLayout.LayoutParams(dp(130), dp(130)); lpLogo.bottomMargin = dp(22);
        box.addView(logo, lpLogo); box.addView(text("JUNIOR VARIASI", 28, true)); setContentView(box);
    }

    private void showHome() {
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setPadding(dp(22), dp(45), dp(22), dp(22)); box.setBackgroundColor(Color.BLACK);
        TextView logo = text("JV", 38, true); logo.setBackgroundColor(Color.rgb(110, 0, 55));
        LinearLayout.LayoutParams lpLogo = new LinearLayout.LayoutParams(dp(92), dp(92)); lpLogo.gravity = Gravity.CENTER_HORIZONTAL;
        box.addView(logo, lpLogo); box.addView(text("JUNIOR VARIASI", 26, true));
        TextView list = text(ipListText(), 16, false); box.addView(list, new LinearLayout.LayoutParams(-1, -2));
        Button auto = new Button(this); auto.setText("AUTO COBA"); auto.setOnClickListener(v -> autoTry(auto)); box.addView(auto, new LinearLayout.LayoutParams(-1, dp(58)));
        Button add = new Button(this); add.setText("TAMBAH IP"); add.setOnClickListener(v -> showAddIpDialog());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(58)); lp.topMargin = dp(12); box.addView(add, lp);
        setContentView(box);
    }

    private String ipListText() {
        ArrayList<String> ips = getIps(); if (ips.isEmpty()) return "Belum ada IP modul";
        StringBuilder b = new StringBuilder("IP MODUL TERSIMPAN\n"); for (String ip : ips) b.append("\n").append(ip); return b.toString();
    }

    private void showAddIpDialog() {
        EditText input = new EditText(this); input.setSingleLine(true); input.setInputType(InputType.TYPE_CLASS_TEXT); input.setHint("Contoh: 10.93.2.91");
        AlertDialog d = new AlertDialog.Builder(this).setTitle("TAMBAH IP MODUL").setView(input)
                .setPositiveButton("SIMPAN", null).setNegativeButton("BATAL", null).create();
        d.setOnShowListener(x -> d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String ip = normalizeIp(input.getText().toString()); if (ip.isEmpty()) { input.setError("IP belum diisi"); return; }
            addIp(ip); d.dismiss(); showHome();
        })); d.show();
    }

    private void autoTry(Button button) {
        ArrayList<String> ips = getIps(); if (ips.isEmpty()) { showAddIpDialog(); return; }
        button.setEnabled(false); button.setText("MENCARI...");
        worker.execute(() -> {
            String found = null;
            for (String ip : ips) if (isWebAlive(ip)) { found = ip; break; }
            final String hit = found;
            main.post(() -> { button.setEnabled(true); button.setText("AUTO COBA");
                if (hit != null) openWebControl(hit); else Toast.makeText(this, "Web Control belum ketemu", Toast.LENGTH_LONG).show(); });
        });
    }

    private boolean isWebAlive(String ip) {
        HttpURLConnection c = null;
        try { c = (HttpURLConnection)new URL("http://" + ip + "/").openConnection(); c.setConnectTimeout(1200); c.setReadTimeout(1200);
            c.setInstanceFollowRedirects(true); c.setRequestMethod("GET"); int code = c.getResponseCode(); return code >= 200 && code < 500;
        } catch (Exception e) { return false; } finally { if (c != null) c.disconnect(); }
    }

    private void openWebControl(String ip) {
        webView = new WebView(this); WebSettings s = webView.getSettings(); s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true);
        s.setLoadWithOverviewMode(true); s.setUseWideViewPort(true); s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webView.setWebViewClient(new WebViewClient()); webView.setWebChromeClient(new WebChromeClient()); webView.setBackgroundColor(Color.BLACK);
        webView.setOnLongClickListener(v -> { showHome(); return true; }); webView.setLongClickable(true); setContentView(webView); webView.loadUrl("http://" + ip + "/");
    }

    private ArrayList<String> getIps() { return new ArrayList<>(prefs.getStringSet(KEY_IPS, new LinkedHashSet<>())); }
    private void addIp(String ip) { Set<String> s = new LinkedHashSet<>(prefs.getStringSet(KEY_IPS, new LinkedHashSet<>())); s.add(ip); prefs.edit().putStringSet(KEY_IPS, s).apply(); }
    private String normalizeIp(String v) { if (v == null) return ""; String s=v.trim().replace("http://","").replace("https://",""); int p=s.indexOf('/'); if(p>=0)s=s.substring(0,p); return s.trim(); }
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    @Override public void onBackPressed() { if (webView != null && webView.canGoBack()) webView.goBack(); else { webView=null; showHome(); } }
    @Override protected void onResume() { super.onResume(); hideSystemUi(); }
    @Override protected void onDestroy() { worker.shutdownNow(); super.onDestroy(); }
}

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
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class MainActivity extends Activity {
    private static final String PREFS = "junior_variasi_prefs";
    private static final String KEY_IP = "module_ip";
    private static final String DEFAULT_IP = "10.206.200.160";
    private static final long SPLASH_MS = 1300;

    private WebView webView;
    private FrameLayout root;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        hideSystemUi();

        prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        showSplash();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            String savedIp = prefs.getString(KEY_IP, "");
            if (savedIp == null || savedIp.trim().isEmpty()) {
                showIpDialog(DEFAULT_IP, true);
            } else {
                openWebControl(savedIp);
            }
        }, SPLASH_MS);
    }

    private void hideSystemUi() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
    }

    private void showSplash() {
        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        ImageView logo = new ImageView(this);
        logo.setImageResource(com.juniorvariasi.control.R.drawable.splash_logo);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int pad = dp(28);
        logo.setPadding(pad, pad, pad, pad);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        lp.gravity = Gravity.CENTER;
        root.addView(logo, lp);
        setContentView(root);
    }

    private void openWebControl(String rawIp) {
        String ip = normalizeIp(rawIp);
        prefs.edit().putString(KEY_IP, ip).apply();

        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        webView = new WebView(this);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);

        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        webView.setBackgroundColor(Color.BLACK);

        webView.setOnLongClickListener(v -> {
            showIpDialog(prefs.getString(KEY_IP, DEFAULT_IP), false);
            return true;
        });
        webView.setLongClickable(true);

        root.addView(webView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        setContentView(root);

        webView.loadUrl("http://" + ip + "/");
    }

    private void showIpDialog(String currentIp, boolean firstRun) {
        final EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setText(currentIp == null ? DEFAULT_IP : currentIp);
        input.setSelection(input.getText().length());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("Contoh: 10.206.200.160");
        int p = dp(18);
        input.setPadding(p, p, p, p);

        FrameLayout box = new FrameLayout(this);
        box.setPadding(dp(18), dp(4), dp(18), 0);
        box.addView(input, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        AlertDialog.Builder b = new AlertDialog.Builder(this)
                .setTitle("IP MODUL JUNIOR")
                .setMessage(firstRun ? "Masukkan IP modul. IP ini akan disimpan di HP." : "Ganti IP modul lalu tekan HUBUNGKAN.")
                .setView(box)
                .setPositiveButton("HUBUNGKAN", null);

        if (!firstRun) {
            b.setNegativeButton("BATAL", (d, w) -> d.dismiss());
        }

        AlertDialog dialog = b.create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String ip = normalizeIp(input.getText().toString());
            if (ip.isEmpty()) {
                input.setError("IP belum diisi");
                return;
            }
            dialog.dismiss();
            openWebControl(ip);
        }));
        dialog.setCanceledOnTouchOutside(!firstRun);
        dialog.show();
    }

    private String normalizeIp(String value) {
        if (value == null) return "";
        String s = value.trim();
        s = s.replace("http://", "").replace("https://", "");
        int slash = s.indexOf('/');
        if (slash >= 0) s = s.substring(0, slash);
        return s.trim();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUi();
    }
}

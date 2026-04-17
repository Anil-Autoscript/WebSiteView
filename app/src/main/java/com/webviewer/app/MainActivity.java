package com.webviewer.app;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.webkit.*;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progressBar;
    private EditText urlEditText;
    private ImageButton btnBack, btnForward, btnRefresh, btnHome;
    private View errorView;
    private TextView errorMessage;
    private View loadingOverlay;
    private SharedPreferences prefs;

    private static final String PREF_CURRENT_URL = "current_url";
    private static final String PREF_HOME_URL = "home_url";
    private static final String DEFAULT_URL = "https://www.google.com";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("WebViewerPrefs", MODE_PRIVATE);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        webView = findViewById(R.id.webview);
        swipeRefresh = findViewById(R.id.swipe_refresh);
        progressBar = findViewById(R.id.progress_bar);
        urlEditText = findViewById(R.id.url_edit_text);
        btnBack = findViewById(R.id.btn_back);
        btnForward = findViewById(R.id.btn_forward);
        btnRefresh = findViewById(R.id.btn_refresh);
        btnHome = findViewById(R.id.btn_home);
        errorView = findViewById(R.id.error_view);
        errorMessage = findViewById(R.id.error_message);
        loadingOverlay = findViewById(R.id.loading_overlay);

        setupWebView();
        setupNavigationButtons();
        setupUrlBar();
        setupSwipeRefresh();

        // Load URL from intent or saved preference
        String urlToLoad = getIntent().getStringExtra("url");
        if (urlToLoad == null || urlToLoad.isEmpty()) {
            urlToLoad = prefs.getString(PREF_CURRENT_URL, DEFAULT_URL);
        }
        loadUrl(urlToLoad);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setDatabaseEnabled(true);
        settings.setGeolocationEnabled(true);
        settings.setSupportZoom(true);
        settings.setTextZoom(100);
        settings.setUserAgentString(settings.getUserAgentString() + " WebViewerApp/1.0");

        // Modern rendering
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                errorView.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
                updateUrlBar(url);
                updateNavButtons();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                updateUrlBar(url);
                updateNavButtons();
                // Save current URL
                prefs.edit().putString(PREF_CURRENT_URL, url).apply();
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    progressBar.setVisibility(View.GONE);
                    swipeRefresh.setRefreshing(false);
                    showError("Unable to load page. Check your internet connection.\n\nError: " + error.getDescription());
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                // Handle tel: and mailto: links
                if (url.startsWith("tel:") || url.startsWith("mailto:") || url.startsWith("intent:")) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(intent);
                    } catch (Exception e) {
                        // Ignore
                    }
                    return true;
                }
                return false;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
                if (newProgress == 100) {
                    new Handler().postDelayed(() -> progressBar.setVisibility(View.GONE), 200);
                }
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                // Optionally update toolbar title
            }

            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Alert")
                        .setMessage(message)
                        .setPositiveButton("OK", (d, w) -> result.confirm())
                        .setCancelable(false)
                        .show();
                return true;
            }

            @Override
            public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Confirm")
                        .setMessage(message)
                        .setPositiveButton("OK", (d, w) -> result.confirm())
                        .setNegativeButton("Cancel", (d, w) -> result.cancel())
                        .show();
                return true;
            }
        });
    }

    private void setupNavigationButtons() {
        btnBack.setOnClickListener(v -> {
            if (webView.canGoBack()) webView.goBack();
        });

        btnForward.setOnClickListener(v -> {
            if (webView.canGoForward()) webView.goForward();
        });

        btnRefresh.setOnClickListener(v -> {
            errorView.setVisibility(View.GONE);
            webView.reload();
        });

        btnHome.setOnClickListener(v -> {
            String homeUrl = prefs.getString(PREF_HOME_URL, DEFAULT_URL);
            loadUrl(homeUrl);
        });
    }

    private void setupUrlBar() {
        urlEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                String input = urlEditText.getText().toString().trim();
                if (!input.isEmpty()) {
                    loadUrl(formatUrl(input));
                }
                urlEditText.clearFocus();
                return true;
            }
            return false;
        });

        urlEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                urlEditText.selectAll();
            }
        });
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setColorSchemeResources(
                R.color.accent_primary,
                R.color.accent_secondary,
                R.color.accent_tertiary
        );
        swipeRefresh.setOnRefreshListener(() -> {
            errorView.setVisibility(View.GONE);
            webView.reload();
        });
    }

    private String formatUrl(String input) {
        if (input.startsWith("http://") || input.startsWith("https://")) {
            return input;
        } else if (input.contains(".") && !input.contains(" ")) {
            return "https://" + input;
        } else {
            // Treat as search query
            return "https://www.google.com/search?q=" + Uri.encode(input);
        }
    }

    private void loadUrl(String url) {
        if (url == null || url.isEmpty()) url = DEFAULT_URL;
        String formatted = formatUrl(url);
        webView.loadUrl(formatted);
        updateUrlBar(formatted);
    }

    private void updateUrlBar(String url) {
        if (!urlEditText.hasFocus()) {
            urlEditText.setText(url);
        }
    }

    private void updateNavButtons() {
        btnBack.setAlpha(webView.canGoBack() ? 1.0f : 0.4f);
        btnForward.setAlpha(webView.canGoForward() ? 1.0f : 0.4f);
    }

    private void showError(String message) {
        errorView.setVisibility(View.VISIBLE);
        errorMessage.setText(message);
        Button retryBtn = errorView.findViewById(R.id.btn_retry);
        retryBtn.setOnClickListener(v -> {
            errorView.setVisibility(View.GONE);
            webView.reload();
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_urls) {
            Intent intent = new Intent(this, UrlManagerActivity.class);
            startActivityForResult(intent, 100);
            return true;
        } else if (id == R.id.menu_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (id == R.id.menu_share) {
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_TEXT, webView.getUrl());
            startActivity(Intent.createChooser(share, "Share URL"));
            return true;
        } else if (id == R.id.menu_open_browser) {
            Intent browser = new Intent(Intent.ACTION_VIEW, Uri.parse(webView.getUrl()));
            startActivity(browser);
            return true;
        } else if (id == R.id.menu_clear_cache) {
            webView.clearCache(true);
            webView.clearHistory();
            Toast.makeText(this, "Cache cleared", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            String url = data.getStringExtra("selected_url");
            if (url != null && !url.isEmpty()) {
                loadUrl(url);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Exit App")
                    .setMessage("Are you sure you want to exit?")
                    .setPositiveButton("Exit", (d, w) -> super.onBackPressed())
                    .setNegativeButton("Stay", null)
                    .show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        webView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }
}

package com.webviewer.app;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private EditText homeUrlInput;
    private Switch jsSwitch, desktopModeSwitch, cacheSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences("WebViewerPrefs", MODE_PRIVATE);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Settings");
        }

        homeUrlInput = findViewById(R.id.home_url_input);
        jsSwitch = findViewById(R.id.js_switch);
        desktopModeSwitch = findViewById(R.id.desktop_mode_switch);
        cacheSwitch = findViewById(R.id.cache_switch);
        Button saveBtn = findViewById(R.id.btn_save);

        // Load current settings
        homeUrlInput.setText(prefs.getString("home_url", "https://www.google.com"));
        jsSwitch.setChecked(prefs.getBoolean("js_enabled", true));
        desktopModeSwitch.setChecked(prefs.getBoolean("desktop_mode", false));
        cacheSwitch.setChecked(prefs.getBoolean("cache_enabled", true));

        saveBtn.setOnClickListener(v -> {
            String homeUrl = homeUrlInput.getText().toString().trim();
            if (!homeUrl.startsWith("http")) homeUrl = "https://" + homeUrl;

            prefs.edit()
                    .putString("home_url", homeUrl)
                    .putBoolean("js_enabled", jsSwitch.isChecked())
                    .putBoolean("desktop_mode", desktopModeSwitch.isChecked())
                    .putBoolean("cache_enabled", cacheSwitch.isChecked())
                    .apply();

            Toast.makeText(this, "Settings saved!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}

package com.example.ctrl_alt_elite;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;

public class SettingsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        // This puts activity_settings INSIDE activity_base's FrameLayout
        setActivityContent(R.layout.activity_settings);

    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-sync the navigation bar highlight every time the screen comes to the foreground
        setupNavigation();
    }
}

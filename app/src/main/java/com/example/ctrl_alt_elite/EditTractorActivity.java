package com.example.ctrl_alt_elite;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;

public class EditTractorActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        // This puts activity_main INSIDE activity_base's FrameLayout
        setActivityContent(R.layout.activity_edit_tractor);

    }
    @Override
    protected void onResume() {
        super.onResume();
        // Re-sync the navigation bar highlight every time the screen comes to the foreground
        setupNavigation();
    }
}
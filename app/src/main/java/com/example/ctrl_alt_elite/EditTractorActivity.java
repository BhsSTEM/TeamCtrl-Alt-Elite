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
}
package com.example.ctrl_alt_elite;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;

public class EditTractorActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        // This puts activity_main INSIDE activity_base's FrameLayout
        setActivityContent(R.layout.activity_edit_tractor);

        Button button = (Button) findViewById(R.id.btn_add_tractor);
        button.setOnClickListener(this::onClick);
    }
        public void onClick(View v) {
            Intent intent = new Intent(EditTractorActivity.this, AddTractorActivity.class);
            startActivity(intent);
        }
    @Override
    protected void onResume() {
        super.onResume();
        // Re-sync the navigation bar highlight every time the screen comes to the foreground
        setupNavigation();
    }
}
package com.example.ctrl_alt_elite;

import android.content.Intent;
import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_base);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupNavigation();
    }

    protected void setActivityContent(int layoutResId) {
        setContentView(R.layout.activity_base);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        FrameLayout contentFrame = findViewById(R.id.activity_content);
        if (contentFrame != null) {
            getLayoutInflater().inflate(layoutResId, contentFrame, true);
        }
        setupNavigation();
    }

    public void setupNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);

        if (bottomNav != null) {
            bottomNav.setOnItemSelectedListener(null);

            if (this instanceof MainActivity) {
                bottomNav.setSelectedItemId(R.id.nav_home);
            } else if (this instanceof SettingsActivity) {
                bottomNav.setSelectedItemId(R.id.nav_settings);
            } else if (this instanceof evansMapActivity) {
                bottomNav.setSelectedItemId(R.id.nav_map);
            } else if (this instanceof ManageTractorsActivity || this instanceof AddTractorActivity || this instanceof EditTractorActivity){
                bottomNav.setSelectedItemId(R.id.nav_machines);
            } else if (this instanceof TasksActivity) {
                bottomNav.setSelectedItemId(R.id.nav_tasks);
            }

            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();

                if (id == R.id.nav_home) {
                    if (!(this instanceof MainActivity)) {
                        Intent intent = new Intent(this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                        startActivity(intent);
                        overridePendingTransition(0, 0);
                    }
                    return true;
                } else if (id == R.id.nav_map) {
                    if (!(this instanceof evansMapActivity)) {
                        Intent intent = new Intent(this, evansMapActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                        startActivity(intent);
                        overridePendingTransition(0, 0);
                    }
                    return true;
                } else if (id == R.id.nav_settings) {
                    if (!(this instanceof SettingsActivity)) {
                        Intent intent = new Intent(this, SettingsActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                        startActivity(intent);
                        overridePendingTransition(0, 0);
                    }
                    return true;
                } else if (id == R.id.nav_machines) {
                    if (!(this instanceof ManageTractorsActivity)) {
                        Intent intent = new Intent(this, ManageTractorsActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                        startActivity(intent);
                        overridePendingTransition(0, 0);
                    }
                    return true;
                } else if (id == R.id.nav_tasks) {
                    if (!(this instanceof TasksActivity)) {
                        Intent intent = new Intent(this, TasksActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                        startActivity(intent);
                        overridePendingTransition(0, 0);
                    }
                    return true;
                }
                return false;
            });
        }
    }
}

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

    // Call this INSTEAD of setContentView in your other activities
    protected void setActivityContent(int layoutResId) {
        // 1. Inflate the base layout first
        setContentView(R.layout.activity_base);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // Force bottom padding to 0 so the menu bar touches the floor
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        // 2. Find the FrameLayout "hole" in the base layout
        FrameLayout contentFrame = findViewById(R.id.activity_content);

        // 3. Put the specific activity layout (like activity_main) inside it
        if (contentFrame != null) {
            getLayoutInflater().inflate(layoutResId, contentFrame, true);
        }
        // 4. Initialize the nav bar logic (your existing listener code)
        setupNavigation();
    }

    private void setupNavigation() {
        // Finds the bottom navigation XML file
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);

        if (bottomNav != null) {
            // Sets up a listener for the bottom bar so it can find what gets pressed
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
                }

                // Add your Map and Settings checks here once those activities exist
                else if (id == R.id.nav_map) {
                    /*
                    if (!(this instanceof MapActivity)) {
                        Intent intent = new Intent(this, MapActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                        startActivity(intent);
                        overridePendingTransition(0, 0);
                    }

                     */
                    return true;
                }
                else if (id == R.id.nav_settings) {
                    /*
                    if (!(this instanceof SettingsActivity)) {
                        Intent intent = new Intent(this, SettingsActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                        startActivity(intent);
                        overridePendingTransition(0, 0);
                    }

                     */
                    return true;
                }

                return false;
            });
        }
    }
}
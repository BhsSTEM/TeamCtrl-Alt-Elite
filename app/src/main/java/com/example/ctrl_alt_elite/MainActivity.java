package com.example.ctrl_alt_elite;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        // This puts activity_main INSIDE activity_base's FrameLayout
        setActivityContent(R.layout.activity_main);

        // Highlight the Home icon
        BottomNavigationView nav = findViewById(R.id.bottom_nav);
        if (nav != null) {
           nav.setSelectedItemId(R.id.nav_home);
        }
    }
}
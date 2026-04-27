package com.example.ctrl_alt_elite;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SettingsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActivityContent(R.layout.activity_settings);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();

        Button logoutButton = findViewById(R.id.signOutButton);
        MaterialSwitch darkModeSwitch = findViewById(R.id.switch1);
        TextView usernameText = findViewById(R.id.textView3);

        // Load current user info
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            if (usernameText != null) {
                usernameText.setText(user.getEmail());
            }
        }

        // Load current dark mode preference using keys from BaseActivity
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean(DARK_MODE_KEY, false);
        darkModeSwitch.setChecked(isDarkMode);

        // Handle logout button
        if (logoutButton != null) {
            logoutButton.setOnClickListener(v -> {
                mAuth.signOut();
                startActivity(new Intent(SettingsActivity.this, LoginPage.class));
                finish();
            });
        }

        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save preference
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(DARK_MODE_KEY, isChecked);
            editor.apply();

            // Apply theme immediately
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });
    }
}
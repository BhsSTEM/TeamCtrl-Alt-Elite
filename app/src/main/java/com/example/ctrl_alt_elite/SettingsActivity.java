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
import com.google.firebase.firestore.FirebaseFirestore;

public class SettingsActivity extends BaseActivity {

    private FirebaseFirestore userdb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActivityContent(R.layout.activity_settings);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        userdb = FirebaseFirestore.getInstance("sign-up");

        Button logoutButton = findViewById(R.id.signOutButton);
        MaterialSwitch darkModeSwitch = findViewById(R.id.switch1);
        TextView usernameText = findViewById(R.id.textView3);
        TextView roleText = findViewById(R.id.textView4);
        TextView farmPinText = findViewById(R.id.settingsFarmPin);

        // Load current user info from Firebase Auth and Firestore
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            userdb.collection("users").document(user.getUid()).get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    String name = task.getResult().getString("name");
                    String role = task.getResult().getString("role");
                    String farmId = task.getResult().getString("farmId");

                    if (usernameText != null) {
                        usernameText.setText("Username: " + (name != null ? name : user.getEmail()));
                    }
                    if (roleText != null) {
                        roleText.setText("Role: " + (role != null && !role.isEmpty() ? role : "Not Assigned"));
                    }
                    if (farmPinText != null) {
                        farmPinText.setText("Company ID: " + (farmId != null ? farmId : "None"));
                    }
                }
            });
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
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
import com.google.firebase.firestore.DocumentSnapshot;

public class SettingsActivity extends BaseActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private Button logoutButton;
    private MaterialSwitch darkModeSwitch;
    private TextView usernameText;
    private TextView roleText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActivityContent(R.layout.activity_settings);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        
        logoutButton = findViewById(R.id.signOutButton);
        darkModeSwitch = findViewById(R.id.switch1);
        usernameText = findViewById(R.id.textView3);
        roleText = findViewById(R.id.textView4);

        // Load current user info
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            if (usernameText != null) {
                usernameText.setText(user.getEmail());
            }
            
            // Fetch role from Firestore
            db.collection("users").document(user.getUid()).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot document = task.getResult();
                        String role = document.getString("role");
                        if (roleText != null) {
                            if (role != null && !role.isEmpty()) {
                                roleText.setText(role);
                            } else {
                                roleText.setText(getString(R.string.settings_no_role));
                            }
                        }
                    } else {
                        if (roleText != null) {
                            roleText.setText(getString(R.string.settings_no_role));
                        }
                    }
                });
        }

        // Load current dark mode preference
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

        // Handle dark mode switch
        if (darkModeSwitch != null) {
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
}

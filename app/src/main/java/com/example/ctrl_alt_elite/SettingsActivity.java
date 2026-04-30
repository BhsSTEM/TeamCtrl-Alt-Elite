package com.example.ctrl_alt_elite;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

public class SettingsActivity extends BaseActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private Button logoutButton;
    private TextView usernameText;
    private TextView roleText;
    private TextView farmPinText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActivityContent(R.layout.activity_settings);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        
        logoutButton = findViewById(R.id.signOutButton);
        usernameText = findViewById(R.id.textView3);
        roleText = findViewById(R.id.textView4);
        farmPinText = findViewById(R.id.settingsFarmPin);

        // Load current user info
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            if (usernameText != null) {
                usernameText.setText(user.getEmail());
            }
            
            // Fetch role and farmId from Firestore
            db.collection("users").document(user.getUid()).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot document = task.getResult();
                        String role = document.getString("role");
                        String farmId = document.getString("farmId");

                        if (roleText != null) {
                            if (role != null && !role.isEmpty()) {
                                roleText.setText(role);
                            } else {
                                roleText.setText(getString(R.string.settings_no_role));
                            }
                        }

                        if (farmPinText != null) {
                            if (farmId != null && !farmId.isEmpty()) {
                                farmPinText.setText("Company ID: " + farmId);
                            } else {
                                farmPinText.setText("Company ID: N/A");
                            }
                        }
                    } else {
                        if (roleText != null) {
                            roleText.setText(getString(R.string.settings_no_role));
                        }
                    }
                });
        }

        // Handle logout button
        if (logoutButton != null) {
            logoutButton.setOnClickListener(v -> {
                mAuth.signOut();
                startActivity(new Intent(SettingsActivity.this, LoginPage.class));
                finish();
            });
        }
    }
}

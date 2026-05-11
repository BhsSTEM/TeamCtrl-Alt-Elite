package com.example.ctrl_alt_elite;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class SettingsActivity extends BaseActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private Button logoutButton;
    private Button changeCompanyIdButton;
    private Button changeRoleButton;
    private TextView usernameText;
    private TextView roleText;
    private TextView farmPinText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActivityContent(R.layout.activity_settings);

        mAuth = FirebaseAuth.getInstance();
        // Ensure this matches the instance name used in signUpPage
        db = FirebaseFirestore.getInstance("sign-up");
        
        logoutButton = findViewById(R.id.signOutButton);
        changeCompanyIdButton = findViewById(R.id.changeCompanyIdButton);
        changeRoleButton = findViewById(R.id.changeRoleButton);
        usernameText = findViewById(R.id.textView3);
        roleText = findViewById(R.id.textView4);
        farmPinText = findViewById(R.id.settingsFarmPin);

        // Load current user info
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            if (usernameText != null) {
                usernameText.setText(user.getEmail());
            }
            
            loadUserData(user.getUid());
        }

        // Handle change role button
        if (changeRoleButton != null) {
            changeRoleButton.setOnClickListener(v -> {
                showChangeRoleDialog();
            });
        }

        // Handle change company id button
        if (changeCompanyIdButton != null) {
            changeCompanyIdButton.setOnClickListener(v -> {
                showChangeCompanyIdDialog();
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

    private void loadUserData(String uid) {
        // Fetch role and Company from Firestore
        db.collection("users").document(uid).get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    DocumentSnapshot document = task.getResult();
                    String role = document.getString("role");
                    String companyId = document.getString("companyId");

                    if (roleText != null) {
                        if (role != null && !role.isEmpty()) {
                            roleText.setText(role);
                        } else {
                            roleText.setText(getString(R.string.settings_no_role));
                        }
                    }

                    if (farmPinText != null) {
                        if (companyId != null && !companyId.isEmpty()) {
                            farmPinText.setText("Company ID: " + companyId);
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

    private void showChangeRoleDialog() {
        String[] roles = {getString(R.string.role_owner), getString(R.string.role_operator)};
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.settings_change_role_title);
        builder.setItems(roles, (dialog, which) -> {
            String selectedRole = roles[which];
            updateRole(selectedRole);
        });
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void updateRole(String newRole) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            Map<String, Object> data = new HashMap<>();
            data.put("role", newRole);

            // Using set with SetOptions.merge() so it works even if the document doesn't exist
            db.collection("users").document(user.getUid())
                .set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(SettingsActivity.this, R.string.settings_role_update_success, Toast.LENGTH_SHORT).show();
                    loadUserData(user.getUid());
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(SettingsActivity.this, R.string.settings_role_update_error, Toast.LENGTH_SHORT).show();
                });
        }
    }

    private void showChangeCompanyIdDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.settings_change_company_id_title);

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint(R.string.settings_new_company_id_hint);
        input.setFilters(new InputFilter[] { new InputFilter.LengthFilter(6) });
        
        builder.setView(input);

        builder.setPositiveButton(R.string.settings_save, (dialog, which) -> {
            String newCompanyId = input.getText().toString().trim();
            if (newCompanyId.isEmpty()) {
                Toast.makeText(SettingsActivity.this, R.string.settings_id_empty_error, Toast.LENGTH_SHORT).show();
            } else if (newCompanyId.length() != 6) {
                Toast.makeText(SettingsActivity.this, R.string.settings_id_length_error, Toast.LENGTH_SHORT).show();
            } else {
                updateCompanyId(newCompanyId);
            }
        });
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void updateCompanyId(String newCompanyId) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            Map<String, Object> data = new HashMap<>();
            data.put("companyId", newCompanyId);

            db.collection("users").document(user.getUid())
                .set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(SettingsActivity.this, R.string.settings_id_update_success, Toast.LENGTH_SHORT).show();
                    loadUserData(user.getUid());
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(SettingsActivity.this, R.string.settings_id_update_error, Toast.LENGTH_SHORT).show();
                });
        }
    }
}

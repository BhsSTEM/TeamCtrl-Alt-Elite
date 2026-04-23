package com.example.ctrl_alt_elite;

import static android.content.ContentValues.TAG;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class signUpPage extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    EditText nameSignUpText;
    EditText emailSignUpText;
    EditText passwordSignUpText;
    Button signUpButton;
    ImageButton backButton3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.page_signup);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        // Pointing to the specific 'sign-ons' database
        db = FirebaseFirestore.getInstance("sign-up");

        // UI elements
        nameSignUpText = findViewById(R.id.nameSignUpInput);
        emailSignUpText = findViewById(R.id.emailSignUpInput);
        passwordSignUpText = findViewById(R.id.passwordSignUpInput);
        backButton3 = findViewById(R.id.backButton3);
        signUpButton = findViewById(R.id.signUpButton);

        // Sign Up Logic
        signUpButton.setOnClickListener(v -> registerUser());

        // Back Button Logic
        backButton3.setOnClickListener(v -> {
            Intent intent = new Intent(signUpPage.this, LoginPage.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void registerUser() {
        String name = nameSignUpText.getText().toString().trim();
        String email = emailSignUpText.getText().toString().trim();
        String password = passwordSignUpText.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create user in Firebase Auth
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        saveUserToFirestore(name, email);
                    } else {
                        Toast.makeText(signUpPage.this, "Sign up failed: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUserToFirestore(String name, String email) {
        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("email", email);
        user.put("uid", mAuth.getCurrentUser().getUid());
        user.put("role", "");// Blank for now and the foreseeable future

        db.collection("users").document(mAuth.getCurrentUser().getUid())
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Account Created!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(signUpPage.this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving to Firestore", e);
                });
    }
}
package com.example.ctrl_alt_elite;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginPage extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private EditText emailInput, passwordInput;
    private Button loginBtn, goToSignUpBtn, forgotPasswordBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.page_login);

        mAuth = FirebaseAuth.getInstance();

        // Initialize UI Elements
        emailInput = findViewById(R.id.emailAddressInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginBtn = findViewById(R.id.loginConfirmButton);
        goToSignUpBtn = findViewById(R.id.createAccountButton);
        forgotPasswordBtn = findViewById(R.id.forgotPasswordButton);

        // Logic for Login Button
        loginBtn.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (!email.isEmpty() && !password.isEmpty()) {
                loginUser(email, password);
            } else {
                Toast.makeText(LoginPage.this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            }
        });

        // Logic for Sign Up Transition
        goToSignUpBtn.setOnClickListener(v -> {
            startActivity(new Intent(LoginPage.this, signUpPage.class));
        });


        forgotPasswordBtn.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();

            if (email.isEmpty()) {
                Toast.makeText(LoginPage.this, "Please enter your email to reset password", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(LoginPage.this, "Reset email sent! Check your inbox.", Toast.LENGTH_LONG).show();
                            Log.d("AUTH", "Email sent.");
                        } else {
                            Toast.makeText(LoginPage.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }

    private void loginUser(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d("AUTH", "signInWithEmail:success");
                        startActivity(new Intent(LoginPage.this, MainActivity.class));
                        finish();
                    } else {
                        Log.w("AUTH", "signInWithEmail:failure", task.getException());
                        Toast.makeText(LoginPage.this, "Authentication failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
            startActivity(new Intent(LoginPage.this, MainActivity.class));
            finish();
        }
    }
}
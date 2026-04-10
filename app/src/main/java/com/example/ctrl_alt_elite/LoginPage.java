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

    // 1. Declare Firebase and UI variables at the class level
    private FirebaseAuth mAuth;
    private EditText emailInput, passwordInput;
    private Button loginBtn, goToSignUpBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.page_login);

        // 2. Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // 3. Link variables to your XML IDs
        emailInput = findViewById(R.id.emailAddressInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginBtn = findViewById(R.id.loginConfirmButton);
        goToSignUpBtn = findViewById(R.id.createAccountButton);


        // 4. Set the Login Button Logic
        loginBtn.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (!email.isEmpty() && !password.isEmpty()) {
                loginUser(email, password);
            } else {
                Toast.makeText(LoginPage.this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            }
        });

        // 5. Link to SignUpPage
        goToSignUpBtn.setOnClickListener(v -> {
            startActivity(new Intent(LoginPage.this, signUpPage.class));
        });
    }

    private void loginUser(String email, String password) {
        // This is the core Firebase Authentication call
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Login success!
                        Log.d("AUTH", "signInWithEmail:success");
                        FirebaseUser user = mAuth.getCurrentUser();

                        // Move to the Main screen (MainActivity)
                        Intent intent = new Intent(LoginPage.this, MainActivity.class);
                        startActivity(intent);
                        finish(); // Closes Login page so user can't "back" into it
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w("AUTH", "signInWithEmail:failure", task.getException());
                        Toast.makeText(LoginPage.this, "Authentication failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is already signed in and skip login if they are
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
            startActivity(new Intent(LoginPage.this, MainActivity.class));
            finish();
        }
    }
}
package com.example.ctrl_alt_elite;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class LoginPage extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.page_login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.background), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 1. You need to find the button first
        Button createAccountButton = findViewById(R.id.createAccountButton);

        // 2. Set the click listener outside of the WindowInsets listener
        createAccountButton.setOnClickListener(v -> {
            Intent intent = new Intent(LoginPage.this, signUpPage.class);
            startActivity(intent);
        });

        Button loginButton = findViewById(R.id.loginConfirmButton);

        loginButton.setOnClickListener(v -> {
            Intent intent = new Intent(LoginPage.this, MainActivity.class);
            startActivity(intent);
        });
    }
}

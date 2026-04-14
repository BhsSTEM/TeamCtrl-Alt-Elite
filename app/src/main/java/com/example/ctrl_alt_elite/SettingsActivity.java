package com.example.ctrl_alt_elite;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;





public class SettingsActivity extends BaseActivity {

    private FirebaseAuth mAuth;
    private Button logoutButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // This puts activity_main INSIDE activity_base's FrameLayout
        setActivityContent(R.layout.activity_settings);

        mAuth = FirebaseAuth.getInstance();
        logoutButton = findViewById(R.id.signOutButton);
//makes logout button work
logoutButton.setOnClickListener(View -> {
    mAuth.signOut();
    startActivity(new Intent(SettingsActivity.this, LoginPage.class));
    finish();
});
    }


}
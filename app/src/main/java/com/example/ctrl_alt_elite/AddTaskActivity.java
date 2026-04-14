package com.example.ctrl_alt_elite;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

public class AddTaskActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task);

        // Back button
        ImageButton backButton = findViewById(R.id.backButton);
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }

        // Create Task button
        Button createTaskButton = findViewById(R.id.createTaskButton);
        if (createTaskButton != null) {
            createTaskButton.setOnClickListener(v -> {
                // For now, just go back to TasksActivity
                finish();
            });
        }
    }

    @Override
    public void finish() {
        super.finish();
        // Slide out to the left (fast)
        overridePendingTransition(0, R.anim.slide_out_left_fast);
    }
}
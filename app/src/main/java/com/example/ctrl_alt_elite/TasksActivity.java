package com.example.ctrl_alt_elite;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class TasksActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActivityContent(R.layout.activity_tasks);

        FloatingActionButton addTaskFab = findViewById(R.id.addTaskFab);
        if (addTaskFab != null) {
            addTaskFab.setOnClickListener(v -> {
                Intent intent = new Intent(TasksActivity.this, AddTaskActivity.class);
                startActivity(intent);
                // Slide in from left (fast)
                overridePendingTransition(R.anim.slide_in_left_fast, 0);
            });
        }
    }

}
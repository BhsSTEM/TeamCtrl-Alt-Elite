package com.example.ctrl_alt_elite;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AddTaskActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private TextInputEditText titleInput, descriptionInput, dueDateInput;
    private AutoCompleteTextView assignToDropdown, repeatIntervalDropdown, tractorDropdown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task);

        // Pointing to the specific 'tasks' database
        db = FirebaseFirestore.getInstance("tasks");

        titleInput = findViewById(R.id.taskTitleInput);
        descriptionInput = findViewById(R.id.taskDescriptionInput);
        dueDateInput = findViewById(R.id.taskDueDateInput);
        assignToDropdown = findViewById(R.id.assignToDropdown);
        repeatIntervalDropdown = findViewById(R.id.repeatIntervalDropdown);
        tractorDropdown = findViewById(R.id.associateTractorDropdown);

        setupDropdowns();

        // Back button
        ImageButton backButton = findViewById(R.id.backButton);
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }

        // Create Task button
        Button createTaskButton = findViewById(R.id.createTaskButton);
        if (createTaskButton != null) {
            createTaskButton.setOnClickListener(v -> saveTaskToFirestore());
        }
    }

    private void setupDropdowns() {
        // Repeat Intervals
        String[] intervals = {
                getString(R.string.repeat_none),
                getString(R.string.repeat_daily),
                getString(R.string.repeat_weekly),
                getString(R.string.repeat_monthly)
        };
        ArrayAdapter<String> intervalAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, intervals);
        repeatIntervalDropdown.setAdapter(intervalAdapter);

        // Fetch Users (for Assign To)
        db.collection("users").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<String> userEmails = new ArrayList<>();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    userEmails.add(document.getString("email"));
                }
                ArrayAdapter<String> userAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, userEmails);
                assignToDropdown.setAdapter(userAdapter);
            }
        });

        // Fetch Tractors (assuming a "tractors" collection exists)
        db.collection("tractors").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<String> tractorNames = new ArrayList<>();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    tractorNames.add(document.getString("name"));
                }
                ArrayAdapter<String> tractorAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, tractorNames);
                tractorDropdown.setAdapter(tractorAdapter);
            }
        });
    }

    private void saveTaskToFirestore() {
        String title = titleInput.getText().toString().trim();
        String description = descriptionInput.getText().toString().trim();
        String dueDate = dueDateInput.getText().toString().trim();
        String assignedTo = assignToDropdown.getText().toString();
        String repeat = repeatIntervalDropdown.getText().toString();
        String tractor = tractorDropdown.getText().toString();

        if (title.isEmpty()) {
            Toast.makeText(this, "Title is required", Toast.LENGTH_SHORT).show();
            return;
        }

        String taskId = UUID.randomUUID().toString();
        Map<String, Object> task = new HashMap<>();
        task.put("id", taskId);
        task.put("title", title);
        task.put("description", description);
        task.put("dueDate", dueDate);
        task.put("assignedTo", assignedTo);
        task.put("repeatInterval", repeat);
        task.put("tractorId", tractor);

        db.collection("tasks").document(taskId).set(task)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Task Created!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.slide_out_left_fast);
    }
}
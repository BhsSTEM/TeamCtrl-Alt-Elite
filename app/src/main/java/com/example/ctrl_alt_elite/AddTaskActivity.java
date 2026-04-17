package com.example.ctrl_alt_elite;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
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
    private TextInputEditText titleInput, descriptionInput, dayInput, monthInput, yearInput;
    private AutoCompleteTextView assignToDropdown, repeatIntervalDropdown, tractorDropdown;
    private TextView headerText;
    private Button createTaskButton;
    
    private String existingTaskId = null; // Used if we are in Edit mode

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task);

        // Pointing to the specific 'tasks' database
        db = FirebaseFirestore.getInstance("tasks");

        headerText = findViewById(R.id.addTaskHeader);
        titleInput = findViewById(R.id.taskTitleInput);
        descriptionInput = findViewById(R.id.taskDescriptionInput);
        dayInput = findViewById(R.id.taskDayInput);
        monthInput = findViewById(R.id.taskMonthInput);
        yearInput = findViewById(R.id.taskYearInput);
        assignToDropdown = findViewById(R.id.assignToDropdown);
        repeatIntervalDropdown = findViewById(R.id.repeatIntervalDropdown);
        tractorDropdown = findViewById(R.id.associateTractorDropdown);
        createTaskButton = findViewById(R.id.createTaskButton);

        setupDropdowns();
        setupDateAutofill();

        // Check if we are editing an existing task
        if (getIntent().hasExtra("task_id")) {
            loadExistingTaskData();
        }

        // Back button
        ImageButton backButton = findViewById(R.id.backButton);
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }

        // Create/Update Task button
        if (createTaskButton != null) {
            createTaskButton.setOnClickListener(v -> saveTaskToFirestore());
        }
    }

    private void setupDateAutofill() {
        addZeroPadWatcher(dayInput);
        addZeroPadWatcher(monthInput);
        addZeroPadWatcher(yearInput);
    }

    private void addZeroPadWatcher(TextInputEditText editText) {
        editText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String val = editText.getText().toString();
                if (!val.isEmpty() && val.length() == 1) {
                    editText.setText("0" + val);
                }
            }
        });
    }

    private void loadExistingTaskData() {
        existingTaskId = getIntent().getStringExtra("task_id");
        
        headerText.setText("Edit Task");
        createTaskButton.setText("Update Task");

        titleInput.setText(getIntent().getStringExtra("task_title"));
        descriptionInput.setText(getIntent().getStringExtra("task_description"));
        
        // Handle splitting the saved date back into DD, MM, YY
        String fullDate = getIntent().getStringExtra("task_due_date");
        if (fullDate != null && fullDate.contains("/")) {
            String[] parts = fullDate.split("/");
            if (parts.length == 3) {
                dayInput.setText(parts[0]);
                monthInput.setText(parts[1]);
                yearInput.setText(parts[2]);
            }
        }
        
        assignToDropdown.setText(getIntent().getStringExtra("task_assigned_to"), false);
        repeatIntervalDropdown.setText(getIntent().getStringExtra("task_repeat"), false);
        tractorDropdown.setText(getIntent().getStringExtra("task_tractor"), false);
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
        
        // Combine the three date fields into a single string for storage
        String day = dayInput.getText().toString().trim();
        String month = monthInput.getText().toString().trim();
        String year = yearInput.getText().toString().trim();

        // Ensure padding if not already padded (e.g. if user didn't trigger focus change)
        if (day.length() == 1) day = "0" + day;
        if (month.length() == 1) month = "0" + month;
        if (year.length() == 1) year = "0" + year;

        String dueDate = day + "/" + month + "/" + year;

        String assignedTo = assignToDropdown.getText().toString();
        String repeat = repeatIntervalDropdown.getText().toString();
        String tractor = tractorDropdown.getText().toString();

        if (title.isEmpty()) {
            Toast.makeText(this, "Title is required", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (day.isEmpty() || month.isEmpty() || year.isEmpty()) {
            Toast.makeText(this, "Full date is required", Toast.LENGTH_SHORT).show();
            return;
        }

        // Use existing ID if editing, otherwise generate a new one
        String taskId = (existingTaskId != null) ? existingTaskId : UUID.randomUUID().toString();
        
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
                    String message = (existingTaskId != null) ? "Task Updated!" : "Task Created!";
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
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
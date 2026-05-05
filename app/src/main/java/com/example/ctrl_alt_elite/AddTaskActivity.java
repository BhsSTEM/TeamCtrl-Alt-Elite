package com.example.ctrl_alt_elite;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AddTaskActivity extends BaseActivity {

    private FirebaseFirestore db;
    private FirebaseFirestore userdb;
    private FirebaseAuth mAuth;

    private TextInputEditText titleInput, descriptionInput, dayInput, monthInput, yearInput;
    private AutoCompleteTextView assignToDropdown, repeatIntervalDropdown, tractorDropdown;
    private TextView headerText;
    private Button createTaskButton;
    
    private MaterialSwitch enableChecklistSwitch;
    private LinearLayout checklistSection, checklistInputContainer;
    private Button addChecklistItemButton;
    
    private String existingTaskId = null; // Used if we are in Edit mode
    private boolean existingTaskCompleted = false;
    private String userCompanyId = null;

    // For multi-select Assign To
    private String[] allUsers;
    private boolean[] selectedUsers;
    private List<String> finalSelectedUsers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActivityContent(R.layout.activity_add_task);

        // Pointing to the specific 'tasks' database
        db = FirebaseFirestore.getInstance("tasks");
        userdb = FirebaseFirestore.getInstance("sign-up");
        mAuth = FirebaseAuth.getInstance();

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

        // Checklist views
        enableChecklistSwitch = findViewById(R.id.enableChecklistSwitch);
        checklistSection = findViewById(R.id.checklistSection);
        checklistInputContainer = findViewById(R.id.checklistInputContainer);
        addChecklistItemButton = findViewById(R.id.addChecklistItemButton);

        fetchUserCompanyId();
        setupRepeatIntervalDropdown();
        setupDateAutofill();
        setupChecklistLogic();

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

    private void fetchUserCompanyId() {
        if (mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();
            userdb.collection("users").document(uid).get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    userCompanyId = documentSnapshot.getString("companyId");
                    if (userCompanyId == null || userCompanyId.trim().isEmpty()) {
                        userCompanyId = "";
                    }
                } else {
                    userCompanyId = "";
                }
                // Once we have the company ID, fetch users from the same company
                fetchCompanyUsers();
                fetchCompanyTractors();
            });
        }
    }

    private void fetchCompanyUsers() {
        if (userCompanyId == null) return;

        userdb.collection("users")
                .whereEqualTo("companyId", userCompanyId)
                .get()
                .addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<String> userList = new ArrayList<>();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    String name = document.getString("name");
                    if (name != null) userList.add(name);
                }
                allUsers = userList.toArray(new String[0]);
                selectedUsers = new boolean[allUsers.length];

                // Update selectedUsers based on finalSelectedUsers
                if (allUsers != null) {
                    for (int i = 0; i < allUsers.length; i++) {
                        if (finalSelectedUsers.contains(allUsers[i])) {
                            selectedUsers[i] = true;
                        }
                    }
                }

                assignToDropdown.setOnClickListener(v -> showUserSelectionDialog());
            }
        });
    }

    private void fetchCompanyTractors() {
        // Assuming tractors are also company specific. If not, remove the whereEqualTo
        db.collection("tractors")
                .get()
                .addOnCompleteListener(task -> {
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

    private void setupRepeatIntervalDropdown() {
        String[] intervals = {
                getString(R.string.repeat_none),
                getString(R.string.repeat_daily),
                getString(R.string.repeat_weekly),
                getString(R.string.repeat_monthly)
        };
        ArrayAdapter<String> intervalAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, intervals);
        repeatIntervalDropdown.setAdapter(intervalAdapter);
    }

    private void setupChecklistLogic() {
        enableChecklistSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            checklistSection.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (isChecked && checklistInputContainer.getChildCount() == 0) {
                addChecklistItem("");
            }
        });

        addChecklistItemButton.setOnClickListener(v -> addChecklistItem(""));
    }

    private void addChecklistItem(String initialText) {
        View itemView = LayoutInflater.from(this).inflate(R.layout.item_checklist_input, checklistInputContainer, false);
        TextInputEditText itemInput = itemView.findViewById(R.id.checklistItemInput);
        ImageButton removeButton = itemView.findViewById(R.id.removeChecklistItemButton);
        
        if (itemInput != null) {
            itemInput.setText(initialText);
        }

        removeButton.setOnClickListener(v -> {
            checklistInputContainer.removeView(itemView);
            if (checklistInputContainer.getChildCount() == 0) {
                enableChecklistSwitch.setChecked(false);
            }
        });

        checklistInputContainer.addView(itemView);
    }

    private void setupDateAutofill() {
        addZeroPadWatcher(dayInput, 2);
        addZeroPadWatcher(monthInput, 2);
        addZeroPadWatcher(yearInput, 4);
    }

    private void addZeroPadWatcher(TextInputEditText editText, int maxLength) {
        editText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String val = editText.getText().toString();

                if (val.length() > 0) {
                    editText.setText(fillZeros(val, maxLength));
                }
            }
        });
    }

    private String fillZeros(String val, int maxLength) {
        if (val.length() < maxLength) {
            val = "0" + val;
            return fillZeros(val, maxLength);
        }

        return val;
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
        
        String assigned = getIntent().getStringExtra("task_assigned_to");
        assignToDropdown.setText(assigned, false);
        if (assigned != null && !assigned.isEmpty()) {
            finalSelectedUsers = new ArrayList<>(Arrays.asList(assigned.split(", ")));
        }

        repeatIntervalDropdown.setText(getIntent().getStringExtra("task_repeat"), false);
        tractorDropdown.setText(getIntent().getStringExtra("task_tractor"), false);

        // Load checklist and completion status from Firestore
        if (existingTaskId != null) {
            db.collection("tasks").document(existingTaskId).get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    existingTaskCompleted = documentSnapshot.getBoolean("completed") != null && documentSnapshot.getBoolean("completed");
                    String cid = documentSnapshot.getString("companyId");
                    if (cid == null || cid.trim().isEmpty()) {
                        userCompanyId = "";
                    } else {
                        userCompanyId = cid;
                    }
                    
                    List<Map<String, Object>> checklist = (List<Map<String, Object>>) documentSnapshot.get("checklist");
                    if (checklist != null && !checklist.isEmpty()) {
                        enableChecklistSwitch.setChecked(true);
                        checklistInputContainer.removeAllViews();
                        for (Map<String, Object> item : checklist) {
                            String text = (String) item.get("text");
                            addChecklistItem(text != null ? text : "");
                        }
                    }
                }
            });
        }
    }

    private void showUserSelectionDialog() {
        if (allUsers == null || allUsers.length == 0) {
            Toast.makeText(this, "No users found in your company", Toast.LENGTH_SHORT).show();
            return;
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Select People");
        builder.setMultiChoiceItems(allUsers, selectedUsers, (dialog, which, isChecked) -> {
            selectedUsers[which] = isChecked;
        });

        builder.setPositiveButton("OK", (dialog, which) -> {
            finalSelectedUsers.clear();
            StringBuilder selectedText = new StringBuilder();
            for (int i = 0; i < selectedUsers.length; i++) {
                if (selectedUsers[i]) {
                    finalSelectedUsers.add(allUsers[i]);
                    if (selectedText.length() > 0) {
                        selectedText.append(", ");
                    }
                    selectedText.append(allUsers[i]);
                }
            }
            assignToDropdown.setText(selectedText.toString(), false);
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void saveTaskToFirestore() {
        String title = titleInput.getText().toString().trim();
        String description = descriptionInput.getText().toString().trim();
        
        String day = dayInput.getText().toString().trim();
        String month = monthInput.getText().toString().trim();
        String year = yearInput.getText().toString().trim();

        String dueDate = "";
        if (!day.isEmpty() && !month.isEmpty() && !year.isEmpty()) {
            if (day.length() == 1) day = "0" + day;
            if (month.length() == 1) month = "0" + month;
            if (year.length() == 1) year = "0" + year;
            dueDate = day + "/" + month + "/" + year;
        }

        // assignedTo is now a comma-separated string from the dropdown text
        String assignedTo = assignToDropdown.getText().toString();
        String repeat = repeatIntervalDropdown.getText().toString();
        String tractor = tractorDropdown.getText().toString();

        if (title.isEmpty()) {
            Toast.makeText(this, "Title is required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (userCompanyId == null) {
            Toast.makeText(this, "Waiting for company data...", Toast.LENGTH_SHORT).show();
            return;
        }

        String taskId = (existingTaskId != null) ? existingTaskId : UUID.randomUUID().toString();
        
        // Collect checklist items
        List<Map<String, Object>> checklistItems = new ArrayList<>();
        if (enableChecklistSwitch.isChecked()) {
            for (int i = 0; i < checklistInputContainer.getChildCount(); i++) {
                View child = checklistInputContainer.getChildAt(i);
                TextInputEditText itemInput = child.findViewById(R.id.checklistItemInput);

                if (itemInput != null) {
                    String text = itemInput.getText().toString().trim();
                    if (!text.isEmpty()) {
                        Map<String, Object> item = new HashMap<>();
                        item.put("text", text);
                        item.put("completed", false);
                        checklistItems.add(item);
                    }
                }
            }
        }

        Map<String, Object> task = new HashMap<>();
        task.put("id", taskId);
        task.put("companyId", userCompanyId);
        task.put("title", title);
        task.put("description", description);
        task.put("dueDate", dueDate);
        task.put("assignedTo", assignedTo);
        task.put("repeatInterval", repeat);
        task.put("tractorId", tractor);
        task.put("checklist", checklistItems);
        task.put("completed", existingTaskCompleted); // Preserve completion status on edit

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

package com.example.ctrl_alt_elite;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class TasksActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private TaskAdapter adapter;
    private List<Task> taskList;
    private FirebaseFirestore db;
    private FirebaseFirestore userdb;
    private FirebaseAuth mAuth;
    private ProgressBar progressBar;
    private TextView noTasksTextView;
    private String userCompanyId = null;
    private String userRole = null;
    private String userName = null;

    private ListenerRegistration userListener;
    private ListenerRegistration tasksListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActivityContent(R.layout.activity_tasks);

        // Pointing to the specific 'tasks' database
        db = FirebaseFirestore.getInstance("tasks");
        userdb = FirebaseFirestore.getInstance("sign-up");
        mAuth = FirebaseAuth.getInstance();
        
        taskList = new ArrayList<>();

        recyclerView = findViewById(R.id.tasksRecyclerView);
        progressBar = findViewById(R.id.tasksProgressBar);
        noTasksTextView = findViewById(R.id.noTasksTextView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new TaskAdapter(taskList, this::showTaskMenu);
        recyclerView.setAdapter(adapter);

        startListeningToUserChanges();

        FloatingActionButton addTaskFab = findViewById(R.id.addTaskFab);
        if (addTaskFab != null) {
            addTaskFab.setOnClickListener(v -> {
                Intent intent = new Intent(TasksActivity.this, AddTaskActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right_fast, R.anim.slide_out_left_fast);
            });
        }
    }

    private void startListeningToUserChanges() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        // Real-time listener for user document to detect role/company changes
        userListener = userdb.collection("users").document(uid).addSnapshotListener((documentSnapshot, error) -> {
            if (error != null) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Error listening to user data.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (documentSnapshot != null && documentSnapshot.exists()) {
                userCompanyId = documentSnapshot.getString("companyId");
                userRole = documentSnapshot.getString("role");
                userName = documentSnapshot.getString("name");

                // If companyId is null or empty, use the shared default company ID
                if (userCompanyId == null || userCompanyId.trim().isEmpty()) {
                    userCompanyId = "";
                }

                // UI adjustments based on role - updates instantly if the role changes in the DB
                FloatingActionButton addTaskFab = findViewById(R.id.addTaskFab);
                if ("Operator".equalsIgnoreCase(userRole)) {
                    if (addTaskFab != null) addTaskFab.setVisibility(View.GONE);
                } else {
                    if (addTaskFab != null) addTaskFab.setVisibility(View.VISIBLE);
                }
                
                // Refresh task listener for the current company and role
                startListeningToTasks(userCompanyId);
            } else {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void startListeningToTasks(String companyId) {
        // Remove existing listener to avoid leaks or multiple triggers
        if (tasksListener != null) {
            tasksListener.remove();
        }

        tasksListener = db.collection("tasks")
            .whereEqualTo("companyId", companyId)
            .addSnapshotListener((value, error) -> {
                if (progressBar != null) progressBar.setVisibility(View.GONE);

                if (error != null) {
                    Toast.makeText(this, "Error fetching tasks", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (value != null) {
                    taskList.clear();
                    for (QueryDocumentSnapshot doc : value) {
                        Task task = doc.toObject(Task.class);
                        
                        if ("Operator".equalsIgnoreCase(userRole)) {
                            // Filter tasks: Operators only see tasks assigned to them
                            String assigned = task.getAssignedTo();
                            if (assigned != null && userName != null) {
                                // assignedTo can be a comma-separated list of names
                                String[] names = assigned.split(", ");
                                boolean isAssignedToMe = false;
                                for (String name : names) {
                                    if (name.trim().equalsIgnoreCase(userName.trim())) {
                                        isAssignedToMe = true;
                                        break;
                                    }
                                }
                                if (isAssignedToMe) {
                                    taskList.add(task);
                                }
                            }
                        } else {
                            // "Owner" or other roles see all tasks for the company
                            taskList.add(task);
                        }
                    }

                    // Update empty state visibility
                    if (taskList.isEmpty()) {
                        if (noTasksTextView != null) noTasksTextView.setVisibility(View.VISIBLE);
                        if (recyclerView != null) recyclerView.setVisibility(View.GONE);
                    } else {
                        if (noTasksTextView != null) noTasksTextView.setVisibility(View.GONE);
                        if (recyclerView != null) recyclerView.setVisibility(View.VISIBLE);
                    }

                    adapter.notifyDataSetChanged();
                }
            });
    }

    private void showTaskMenu(View view, Task task) {
        // Operators shouldn't be able to edit or delete tasks
        if ("Operator".equalsIgnoreCase(userRole)) {
            return;
        }

        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenu().add("Edit");
        popup.getMenu().add("Delete");

        popup.setOnMenuItemClickListener(item -> {
            if (item.getTitle().equals("Edit")) {
                editTask(task);
            } else if (item.getTitle().equals("Delete")) {
                deleteTask(task);
            }
            return true;
        });
        popup.show();
    }

    private void editTask(Task task) {
        Intent intent = new Intent(TasksActivity.this, AddTaskActivity.class);
        intent.putExtra("task_id", task.getId());
        intent.putExtra("task_title", task.getTitle());
        intent.putExtra("task_description", task.getDescription());
        intent.putExtra("task_due_date", task.getDueDate());
        intent.putExtra("task_assigned_to", task.getAssignedTo());
        intent.putExtra("task_repeat", task.getRepeatInterval());
        intent.putExtra("task_tractor", task.getTractorId());
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right_fast, R.anim.slide_out_left_fast);
    }

    private void deleteTask(Task task) {
        db.collection("tasks").document(task.getId()).delete()
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Task Deleted", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Error deleting task", Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up listeners when activity is destroyed to prevent memory leaks
        if (userListener != null) userListener.remove();
        if (tasksListener != null) tasksListener.remove();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}

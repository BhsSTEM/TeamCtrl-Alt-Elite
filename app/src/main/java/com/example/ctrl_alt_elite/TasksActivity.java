package com.example.ctrl_alt_elite;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class TasksActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private TaskAdapter adapter;
    private List<Task> taskList;
    private FirebaseFirestore db;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActivityContent(R.layout.activity_tasks);

        // Pointing to the specific 'tasks' database
        db = FirebaseFirestore.getInstance("tasks");
        taskList = new ArrayList<>();

        recyclerView = findViewById(R.id.tasksRecyclerView);
        progressBar = findViewById(R.id.tasksProgressBar);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new TaskAdapter(taskList, this::showTaskMenu);
        recyclerView.setAdapter(adapter);

        fetchTasks();

        FloatingActionButton addTaskFab = findViewById(R.id.addTaskFab);
        if (addTaskFab != null) {
            addTaskFab.setOnClickListener(v -> {
                Intent intent = new Intent(TasksActivity.this, AddTaskActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right_fast, R.anim.slide_out_left_fast);
            });
        }
    }

    private void fetchTasks() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        
        db.collection("tasks").addSnapshotListener((value, error) -> {
            if (progressBar != null) progressBar.setVisibility(View.GONE);

            if (error != null) {
                Toast.makeText(this, "Error fetching tasks", Toast.LENGTH_SHORT).show();
                return;
            }

            if (value != null) {
                taskList.clear();
                for (QueryDocumentSnapshot doc : value) {
                    Task task = doc.toObject(Task.class);
                    taskList.add(task);
                }
                adapter.notifyDataSetChanged();
            }
        });
    }

    private void showTaskMenu(View view, Task task) {
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
    protected void onResume() {
        super.onResume();
    }
}
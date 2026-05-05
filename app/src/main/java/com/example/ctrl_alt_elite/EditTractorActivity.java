package com.example.ctrl_alt_elite;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class EditTractorActivity extends BaseActivity {

    private TextView txtTractorName, txtTractorStatus, txtFuelValue, txtEngineHours, txtLastService;
    private TextView txtYear, txtModel, txtPIN;
    private ImageView imgTractorLarge;
    private ProgressBar progressFuel;
    private TaskAdapter taskAdapter;
    private List<Task> taskList;
    private Tractor currentTractor;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActivityContent(R.layout.activity_edit_tractor);

        db = FirebaseFirestore.getInstance();
        currentTractor = (Tractor) getIntent().getSerializableExtra("TRACTOR_DATA");

        initViews();
        setupListeners();

        if (currentTractor != null) {
            populateTractorData();
            fetchMaintenanceTasks();
        } else {
            Toast.makeText(this, getString(R.string.error_machine_data), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initViews() {
        txtTractorName = findViewById(R.id.txtTractorName);
        txtTractorStatus = findViewById(R.id.txtTractorStatus);
        txtFuelValue = findViewById(R.id.txtFuelValue);
        txtEngineHours = findViewById(R.id.txtEngineHours);
        txtLastService = findViewById(R.id.txtLastService);
        txtYear = findViewById(R.id.txtYear);
        txtModel = findViewById(R.id.txtModel);
        txtPIN = findViewById(R.id.txtPIN);
        imgTractorLarge = findViewById(R.id.imgTractorLarge);
        progressFuel = findViewById(R.id.progressFuel);
        RecyclerView rvTasks = findViewById(R.id.rvTasks);

        taskList = new ArrayList<>();
        taskAdapter = new TaskAdapter(taskList, (view, task) -> {
            // Task menu logic
        });
        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        rvTasks.setAdapter(taskAdapter);
    }

    private void setupListeners() {
        // Operator Guide Link
        findViewById(R.id.btnGuide).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.deere.com/en/parts-and-service/manuals-and-training/quick-reference-guides/"));
            startActivity(intent);
        });

        // Main Edit Button in the card - Takes you to the detailed Editor form
        findViewById(R.id.btnEditMachine).setOnClickListener(v -> navigateToEditForm());

        // View All Tasks
        findViewById(R.id.btnViewAllTasks).setOnClickListener(v -> {
            Intent intent = new Intent(this, TasksActivity.class);
            startActivity(intent);
        });

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        }
    }

    /**
     * Navigates to AddTractorActivity in Edit Mode.
     */
    private void navigateToEditForm() {
        if (currentTractor != null) {
            Intent intent = new Intent(this, AddTractorActivity.class);
            intent.putExtra("TRACTOR_DATA", currentTractor);
            startActivity(intent);
        }
    }

    private void populateTractorData() {
        txtTractorName.setText(currentTractor.getName());
        txtTractorStatus.setText(getString(R.string.status_format,
                currentTractor.getStatus() != null ? currentTractor.getStatus() : "Operational"));

        int fuel = currentTractor.getFuel();
        txtFuelValue.setText(getString(R.string.fuel_percent_format, fuel));
        progressFuel.setProgress(fuel);

        txtYear.setText(String.valueOf(currentTractor.getYear()));
        txtModel.setText(currentTractor.getModel());
        txtPIN.setText(currentTractor.getPin() != null ? currentTractor.getPin() : "N/A");

        txtLastService.setText(currentTractor.getLastUpdated() != null ? currentTractor.getLastUpdated() : getString(R.string.no_recent_service));

        // Show real engine hours from Firebase
        txtEngineHours.setText(getString(R.string.hours_format, String.valueOf(currentTractor.getEngineHours())));

        if (currentTractor.getImageUrl() != null && !currentTractor.getImageUrl().isEmpty() && !currentTractor.getImageUrl().equals("link")) {
            Glide.with(this)
                    .load(currentTractor.getImageUrl())
                    .placeholder(R.drawable.pngimg_com___tractor_png101303_removebg_preview)
                    .into(imgTractorLarge);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentTractor != null && currentTractor.getDocumentId() != null) {
            refreshTractorData();
        }
    }

    private void refreshTractorData() {
        db.collection("tractors").document(currentTractor.getDocumentId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Tractor updatedTractor = documentSnapshot.toObject(Tractor.class);
                        if (updatedTractor != null) {
                            updatedTractor.setDocumentId(documentSnapshot.getId());
                            currentTractor = updatedTractor;
                            populateTractorData();
                        }
                    }
                });
    }

    private void fetchMaintenanceTasks() {
        if (currentTractor == null || currentTractor.getDocumentId() == null) return;

        db.collection("tasks")
                .whereEqualTo("tractorId", currentTractor.getDocumentId())
                .orderBy("dueDate", Query.Direction.ASCENDING)
                .limit(5)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        taskList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Task maintenanceTask = document.toObject(Task.class);
                            maintenanceTask.setId(document.getId());
                            taskList.add(maintenanceTask);
                        }
                        taskAdapter.notifyDataSetChanged();
                    }
                });
    }
}

package com.example.ctrl_alt_elite;

import android.content.Intent;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Firebase;

import java.util.ArrayList;
import java.util.List;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.QuerySnapshot;
import android.util.Log;
import androidx.annotation.Nullable;

public class ManageTractorsActivity extends BaseActivity {

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private List<Tractor> tractorList = new ArrayList<>();
    private TractorAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActivityContent(R.layout.add_tracter);

        MaterialButton btnAddTractor = findViewById(R.id.btn_add_tractor);
        if (btnAddTractor != null) {
            btnAddTractor.setOnClickListener(v -> {
                Intent intent = new Intent(ManageTractorsActivity.this, AddTractorActivity.class);
                startActivity(intent);
            });
        }

        // Setup RecyclerView
        RecyclerView recyclerView = findViewById(R.id.tracters);
        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            adapter = new TractorAdapter(tractorList);
            recyclerView.setAdapter(adapter);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupNavigation();
    }
}

package com.example.ctrl_alt_elite;

import android.content.Intent;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.QuerySnapshot;
import android.util.Log;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

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

        RecyclerView recyclerView = findViewById(R.id.tracters);
        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            adapter = new TractorAdapter(tractorList, false); // explicit false for isMapContext
            recyclerView.setAdapter(adapter);
        }
        listenToFirestore();

    }

    private void listenToFirestore() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Log.e("FirestoreError", "User not logged in");
            return;
        }

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("tractors")
                .whereEqualTo("user", currentUserId)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            Log.e("FirestoreError", "Listen failed.", error);
                            return;
                        }

                        if (value != null) {
                            tractorList.clear();
                            for (QueryDocumentSnapshot doc : value) {
                                Tractor tractor = doc.toObject(Tractor.class);
                                tractor.setDocumentId(doc.getId()); // Store ID for editing
                                tractorList.add(tractor);
                            }
                            adapter.notifyDataSetChanged();
                            Log.d("FirestoreData", "Tractors found: " + tractorList.size());
                        }
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupNavigation();
    }
}
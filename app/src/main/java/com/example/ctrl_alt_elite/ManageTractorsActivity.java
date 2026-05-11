package com.example.ctrl_alt_elite;

import android.content.Intent;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import android.util.Log;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;


public class ManageTractorsActivity extends BaseActivity {

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseFirestore userDb = FirebaseFirestore.getInstance("sign-up");
    private List<Tractor> tractorList = new ArrayList<>();
    private TractorAdapter adapter;

    private ListenerRegistration tractorListener;

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
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e("FirestoreError", "User not logged in");
            return;
        }

        String currentUserId = currentUser.getUid();

        // First, get the user's role from the 'users' collection in the 'sign-up' database
        userDb.collection("users").document(currentUserId).get().addOnSuccessListener(documentSnapshot -> {
            String role = documentSnapshot.getString("role");

            com.google.firebase.firestore.Query query;

            // ROLE LOGIC:
            // If owner, get all tractors. If operator, only get theirs.
            if ("owner".equalsIgnoreCase(role)) {
                query = db.collection("tractors");
                Log.d("FirestoreData", "User is Owner: Fetching all tractors");
            } else {
                query = db.collection("tractors").whereEqualTo("user", currentUserId);
                Log.d("FirestoreData", "User is Operator: Fetching personal tractors");
            }


            // Apply the listener to the chosen query
            tractorListener = query.addSnapshotListener(new EventListener<QuerySnapshot>() {
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
                            tractor.setDocumentId(doc.getId());
                            tractorList.add(tractor);
                        }
                        adapter.notifyDataSetChanged();
                    }
                }
            });
        }).addOnFailureListener(e -> {
            Log.e("FirestoreError", "Failed to fetch user role", e);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupNavigation();

        // Stop any existing listener and restart it to catch role changes
        if (tractorListener != null) {
            tractorListener.remove();
        }
        listenToFirestore();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Clean up the listener when leaving the screen
        if (tractorListener != null) {
            tractorListener.remove();
        }
    }
}

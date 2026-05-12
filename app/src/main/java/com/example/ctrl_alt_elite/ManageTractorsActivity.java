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
import android.view.View;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;


public class ManageTractorsActivity extends BaseActivity {

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseFirestore userDb = FirebaseFirestore.getInstance("sign-up");
    private List<Tractor> tractorList = new ArrayList<>();
    private TractorAdapter adapter;

    private ListenerRegistration tractorListener;
    private MaterialButton btnAddTractor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActivityContent(R.layout.add_tracter);

        btnAddTractor = findViewById(R.id.btn_add_tractor);
        if (btnAddTractor != null) {
            btnAddTractor.setOnClickListener(v -> {
                Intent intent = new Intent(ManageTractorsActivity.this, AddTractorActivity.class);
                startActivity(intent);
            });
            // Initially hide until role is confirmed
            btnAddTractor.setVisibility(View.GONE);
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

        // Get the user's role and companyId from the 'users' collection in the 'sign-up' database
        userDb.collection("users").document(currentUserId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String role = documentSnapshot.getString("role");
                String companyId = documentSnapshot.getString("companyId");

                if (companyId == null || companyId.isEmpty()) {
                    Log.e("FirestoreError", "User has no companyId");
                    return;
                }

                boolean isOwner = "owner".equalsIgnoreCase(role);
                if (btnAddTractor != null) {
                    btnAddTractor.setVisibility(isOwner ? View.VISIBLE : View.GONE);
                }

                if (adapter != null) {
                    adapter.setUserRole(role);
                }

                com.google.firebase.firestore.Query query;

                // ROLE LOGIC:
                // If owner, get all tractors in company. If operator (op), only get theirs in the company.
                if (isOwner) {
                    query = db.collection("tractors").whereEqualTo("CompanyId", companyId);
                    Log.d("FirestoreData", "User is Owner: Fetching all tractors for company: " + companyId);
                } else {
                    query = db.collection("tractors")
                            .whereEqualTo("CompanyId", companyId);
                    Log.d("FirestoreData", "User is Operator: Fetching personal tractors for company: " + companyId);
                }

                // Remove existing listener if any
                if (tractorListener != null) {
                    tractorListener.remove();
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
            } else {
                Log.e("FirestoreError", "User document does not exist");
            }
        }).addOnFailureListener(e -> {
            Log.e("FirestoreError", "Failed to fetch user data", e);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupNavigation();
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

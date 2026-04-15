package com.example.ctrl_alt_elite;

import android.content.Intent;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;

public class ManageTractorsActivity extends BaseActivity {

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
            
            // Dummy data for now so you can see it working
            List<Tractor> dummyTractors = new ArrayList<>();
            dummyTractors.add(new Tractor("John Deere 5050D", "2022", "5050D"));
            dummyTractors.add(new Tractor("Mahindra Arjun", "2021", "555 DI"));
            dummyTractors.add(new Tractor("Kubota MU4501", "2023", "MU4501"));
            
            TractorAdapter adapter = new TractorAdapter(dummyTractors);
            recyclerView.setAdapter(adapter);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupNavigation();
    }
}

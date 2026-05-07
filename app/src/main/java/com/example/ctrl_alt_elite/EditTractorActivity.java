package com.example.ctrl_alt_elite;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;

public class EditTractorActivity extends BaseActivity {

    private Tractor tractor;
    private TextView txtTractorTitle, txtYear, txtModelNumber, txtPin, txtFuel, txtStatus, txtSoftwareStatus, txtFirmwareStatus;
    private ImageView imgTractorLarge, btnBack;
    private View btnChangeInfo, cardGuide;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActivityContent(R.layout.activity_edit_tractor);

        db = FirebaseFirestore.getInstance();

        // Initialize Views
        txtTractorTitle = findViewById(R.id.txtTractorTitle);
        txtYear = findViewById(R.id.txtYear);
        txtModelNumber = findViewById(R.id.txtModelNumber);
        txtPin = findViewById(R.id.txtPin);
        txtFuel = findViewById(R.id.txtFuel);
        txtStatus = findViewById(R.id.txtStatus);
        txtSoftwareStatus = findViewById(R.id.txtSoftwareStatus);
        txtFirmwareStatus = findViewById(R.id.txtFirmwareStatus);
        imgTractorLarge = findViewById(R.id.imgTractorLarge);
        btnBack = findViewById(R.id.btnBack);
        btnChangeInfo = findViewById(R.id.btnChangeInfo);
        cardGuide = findViewById(R.id.cardGuide);

        // Get Tractor Data
        if (getIntent().hasExtra("TRACTOR_DATA")) {
            tractor = (Tractor) getIntent().getSerializableExtra("TRACTOR_DATA");
            displayTractorDetails();
            checkUpdateAlerts();
        }

        // Click Listeners
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnChangeInfo != null) {
            btnChangeInfo.setOnClickListener(v -> {
                Intent intent = new Intent(EditTractorActivity.this, AddTractorActivity.class);
                intent.putExtra("TRACTOR_DATA", tractor);
                startActivity(intent);
            });
        }

        if (cardGuide != null) {
            cardGuide.setOnClickListener(v -> {
                String url = (tractor != null && tractor.getGuideUrl() != null) 
                        ? tractor.getGuideUrl() 
                        : "https://www.deere.com/en/parts-and-service/manuals-and-training/quick-reference-guides/";
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            });
        }
    }

    private void checkUpdateAlerts() {
        if (tractor == null) return;
        boolean needsSoftware = tractor.getSoftwareStatus() != null && !tractor.getSoftwareStatus().equalsIgnoreCase("Up to date");
        boolean needsFirmware = tractor.getFirmwareStatus() != null && !tractor.getFirmwareStatus().equalsIgnoreCase("Up to date");

        if (needsSoftware || needsFirmware) {
            String msg = "Notice: " + (needsSoftware ? "Software " : "") + 
                         (needsSoftware && needsFirmware ? "& " : "") + 
                         (needsFirmware ? "Firmware " : "") + "update required!";
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        }
    }

    private void refreshTractorData() {
        if (tractor == null || tractor.getDocumentId() == null) return;

        db.collection("tractors").document(tractor.getDocumentId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        tractor = documentSnapshot.toObject(Tractor.class);
                        if (tractor != null) {
                            tractor.setDocumentId(documentSnapshot.getId());
                            displayTractorDetails();
                        }
                    }
                });
    }

    private void displayTractorDetails() {
        if (tractor == null) return;

        if (txtTractorTitle != null) txtTractorTitle.setText(tractor.getName());
        if (txtYear != null) txtYear.setText(String.valueOf(tractor.getYear()));
        if (txtModelNumber != null) txtModelNumber.setText(tractor.getModel());
        if (txtPin != null) txtPin.setText(tractor.getPin());
        if (txtFuel != null) txtFuel.setText(tractor.getFuel() + "%");
        if (txtStatus != null) txtStatus.setText(tractor.getStatus());
        
        if (txtSoftwareStatus != null) {
            txtSoftwareStatus.setText(tractor.getSoftwareStatus() != null ? tractor.getSoftwareStatus() : "Up to date");
        }
        if (txtFirmwareStatus != null) {
            txtFirmwareStatus.setText(tractor.getFirmwareStatus() != null ? tractor.getFirmwareStatus() : "Up to date");
        }

        if (imgTractorLarge != null && tractor.getImageUrl() != null && !tractor.getImageUrl().isEmpty() && !tractor.getImageUrl().equals("link")) {
            Glide.with(this).load(tractor.getImageUrl()).into(imgTractorLarge);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupNavigation();
        refreshTractorData(); // Auto-update info every time page is opened
    }
}
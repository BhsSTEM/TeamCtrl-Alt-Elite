package com.example.ctrl_alt_elite;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.bumptech.glide.Glide;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.firestore.FirebaseFirestore;

public class EditTractorActivity extends BaseActivity {

    private Tractor tractor;
    private TextView txtTractorName, txtTractorStatus, txtFuelValue, txtEngineHours, txtSoftware, txtFirmware, txtYear, txtModel, txtLastUpdated, txtPIN;
    private ImageView imgTractorLarge, imgStatusWarning, imgSoftwareWarning, imgFirmwareWarning;
    private LinearProgressIndicator progressFuel;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActivityContent(R.layout.activity_edit_tractor);

        db = FirebaseFirestore.getInstance();

        // Initialize Views
        txtTractorName = findViewById(R.id.txtTractorName);
        txtTractorStatus = findViewById(R.id.txtTractorStatus);
        txtFuelValue = findViewById(R.id.txtFuelValue);
        txtEngineHours = findViewById(R.id.txtEngineHours);
        txtSoftware = findViewById(R.id.txtSoftware);
        txtFirmware = findViewById(R.id.txtFirmware);
        txtYear = findViewById(R.id.txtYear);
        txtModel = findViewById(R.id.txtModel);
        txtLastUpdated = findViewById(R.id.txtLastUpdated);
        txtPIN = findViewById(R.id.txtPIN);
        imgTractorLarge = findViewById(R.id.imgTractorLarge);
        progressFuel = findViewById(R.id.progressFuel);
        
        imgStatusWarning = findViewById(R.id.imgStatusWarning);
        imgSoftwareWarning = findViewById(R.id.imgSoftwareWarning);
        imgFirmwareWarning = findViewById(R.id.imgFirmwareWarning);

        // Get Tractor Data
        if (getIntent().hasExtra("TRACTOR_DATA")) {
            tractor = (Tractor) getIntent().getSerializableExtra("TRACTOR_DATA");
            displayTractorDetails();
            checkUpdateAlerts();
        }

        // Click Listeners
        findViewById(R.id.btnEditMachine).setOnClickListener(v -> {
            Intent intent = new Intent(EditTractorActivity.this, AddTractorActivity.class);
            intent.putExtra("TRACTOR_DATA", tractor);
            startActivity(intent);
        });

        findViewById(R.id.btnGuide).setOnClickListener(v -> {
            String url = (tractor != null && tractor.getGuideUrl() != null) 
                    ? tractor.getGuideUrl() 
                    : "https://www.deere.com/en/parts-and-service/manuals-and-training/quick-reference-guides/";
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        });
        
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void checkUpdateAlerts() {
        if (tractor == null) return;
        boolean needsSoftware = tractor.isSoftwareWarning();
        boolean needsFirmware = tractor.isFirmwareWarning();

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

        if (txtTractorName != null) txtTractorName.setText(tractor.getName());
        
        // Status Warning Logic
        if (txtTractorStatus != null) {
            txtTractorStatus.setText(tractor.getStatus());
            if (tractor.isMaintenanceWarning()) {
                txtTractorStatus.setTextColor(Color.parseColor("#FFD600")); // Yellow
                if (imgStatusWarning != null) imgStatusWarning.setVisibility(View.VISIBLE);
            } else {
                txtTractorStatus.setTextColor(Color.parseColor("#4CAF50")); // Default Green
                if (imgStatusWarning != null) imgStatusWarning.setVisibility(View.GONE);
            }
        }

        if (txtFuelValue != null) txtFuelValue.setText(tractor.getFuel() + "%");
        if (progressFuel != null) progressFuel.setProgress(tractor.getFuel());
        if (txtEngineHours != null) txtEngineHours.setText(String.format("%.1f hrs", tractor.getEngineHours()));
        
        // Software Warning Logic
        if (txtSoftware != null) {
            txtSoftware.setText(tractor.getSoftwareStatus() != null ? tractor.getSoftwareStatus() : "Up to date");
            if (tractor.isSoftwareWarning()) {
                txtSoftware.setTextColor(Color.parseColor("#FFD600"));
                if (imgSoftwareWarning != null) imgSoftwareWarning.setVisibility(View.VISIBLE);
            } else {
                txtSoftware.setTextColor(Color.WHITE);
                if (imgSoftwareWarning != null) imgSoftwareWarning.setVisibility(View.GONE);
            }
        }

        // Firmware Warning Logic
        if (txtFirmware != null) {
            txtFirmware.setText(tractor.getFirmwareStatus() != null ? tractor.getFirmwareStatus() : "Up to date");
            if (tractor.isFirmwareWarning()) {
                txtFirmware.setTextColor(Color.parseColor("#FFD600"));
                if (imgFirmwareWarning != null) imgFirmwareWarning.setVisibility(View.VISIBLE);
            } else {
                txtFirmware.setTextColor(Color.WHITE);
                if (imgFirmwareWarning != null) imgFirmwareWarning.setVisibility(View.GONE);
            }
        }

        if (txtYear != null) txtYear.setText(String.valueOf(tractor.getYear()));
        if (txtModel != null) txtModel.setText(tractor.getModel());
        if (txtLastUpdated != null) txtLastUpdated.setText(tractor.getLastUpdated());
        if (txtPIN != null) txtPIN.setText(tractor.getPin());

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
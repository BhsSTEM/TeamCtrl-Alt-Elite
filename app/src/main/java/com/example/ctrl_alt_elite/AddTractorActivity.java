package com.example.ctrl_alt_elite;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class AddTractorActivity extends BaseActivity {

    private ImageView ivTractorImage;
    private Spinner spinnerYear;
    
    private EditText getTractorName;
    private EditText getModelNumber;
    private EditText getPin;
    private TextView titleAddTractor;

    private FirebaseFirestore db;
    private Tractor existingTractor;

    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    ivTractorImage.setImageURI(imageUri);
                }
            });

    private final ActivityResultLauncher<Intent> takePhotoLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getExtras() != null) {
                    Bitmap photo = (Bitmap) result.getData().getExtras().get("data");
                    ivTractorImage.setImageBitmap(photo);
                }
            });

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    showImagePickerOptions();
                } else {
                    Toast.makeText(this, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show();
                }
            });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActivityContent(R.layout.add_add_tractor);

        db = FirebaseFirestore.getInstance();

        ivTractorImage = findViewById(R.id.ivTractorImage);
        spinnerYear = findViewById(R.id.spinnerYear);
        getTractorName = findViewById(R.id.getTractorName);
        getModelNumber = findViewById(R.id.getModelNumber);
        getPin = findViewById(R.id.getPin);
        titleAddTractor = findViewById(R.id.titleAddTractor);

        View btnUploadImage = findViewById(R.id.btnUploadImage);
        TextView btnBack = findViewById(R.id.btnBack);
        Button btnSaveTractor = findViewById(R.id.btnSaveTractor);

        setupYearSpinner();

        // Check if editing existing tractor
        if (getIntent().hasExtra("TRACTOR_DATA")) {
            existingTractor = (Tractor) getIntent().getSerializableExtra("TRACTOR_DATA");
            populateFields(existingTractor);
        }

        if (btnUploadImage != null) {
            btnUploadImage.setOnClickListener(v -> checkPermissionsAndShowOptions());
        }
        
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnSaveTractor != null) {
            btnSaveTractor.setOnClickListener(v -> saveTractorToFirebase());
        }
    }

    private void populateFields(Tractor tractor) {
        if (titleAddTractor != null) titleAddTractor.setText("Edit Tractor");
        getTractorName.setText(tractor.getName());
        getModelNumber.setText(tractor.getModel());
        getPin.setText(tractor.getPin());
        
        // Set year in spinner
        ArrayAdapter adapter = (ArrayAdapter) spinnerYear.getAdapter();
        int position = adapter.getPosition(String.valueOf(tractor.getYear()));
        if (position >= 0) spinnerYear.setSelection(position);

        if (tractor.getImageUrl() != null && !tractor.getImageUrl().isEmpty() && !tractor.getImageUrl().equals("link")) {
            Glide.with(this).load(tractor.getImageUrl()).into(ivTractorImage);
        }
    }

    private void setupYearSpinner() {
        List<String> years = new ArrayList<>();
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        for (int i = currentYear; i >= 1900; i--) {
            years.add(String.valueOf(i));
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, years);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerYear.setAdapter(adapter);
    }

    private void checkPermissionsAndShowOptions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            showImagePickerOptions();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void showImagePickerOptions() {
        String[] options = {"Take Photo", "Choose from Gallery"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Option");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                takePhotoLauncher.launch(takePictureIntent);
            } else {
                Intent pickPhotoIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                pickImageLauncher.launch(pickPhotoIntent);
            }
        });
        builder.show();
    }

    private void saveTractorToFirebase() {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        String uid = mAuth.getUid();
        String name = getTractorName.getText().toString().trim();
        String model = getModelNumber.getText().toString().trim();
        String pin = getPin.getText().toString().trim();
        String yearStr = spinnerYear.getSelectedItem().toString();

        if (name.isEmpty() || model.isEmpty() || pin.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int year = Integer.parseInt(yearStr);

        Tractor tractor = (existingTractor != null) ? existingTractor : new Tractor();
        tractor.setUser(uid);
        tractor.setName(name);
        tractor.setModel(model);
        tractor.setPin(pin);
        tractor.setYear(year);
        if (existingTractor == null) {
            tractor.setStatus("Active");
            tractor.setFuel(100);
        }
        tractor.setLastUpdated(java.text.DateFormat.getDateTimeInstance().format(new java.util.Date()));

        if (existingTractor != null && existingTractor.getDocumentId() != null) {
            // Update existing
            db.collection("tractors").document(existingTractor.getDocumentId())
                    .set(tractor)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(AddTractorActivity.this, "Tractor updated successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show());
        } else {
            // Add new
            db.collection("tractors")
                    .add(tractor)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(AddTractorActivity.this, "Tractor added successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(AddTractorActivity.this, "Error adding tractor: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupNavigation();
    }
}
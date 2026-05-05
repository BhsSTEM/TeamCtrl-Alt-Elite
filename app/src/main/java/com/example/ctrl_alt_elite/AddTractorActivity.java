package com.example.ctrl_alt_elite;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class AddTractorActivity extends BaseActivity {

    private ImageView ivTractorImage;
    private Spinner spinnerYear;
    
    private EditText getTractorName;
    private EditText getModelNumber;
    private EditText getPin;
    private TextView titleAddTractor;

    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private FusedLocationProviderClient fusedLocationClient;
    private Tractor existingTractor;

    private Uri selectedImageUri;
    private Bitmap selectedBitmap;

    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    selectedBitmap = null;
                    ivTractorImage.setImageURI(selectedImageUri);
                }
            });

    private final ActivityResultLauncher<Intent> takePhotoLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getExtras() != null) {
                    selectedBitmap = (Bitmap) result.getData().getExtras().get("data");
                    selectedImageUri = null;
                    ivTractorImage.setImageBitmap(selectedBitmap);
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

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean coarseLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
                if ((fineLocationGranted != null && fineLocationGranted) || (coarseLocationGranted != null && coarseLocationGranted)) {
                    saveTractorWithLocation();
                } else {
                    Toast.makeText(this, "Location permission is needed to save tractor location", Toast.LENGTH_SHORT).show();
                    uploadImageAndSave("");
                }
            });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActivityContent(R.layout.add_add_tractor);

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance("gs://team-ctrl-alt-elite.appspot.com");
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        ivTractorImage = findViewById(R.id.ivTractorImage);
        spinnerYear = findViewById(R.id.spinnerYear);
        getTractorName = findViewById(R.id.getTractorName);
        getModelNumber = findViewById(R.id.getModelNumber);
        getPin = findViewById(R.id.getPin);
        titleAddTractor = findViewById(R.id.titleAddTractor);

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        }

        View btnUploadImage = findViewById(R.id.btnUploadImage);
        View btnSaveTractor = findViewById(R.id.btnSaveTractor);

        setupYearSpinner();

        getPin.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                if (s.length() >= 8 && titleAddTractor.getText().toString().equals("Add Tractor")) {
                    pinEntered();
                }
            }
        });

        if (getIntent().hasExtra("TRACTOR_DATA")) {
            existingTractor = (Tractor) getIntent().getSerializableExtra("TRACTOR_DATA");
            populateFields(existingTractor);
        }

        if (btnUploadImage != null) {
            btnUploadImage.setOnClickListener(v -> checkPermissionsAndShowOptions());
        }

        if (btnSaveTractor != null) {
            btnSaveTractor.setOnClickListener(v -> checkLocationPermissionsAndSave());
        }
    }


    private void pinEntered(){
        new AlertDialog.Builder(this )
                .setTitle("Tractor Found!")
                .setMessage("Do you want to autofill the rest of the info using the data connected to your PIN?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    autoFill();
                })
                .setNegativeButton("No", null)
                .show();
    }
    private void autoFill(){
        List<String> tractorNames = Arrays.asList(
                "Composter", "Row Crop", "Scraper Special", "Utility Task", "Compact Utility", "Two-Track", "Small Frame", "Legacy Series", "Harvester", "PowerTech"
        );
        Random rando = new Random();
        getTractorName.setText(tractorNames.get(rando.nextInt(tractorNames.size())));
        getModelNumber.setText(String.valueOf(rando.nextInt(9000)+1000));
        if (spinnerYear.getAdapter() != null){
            ArrayAdapter adapter = (ArrayAdapter) spinnerYear.getAdapter();
            int position = adapter.getPosition(String.valueOf(rando.nextInt(37) + 1990));
            if (position >= 0) spinnerYear.setSelection(position);
        }
        Toast.makeText(this, "Tractor details auto-filled!", Toast.LENGTH_SHORT).show();
    }

    private void populateFields(Tractor tractor) {
        if (titleAddTractor != null) titleAddTractor.setText("Edit Tractor");
        getTractorName.setText(tractor.getName());
        getModelNumber.setText(tractor.getModel());
        getPin.setText(tractor.getPin());

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

    private void checkLocationPermissionsAndSave() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        } else {
            saveTractorWithLocation();
        }
    }

    private void saveTractorWithLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                String locationStr = "";
                if (location != null) {
                    locationStr = location.getLatitude() + "," + location.getLongitude();
                }
                uploadImageAndSave(locationStr);
            });
        } else {
            uploadImageAndSave("");
        }
    }

    private void uploadImageAndSave(String locationStr) {
        if (selectedImageUri != null) {
            uploadUri(selectedImageUri, locationStr);
        } else if (selectedBitmap != null) {
            uploadBitmap(selectedBitmap, locationStr);
        } else {
            saveTractorToFirebase(locationStr, null);
        }
    }

    private void uploadUri(Uri uri, String locationStr) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (bitmap != null) {
                uploadBitmap(bitmap, locationStr);
            } else {
                saveTractorToFirebase(locationStr, null);
            }
        } catch (Exception e) {
            Log.e("UPLOAD", "Error reading URI", e);
            saveTractorToFirebase(locationStr, null);
        }
    }

    private void uploadBitmap(Bitmap bitmap, String locationStr) {
        StorageReference ref = storage.getReference().child("tractor_images/" + UUID.randomUUID().toString() + ".jpg");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
        byte[] data = baos.toByteArray();

        ref.putBytes(data).addOnSuccessListener(taskSnapshot -> {
            ref.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                saveTractorToFirebase(locationStr, downloadUri.toString());
            }).addOnFailureListener(e -> {
                Log.e("UPLOAD", "Failed to get download URL", e);
                saveTractorToFirebase(locationStr, null);
            });
        }).addOnFailureListener(e -> {
            Log.e("UPLOAD", "Upload failed", e);
            Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            saveTractorToFirebase(locationStr, null);
        });
    }

    private void saveTractorToFirebase(String locationStr, String imageUrl) {
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

        if (imageUrl != null) {
            tractor.setImageUrl(imageUrl);
        }

        if (!locationStr.isEmpty()) {
            tractor.setLocation(locationStr);
        }

        Random random = new Random();
        if (existingTractor == null) {
            // New tractor: randomize fuel and maintenance status
            tractor.setFuel(random.nextInt(100) + 1);
            if (random.nextInt(5) == 0) { // 20% chance
                tractor.setStatus("Maintenance Required");
            } else {
                tractor.setStatus("Active");
            }
        }
        tractor.setLastUpdated(java.text.DateFormat.getDateTimeInstance().format(new java.util.Date()));

        if (existingTractor != null && existingTractor.getDocumentId() != null) {
            db.collection("tractors").document(existingTractor.getDocumentId())
                    .set(tractor)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(AddTractorActivity.this, "Tractor updated successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show());
        } else {
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

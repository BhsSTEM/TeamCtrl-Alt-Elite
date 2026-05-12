package com.example.ctrl_alt_elite;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
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
import java.util.Locale;
import java.util.Random;
import java.util.UUID;

public class AddTractorActivity extends BaseActivity {

    private ImageView ivTractorImage;
    private AutoCompleteTextView spinnerYear;
    
    private EditText getTractorName, getModelNumber, getPin, getCompanyId, getFuel, getMaintenanceStatus, getSoftwareStatus, getFirmwareStatus, getEngineHours;
    private CheckBox cbMaintenanceWarning, cbSoftwareWarning, cbFirmwareWarning;
    private TextView titleAddTractor, lblTractorImage;
    private View btnUploadImage;

    private FirebaseFirestore db;
    private FirebaseFirestore userDb;
    private FirebaseStorage storage;
    private FusedLocationProviderClient fusedLocationClient;
    private Tractor existingTractor;

    private Uri selectedImageUri;
    private Bitmap selectedBitmap;
    private String userCompanyId = null;
    private String userRole = "";

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
        userDb = FirebaseFirestore.getInstance("sign-up");
        storage = FirebaseStorage.getInstance("gs://team-ctrl-alt-elite.appspot.com");
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        ivTractorImage = findViewById(R.id.ivTractorImage);
        spinnerYear = findViewById(R.id.spinnerYear);
        getTractorName = findViewById(R.id.getTractorName);
        getModelNumber = findViewById(R.id.getModelNumber);
        getPin = findViewById(R.id.getPin);
        getCompanyId = findViewById(R.id.getCompanyId);
        getFuel = findViewById(R.id.getFuel);
        getMaintenanceStatus = findViewById(R.id.getMaintenanceStatus);
        getSoftwareStatus = findViewById(R.id.getSoftwareStatus);
        getFirmwareStatus = findViewById(R.id.getFirmwareStatus);
        getEngineHours = findViewById(R.id.getEngineHours);
        
        cbMaintenanceWarning = findViewById(R.id.cbMaintenanceWarning);
        cbSoftwareWarning = findViewById(R.id.cbSoftwareWarning);
        cbFirmwareWarning = findViewById(R.id.cbFirmwareWarning);
        
        titleAddTractor = findViewById(R.id.titleAddTractor);
        lblTractorImage = findViewById(R.id.lblTractorImage);

        btnUploadImage = findViewById(R.id.btnUploadImage);
        View btnBack = findViewById(R.id.btnBack);
        Button btnSaveTractor = findViewById(R.id.btnSaveTractor);

        setupYearSpinner();
        fetchUserDetails();

        getPin.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() >= 8 && titleAddTractor.getText().toString().equals("Add Tractor") && "Owner".equalsIgnoreCase(userRole)) {
                    pinEntered();
                }
            }
        });

        if (getIntent().hasExtra("TRACTOR_DATA")) {
            existingTractor = (Tractor) getIntent().getSerializableExtra("TRACTOR_DATA");
            populateFields(existingTractor);
        }

        if (btnUploadImage != null) {
            btnUploadImage.setOnClickListener(v -> {
                if ("Owner".equalsIgnoreCase(userRole)) {
                    checkPermissionsAndShowOptions();
                } else {
                    Toast.makeText(this, "Only owners can change tractor photos", Toast.LENGTH_SHORT).show();
                }
            });
        }
        
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnSaveTractor != null) {
            btnSaveTractor.setOnClickListener(v -> checkLocationPermissionsAndSave());
        }
    }

    private void fetchUserDetails() {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();
            userDb.collection("users").document(uid).get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    userCompanyId = documentSnapshot.getString("companyId");
                    String role = documentSnapshot.getString("role");
                    userRole = (role != null) ? role.trim() : "";
                    
                    // Apply role-based restrictions
                    applyRoleRestrictions();

                    // Pre-fill if not editing existing tractor
                    if (existingTractor == null && getCompanyId != null && userCompanyId != null) {
                        getCompanyId.setText(userCompanyId);
                    }
                }
            }).addOnFailureListener(e -> {
                userCompanyId = "";
                userRole = "Operator"; // Default to restricted if check fails
                applyRoleRestrictions();
            });
        }
    }

    private void applyRoleRestrictions() {
        boolean isOwner = "Owner".equalsIgnoreCase(userRole);
        int ownerVisibility = isOwner ? View.VISIBLE : View.GONE;

        // Hide fields only owners can edit
        setInputLayoutVisibility(getTractorName, ownerVisibility);
        setInputLayoutVisibility(getModelNumber, ownerVisibility);
        setInputLayoutVisibility(getPin, ownerVisibility);
        setInputLayoutVisibility(getCompanyId, ownerVisibility);
        setInputLayoutVisibility(spinnerYear, ownerVisibility);

        if (btnUploadImage != null) {
            btnUploadImage.setVisibility(ownerVisibility);
        }
        
        if (lblTractorImage != null) {
            lblTractorImage.setVisibility(ownerVisibility);
        }

        // Ensure fields both can edit are visible and enabled
        getFuel.setEnabled(true);
        getEngineHours.setEnabled(true);
        getMaintenanceStatus.setEnabled(true);
        getSoftwareStatus.setEnabled(true);
        getFirmwareStatus.setEnabled(true);
        cbMaintenanceWarning.setEnabled(true);
        cbSoftwareWarning.setEnabled(true);
        cbFirmwareWarning.setEnabled(true);

        if (!isOwner && titleAddTractor != null && existingTractor != null) {
            titleAddTractor.setText("Update Tractor Status");
        }
    }

    private void setInputLayoutVisibility(View view, int visibility) {
        if (view == null) return;
        View parent = (View) view.getParent();
        if (parent instanceof com.google.android.material.textfield.TextInputLayout) {
            parent.setVisibility(visibility);
        } else if (parent != null && parent.getParent() instanceof com.google.android.material.textfield.TextInputLayout) {
            ((View) parent.getParent()).setVisibility(visibility);
        } else {
            view.setVisibility(visibility);
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
        // ONLY include names that have a corresponding guide URL in Tractor.java
        List<String> validTractorTypes = Arrays.asList(
            "Compact Utility", 
            "Row Crop", 
            "Scraper Special", 
            "4WD", 
            "Track", 
            "Specialty", 
            "Utility", 
            "Harvester", 
            "Combine", 
            "PowerTech"
        );
        
        Random random = new Random();
        String selectedName = validTractorTypes.get(random.nextInt(validTractorTypes.size()));
        getTractorName.setText(selectedName);
        getModelNumber.setText(String.valueOf(random.nextInt(9000)+1000));
        
        String yearVal = String.valueOf(random.nextInt(37) + 1990);
        spinnerYear.setText(yearVal, false);
        
        getFuel.setText(String.valueOf(random.nextInt(101)));
        boolean mWarn = random.nextBoolean();
        getMaintenanceStatus.setText(mWarn ? "Needs Attention" : "Active");
        cbMaintenanceWarning.setChecked(mWarn);
        
        boolean sWarn = random.nextInt(10) > 7;
        getSoftwareStatus.setText(sWarn ? "Update Required" : "Up to date");
        cbSoftwareWarning.setChecked(sWarn);
        
        boolean fWarn = random.nextInt(10) > 7;
        getFirmwareStatus.setText(fWarn ? "Update Required" : "Up to date");
        cbFirmwareWarning.setChecked(fWarn);
        
        getEngineHours.setText(String.format(Locale.US, "%.1f", 100 + (random.nextDouble() * 2000)));


        Toast.makeText(this, "Tractor details auto-filled with valid guide type!", Toast.LENGTH_SHORT).show();
    }

    private void populateFields(Tractor tractor) {
        if (titleAddTractor != null) titleAddTractor.setText("Edit Tractor Details");
        getTractorName.setText(tractor.getName());
        getModelNumber.setText(tractor.getModel());
        getPin.setText(tractor.getPin());
        getFuel.setText(String.valueOf(tractor.getFuel()));
        getMaintenanceStatus.setText(tractor.getStatus());
        getSoftwareStatus.setText(tractor.getSoftwareStatus());
        getFirmwareStatus.setText(tractor.getFirmwareStatus());
        getEngineHours.setText(String.valueOf(tractor.getEngineHours()));
        if (getCompanyId != null) {
            getCompanyId.setText(tractor.getCompanyId());
        }

        cbMaintenanceWarning.setChecked(tractor.isMaintenanceWarning());
        cbSoftwareWarning.setChecked(tractor.isSoftwareWarning());
        cbFirmwareWarning.setChecked(tractor.isFirmwareWarning());
        
        spinnerYear.setText(String.valueOf(tractor.getYear()), false);

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
        if (mAuth.getCurrentUser() == null) return;
        
        String uid = mAuth.getUid();
        String name = getTractorName.getText().toString().trim();
        String model = getModelNumber.getText().toString().trim();
        String pin = getPin.getText().toString().trim();
        String fuelStr = getFuel.getText().toString().trim();
        String statusText = getMaintenanceStatus.getText().toString().trim();
        String softwareText = getSoftwareStatus.getText().toString().trim();
        String firmwareText = getFirmwareStatus.getText().toString().trim();
        String engineHoursStr = getEngineHours.getText().toString().trim();
        String yearStr = spinnerYear.getText().toString();
        String companyId = getCompanyId != null ? getCompanyId.getText().toString().trim() : "";

        if (name.isEmpty() || model.isEmpty() || pin.isEmpty()) {
            Toast.makeText(this, "Please fill in required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int year = 0;
        try {
            year = Integer.parseInt(yearStr);
        } catch (NumberFormatException e) {
             Toast.makeText(this, "Please select a year", Toast.LENGTH_SHORT).show();
             return;
        }

        int fuel = fuelStr.isEmpty() ? 0 : Integer.parseInt(fuelStr);
        double engineHours = engineHoursStr.isEmpty() ? 0.0 : Double.parseDouble(engineHoursStr);

        // Capture old values for notification logic BEFORE updating the object
        String oldStatus = (existingTractor != null) ? existingTractor.getStatus() : "";
        String oldSoftware = (existingTractor != null) ? existingTractor.getSoftwareStatus() : "";
        String oldFirmware = (existingTractor != null) ? existingTractor.getFirmwareStatus() : "";
        boolean oldMW = (existingTractor != null) && existingTractor.isMaintenanceWarning();
        boolean oldSW = (existingTractor != null) && existingTractor.isSoftwareWarning();
        boolean oldFW = (existingTractor != null) && existingTractor.isFirmwareWarning();

        Tractor tractor = (existingTractor != null) ? existingTractor : new Tractor();
        tractor.setUser(uid);
        tractor.setName(name);
        tractor.setModel(model);
        tractor.setPin(pin);
        tractor.setYear(year);
        tractor.setFuel(fuel);
        tractor.setEngineHours(engineHours);
        tractor.setStatus(statusText.isEmpty() ? "Active" : statusText);
        tractor.setSoftwareStatus(softwareText.isEmpty() ? "Up to date" : softwareText);
        tractor.setFirmwareStatus(firmwareText.isEmpty() ? "Up to date" : firmwareText);
        tractor.setCompanyId(companyId);

        tractor.setMaintenanceWarning(cbMaintenanceWarning.isChecked());
        tractor.setSoftwareWarning(cbSoftwareWarning.isChecked());
        tractor.setFirmwareWarning(cbFirmwareWarning.isChecked());
        
        tractor.setGuideUrl(tractor.getGuideUrl());

        if (imageUrl != null) {
            tractor.setImageUrl(imageUrl);
        }

        if (!locationStr.isEmpty()) {
            tractor.setLocation(locationStr);
        }

        tractor.setLastUpdated(java.text.DateFormat.getDateTimeInstance().format(new java.util.Date()));

        // Check for new warnings or status updates to notify owner
        checkAndNotifyNewWarnings(tractor, oldStatus, oldSoftware, oldFirmware, oldMW, oldSW, oldFW);

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

    private void checkAndNotifyNewWarnings(Tractor tractor, String oldStatus, String oldSoftware, String oldFirmware, boolean oldMW, boolean oldSW, boolean oldFW) {
        // Explicitly block notification if the current user is an Owner
        // We only want Owners to be notified when an OPERATOR makes changes.
        if (userRole != null && userRole.trim().equalsIgnoreCase("Owner")) {
            Log.d("NOTIFICATION", "User is Owner. Skipping notification as requested.");
            return;
        }

        StringBuilder warningDetails = new StringBuilder();
        boolean notify = false;

        String curStatus = tractor.getStatus() != null ? tractor.getStatus() : "";
        String prevStatus = oldStatus != null ? oldStatus : "";
        
        // Maintenance
        if (tractor.isMaintenanceWarning()) {
            // Notify if warning is NEW or if status text changed while warning active
            if (!oldMW || !curStatus.equals(prevStatus)) {
                warningDetails.append("- Maintenance: ").append(curStatus).append("\n");
                notify = true;
            }
        } else if (oldMW) {
            // Notify if warning was RESOLVED (turned off)
            warningDetails.append("- Maintenance Warning Resolved\n");
            notify = true;
        }

        // Software
        String curSoft = tractor.getSoftwareStatus() != null ? tractor.getSoftwareStatus() : "";
        String prevSoft = oldSoftware != null ? oldSoftware : "";
        if (tractor.isSoftwareWarning()) {
            if (!oldSW || !curSoft.equals(prevSoft)) {
                warningDetails.append("- Software: ").append(curSoft).append("\n");
                notify = true;
            }
        } else if (oldSW) {
            warningDetails.append("- Software Warning Resolved\n");
            notify = true;
        }

        // Firmware
        String curFirm = tractor.getFirmwareStatus() != null ? tractor.getFirmwareStatus() : "";
        String prevFirm = oldFirmware != null ? oldFirmware : "";
        if (tractor.isFirmwareWarning()) {
            if (!oldFW || !curFirm.equals(prevFirm)) {
                warningDetails.append("- Firmware: ").append(curFirm).append("\n");
                notify = true;
            }
        } else if (oldFW) {
            warningDetails.append("- Firmware Warning Resolved\n");
            notify = true;
        }

        if (notify) {
            sendOwnerNotification(tractor, warningDetails.toString());
        }
    }

    private void sendOwnerNotification(Tractor tractor, String details) {
        // Since a real Push Notification requires a server/Cloud Functions, 
        // we simulate the notification sending logic here. 
        // In a real app, this would trigger an FCM message to all users in the same CompanyId with the 'Owner' role.
        
        String message = "WARNING: Tractor " + tractor.getName() + " (" + tractor.getModel() + ") has reported issues:\n" + details;
        
        // Simulating the push notification with a Toast and Log for now
        Log.d("NOTIFICATION", "Sending Push Notification to Owner(s) of Company " + tractor.getCompanyId() + ":\n" + message);
        Toast.makeText(this, "Notification sent to Owner: Issues reported for " + tractor.getName(), Toast.LENGTH_LONG).show();
        
        // Potential implementation for real FCM:
        /*
        Map<String, Object> notification = new HashMap<>();
        notification.put("toCompanyId", tractor.getCompanyId());
        notification.put("title", "Tractor Warning!");
        notification.put("body", "Tractor " + tractor.getName() + " needs attention.");
        notification.put("details", details);
        db.collection("notifications").add(notification); 
        */
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupNavigation();
    }
}

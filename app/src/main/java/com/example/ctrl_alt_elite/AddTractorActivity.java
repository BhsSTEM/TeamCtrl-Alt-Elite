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
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class AddTractorActivity extends BaseActivity {
    private boolean isEditMode = false;
    private Tractor existingTractor;
    private ImageView ivTractorImage;
    private Spinner spinnerYear;
    private EditText etTractorName, etModelNumber, etPin;
    private TextView titleText;
    private Button btnSave;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    showImagePickerOptions();
                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    ivTractorImage.setImageURI(imageUri);
                }
            });

    private final ActivityResultLauncher<Intent> takePhotoLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bitmap photo = (Bitmap) result.getData().getExtras().get("data");
                    ivTractorImage.setImageBitmap(photo);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActivityContent(R.layout.add_add_tractor);

        ivTractorImage = findViewById(R.id.ivTractorImage);
        spinnerYear = findViewById(R.id.spinnerYear);
        etTractorName = findViewById(R.id.etTractorName);
        etModelNumber = findViewById(R.id.etModelNumber);
        etPin = findViewById(R.id.etPin);
        btnSave = findViewById(R.id.btnSaveTractor);
        
        // Find the title TextView (second child of toolbar)
        View toolbar = findViewById(R.id.toolbar);
        if (toolbar instanceof androidx.constraintlayout.widget.ConstraintLayout) {
             titleText = (TextView) ((androidx.constraintlayout.widget.ConstraintLayout) toolbar).getChildAt(1);
        }

        View btnUploadImage = findViewById(R.id.btnUploadImage);
        TextView btnBack = findViewById(R.id.btnBack);

        setupYearSpinner();

        // Check if we are in Edit Mode
        if (getIntent().hasExtra("TRACTOR_DATA")) {
            existingTractor = (Tractor) getIntent().getSerializableExtra("TRACTOR_DATA");
            if (existingTractor != null) {
                isEditMode = true;
                preFillData();
            }
        }

        if (btnUploadImage != null) {
            btnUploadImage.setOnClickListener(v -> checkPermissionsAndShowOptions());
        }
        
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnSave != null) {
            btnSave.setOnClickListener(v -> saveTractor());
        }
    }

    private void preFillData() {
        if (titleText != null) titleText.setText("Edit Tractor");
        if (btnSave != null) btnSave.setText("Update Tractor");
        
        if (etTractorName != null) etTractorName.setText(existingTractor.getName());
        if (etModelNumber != null) etModelNumber.setText(existingTractor.getModel());
        if (etPin != null) etPin.setText(existingTractor.getPin());
        
        // Set Year Spinner
        String yearStr = String.valueOf(existingTractor.getYear());
        ArrayAdapter adapter = (ArrayAdapter) spinnerYear.getAdapter();
        if (adapter != null) {
            int position = adapter.getPosition(yearStr);
            if (position >= 0) spinnerYear.setSelection(position);
        }

        if (ivTractorImage != null && existingTractor.getImageUrl() != null) {
            Glide.with(this).load(existingTractor.getImageUrl()).placeholder(android.R.drawable.ic_menu_camera).into(ivTractorImage);
        }
    }

    private void saveTractor() {
        // Logic to save to Firebase would go here
        String action = isEditMode ? "updated" : "added";
        Toast.makeText(this, "Tractor " + action + " successfully!", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void setupYearSpinner() {
        List<String> years = new ArrayList<>();
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        for (int i = currentYear; i >= 1950; i--) {
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

    @Override
    protected void onResume() {
        super.onResume();
        setupNavigation();
    }
}

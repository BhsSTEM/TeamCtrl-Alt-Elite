package com.example.ctrl_alt_elite;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class evansMapActivity extends BaseActivity implements OnMapReadyCallback {

    private static final String TAG = "EvansMapActivity";
    private GoogleMap map;
    private FusedLocationProviderClient fusedLocationProviderClient;

    private final LatLng defaultLocation = new LatLng(41.5245, -90.5157);
    private static final int DEFAULT_ZOOM = 15;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean locationPermissionGranted;
    private Location lastKnownLocation;

    private int collapsedHeight = 0;
    private FirebaseFirestore db;
    private List<Tractor> tractorList = new ArrayList<>();
    private TractorAdapter adapter;
    private ListenerRegistration tractorListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActivityContent(R.layout.activity_evans_map);

        db = FirebaseFirestore.getInstance();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        setupRecyclerView();
        setupSearchAnimation();
        setupSearchLogic();
    }

    private void setupRecyclerView() {
        RecyclerView rv = findViewById(R.id.rvTractorsMap);
        if (rv != null) {
            rv.setLayoutManager(new LinearLayoutManager(this));
            adapter = new TractorAdapter(tractorList, true); // Set isMapContext to true
            rv.setAdapter(adapter);
        }
    }

    private void setupSearchAnimation() {
        final EditText searchInput = findViewById(R.id.editTextText2);
        final View chip3 = findViewById(R.id.chip3);

        if (searchInput != null && chip3 != null) {
            chip3.post(() -> collapsedHeight = chip3.getHeight());

            searchInput.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    animatePanelHeight(chip3, calculateTargetHeight());
                }
            });
        }
    }

    private void setupSearchLogic() {
        EditText bottomSearchInput = findViewById(R.id.editTextText2);
        if (bottomSearchInput != null) {
            bottomSearchInput.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                        (event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    performSearch(v.getText().toString());
                    return true;
                }
                return false;
            });
        }
    }

    private void performSearch(String query) {
        if (query == null || query.isEmpty()) return;

        for (Tractor tractor : tractorList) {
            if (tractor.getName() != null && tractor.getName().toLowerCase().contains(query.toLowerCase())) {
                showTractorOnMap(tractor);
                return;
            }
        }

        LatLng latLng = parseLocation(query);
        if (latLng != null) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM));
            map.addMarker(new MarkerOptions().position(latLng).title(query));
        } else {
            Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show();
        }
    }

    public void showTractorOnMap(Tractor tractor) {
        LatLng pos = parseLocation(tractor.getLocation());
        if (pos == null) return;
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 17));

        // Collapse the search panel if open
        final View chip3 = findViewById(R.id.chip3);
        if (chip3 != null && collapsedHeight > 0) {
            animatePanelHeight(chip3, collapsedHeight);
            View searchInput = findViewById(R.id.editTextText2);
            if (searchInput != null) searchInput.clearFocus();
        }
    }

    private int calculateTargetHeight() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        return (displayMetrics.heightPixels * 5) / 12;
    }

    private void animatePanelHeight(final View panel, int targetHeight) {
        if (panel == null) return;
        ValueAnimator animator = ValueAnimator.ofInt(panel.getHeight(), targetHeight);
        animator.addUpdateListener(animation -> {
            int val = (Integer) animation.getAnimatedValue();
            ViewGroup.LayoutParams layoutParams = panel.getLayoutParams();
            layoutParams.height = val;
            panel.setLayoutParams(layoutParams);
        });
        animator.setDuration(300);
        animator.start();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.map = googleMap;

        this.map.setOnMapClickListener(latLng -> {
            final View chip3 = findViewById(R.id.chip3);
            if (chip3 != null && collapsedHeight > 0) {
                animatePanelHeight(chip3, collapsedHeight);
                View searchInput = findViewById(R.id.editTextText2);
                if (searchInput != null) searchInput.clearFocus();
            }
        });

        startTractorListener();
        setupCustomControls();
        getLocationPermission();
        updateLocationUI();
        getDeviceLocation();
    }

    private void startTractorListener() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;

        if (tractorListener != null) {
            tractorListener.remove();
        }

        tractorListener = db.collection("tractors")
                .whereEqualTo("user", FirebaseAuth.getInstance().getCurrentUser().getUid())
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            Log.e(TAG, "Listen failed.", error);
                            return;
                        }

                        if (value != null) {
                            tractorList.clear();
                            if (map != null) map.clear(); // Clear existing markers

                            for (QueryDocumentSnapshot document : value) {
                                try {
                                    Tractor tractor = document.toObject(Tractor.class);
                                    tractor.setDocumentId(document.getId());

                                    Object locObj = document.get("location");
                                    if (locObj instanceof GeoPoint) {
                                        GeoPoint gp = (GeoPoint) locObj;
                                        tractor.setLocation(gp.getLatitude() + "," + gp.getLongitude());
                                    } else if (locObj instanceof String) {
                                        tractor.setLocation((String) locObj);
                                    }

                                    tractorList.add(tractor);
                                    addTractorMarker(tractor);
                                } catch (Exception e) {
                                    Log.e(TAG, "Error parsing tractor: " + document.getId(), e);
                                }
                            }
                            if (adapter != null) adapter.notifyDataSetChanged();
                        }
                    }
                });
    }

    private LatLng parseLocation(String locationStr) {
        if (locationStr == null || locationStr.isEmpty()) return null;

        String[] parts = locationStr.split(",");
        if (parts.length == 2) {
            try {
                double lat = Double.parseDouble(parts[0].trim());
                double lng = Double.parseDouble(parts[1].trim());
                return new LatLng(lat, lng);
            } catch (NumberFormatException ignored) {}
        }

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(locationStr, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                return new LatLng(address.getLatitude(), address.getLongitude());
            }
        } catch (IOException e) {
            Log.e(TAG, "Geocoding failed for: " + locationStr, e);
        }
        
        return null;
    }

    private void addTractorMarker(Tractor tractor) {
        LatLng position = parseLocation(tractor.getLocation());
        if (position == null) return;
        
        Object imageSource;
        String imageUrl = tractor.getImageUrl();
        
        if (imageUrl == null || imageUrl.isEmpty() || imageUrl.equals("link")) {
            imageSource = R.drawable.pngimg_com___tractor_png101303_removebg_preview;
        } else {
            imageSource = imageUrl;
        }

        Glide.with(this)
            .asBitmap()
            .load(imageSource)
            .override(100, 100)
            .circleCrop()
            .into(new CustomTarget<Bitmap>() {
                @Override
                public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                    if (map != null) {
                        Marker marker = map.addMarker(new MarkerOptions()
                            .position(position)
                            .title(tractor.getName())
                            .icon(BitmapDescriptorFactory.fromBitmap(resource)));
                        if (marker != null) marker.setTag(tractor);
                    }
                }
                @Override
                public void onLoadCleared(@Nullable Drawable placeholder) {}

                @Override
                public void onLoadFailed(@Nullable Drawable errorDrawable) {
                    addDefaultMarker(position, tractor);
                }
            });
    }

    private void addDefaultMarker(LatLng position, Tractor tractor) {
        if (map != null) {
            Marker marker = map.addMarker(new MarkerOptions()
                .position(position)
                .title(tractor.getName()));
            if (marker != null) marker.setTag(tractor);
        }
    }

    private void setupCustomControls() {
        if (map == null) return;
        map.getUiSettings().setZoomControlsEnabled(false);
        map.getUiSettings().setMyLocationButtonEnabled(false);
        map.getUiSettings().setCompassEnabled(true);

        ImageButton btnZoomIn = findViewById(R.id.btnZoomIn);
        if (btnZoomIn != null) btnZoomIn.setOnClickListener(v -> map.animateCamera(CameraUpdateFactory.zoomIn()));

        ImageButton btnZoomOut = findViewById(R.id.btnZoomOut);
        if (btnZoomOut != null) btnZoomOut.setOnClickListener(v -> map.animateCamera(CameraUpdateFactory.zoomOut()));

        FloatingActionButton fabMyLocation = findViewById(R.id.fabMyLocation);
        if (fabMyLocation != null) fabMyLocation.setOnClickListener(v -> getDeviceLocation());

        FloatingActionButton fabLayers = findViewById(R.id.fabLayers);
        if (fabLayers != null) fabLayers.setOnClickListener(v -> showMapTypeDialog());
    }

    private void showMapTypeDialog() {
        final String[] types = {"Normal", "Satellite", "Terrain", "Hybrid"};
        new AlertDialog.Builder(this)
                .setTitle("Select Map Type")
                .setItems(types, (dialog, which) -> {
                    if (map == null) return;
                    switch (which) {
                        case 0: map.setMapType(GoogleMap.MAP_TYPE_NORMAL); break;
                        case 1: map.setMapType(GoogleMap.MAP_TYPE_SATELLITE); break;
                        case 2: map.setMapType(GoogleMap.MAP_TYPE_TERRAIN); break;
                        case 3: map.setMapType(GoogleMap.MAP_TYPE_HYBRID); break;
                    }
                }).show();
    }

    private void getDeviceLocation() {
        try {
            if (locationPermissionGranted) {
                fusedLocationProviderClient.getLastLocation().addOnCompleteListener(this, task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        lastKnownLocation = task.getResult();
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(
                                new LatLng(lastKnownLocation.getLatitude(),
                                        lastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                    }
                });
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Exception: " + e.getMessage(), e);
        }
    }

    private void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationPermissionGranted = true;
                updateLocationUI();
            }
        }
    }

    private void updateLocationUI() {
        if (map == null) return;
        try {
            if (locationPermissionGranted) {
                map.setMyLocationEnabled(true);
            } else {
                map.setMyLocationEnabled(false);
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Exception: " + e.getMessage());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupNavigation();
        if (tractorListener == null && map != null) {
            startTractorListener();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (tractorListener != null) {
            tractorListener.remove();
            tractorListener = null;
        }
    }
}
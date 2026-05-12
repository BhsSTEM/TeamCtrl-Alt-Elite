package com.example.ctrl_alt_elite;

import static com.google.api.ResourceProto.resource;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.location.Priority;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import android.content.pm.PackageManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import android.Manifest;

public class MainActivity extends BaseActivity implements OnMapReadyCallback {
    //Variables
    private static final String TAG = "MainActivity";
    private TextView tempText;
    private TextView weatherDesc;
    private TextView rainInfo;
    private ImageView weatherIcon;
    private ImageView linkToNoaa;
    private ImageView mapView;
    private FirebaseFirestore userdb;
    private FirebaseAuth mAuth;
    private String notifText;
    private FusedLocationProviderClient fusedLocationClient;
    private GoogleMap map2;
    private static final String USER_AGENT = "TeamCtrlAltElite/1.0 (contact@example.com)";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setActivityContent(R.layout.activity_main);

        // Initialize Weather Views
        tempText = findViewById(R.id.weather_temp);
        weatherDesc = findViewById(R.id.weather_desc);
        rainInfo = findViewById(R.id.RainInfo);
        weatherIcon = findViewById(R.id.weather_icon);
        linkToNoaa = findViewById(R.id.LinkToNOAA);
        // Initialize Firebase Auth
        userdb = FirebaseFirestore.getInstance("sign-up");
        mAuth = FirebaseAuth.getInstance();
        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        // Fetch weather using coordinates and finding location
        getLastLocation();

        // Initialize Map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map2);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        
        // Link to Map Activity
        mapView = findViewById(R.id.MapView);
        if (mapView != null){
            mapView.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, evansMapActivity.class);
                startActivity(intent);
            });
        }
    }
    //Update map location & zoom
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map2 = googleMap;
        updateMapLocationUI();
        //fetchTractorsFromFirestore();
    }
    //Get location for map
    private void updateMapLocationUI() {
        if (map2 == null) return;
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                map2.setMyLocationEnabled(true);
                map2.getUiSettings().setMyLocationButtonEnabled(true);
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void getLastLocation(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 100);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                updateWeatherData(location.getLatitude(), location.getLongitude());
                //Zoom into location
                moveCameraToLocation(location);
            } else {
                Toast.makeText(MainActivity.this, "Requesting fresh location...", Toast.LENGTH_SHORT).show();
                requestFreshLocation();
            }
        });
    }
    //Zoom into location
    private void moveCameraToLocation(Location location) {
        if (map2 != null && location != null) {
            map2.animateCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(location.getLatitude(), location.getLongitude()), 5));
        }
    }
    //Add tractor markers to map
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
    /*
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
                        if (map2 != null) {
                            Marker marker = map2.addMarker(new MarkerOptions()
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
        if (map2 != null) {
            Marker marker = map2.addMarker(new MarkerOptions()
                    .position(position)
                    .title(tractor.getName()));
            if (marker != null) marker.setTag(tractor);
        }
    }
    private void fetchTractorsFromFirestore() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Log.e("MAP_DEBUG", "No user logged in!");
            return;
        }

        // ALWAYS use default instance unless you have multiple Firebase projects
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("tractors")
                .whereEqualTo("userId", user.getUid())
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("MAP_DEBUG", "Firestore error: " + error.getMessage());
                        return;
                    }

                    if (value != null) {
                        if (map2 != null) map2.clear();
                        Log.d("MAP_DEBUG", "Tractors found: " + value.size());

                        for (com.google.firebase.firestore.QueryDocumentSnapshot doc : value) {
                            Tractor tractor = doc.toObject(Tractor.class);
                            if (tractor.getLocation() != null) {
                                addTractorMarker(tractor);
                            } else {
                                Log.e("MAP_DEBUG", "Tractor " + tractor.getName() + " has NO location string!");
                            }
                        }
                    }
                });
    }
     */
    //Update location
    private void requestFreshLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        com.google.android.gms.location.LocationRequest locationRequest = new com.google.android.gms.location.LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setMinUpdateIntervalMillis(5000)
                .setMaxUpdates(1)
                .build();

        fusedLocationClient.requestLocationUpdates(locationRequest, new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult.getLastLocation() != null) {
                    Location location = locationResult.getLastLocation();
                    updateWeatherData(location.getLatitude(), location.getLongitude());
                    moveCameraToLocation(location);
                }
            }
        }, getMainLooper());
    }

    private void updateWeatherData(double latitude, double longitude) {
        // Fetch weather using coordinates
        fetchNoaaWeather(latitude, longitude);
        fetchWeatherAlerts(latitude, longitude);
        // Update UI with weather information link
        if (linkToNoaa != null) {
            linkToNoaa.setOnClickListener(v -> {
                String url = String.format(Locale.US, "https://forecast.weather.gov/MapClick.php?lat=%f&lon=%f", latitude, longitude);
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            });
        }
    }

    private void fetchWeatherAlerts(double lat, double lon) {
        TextView notificationText = findViewById(R.id.Notification);
        if (notificationText == null) return;

        String point = String.format(Locale.US, "%.4f,%.4f", lat, lon);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.weather.gov/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        WeatherApiService service = retrofit.create(WeatherApiService.class);

        service.getActiveAlerts(point).enqueue(new Callback<WAlertsResponse>() {
            @Override
            public void onResponse(Call<WAlertsResponse> call, Response<WAlertsResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().features != null && !response.body().features.isEmpty()) {
                    String alertEvent = response.body().features.get(0).properties.event;
                    notificationText.setText(String.format("ALERT: %s", alertEvent));
                    notificationText.setTextColor(android.graphics.Color.RED);
                } else {
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null) {
                        userdb = FirebaseFirestore.getInstance("sign-up");
                        String uid = user.getUid();
                        userdb.collection("users").document(uid).get().addOnCompleteListener((task) -> {
                            if (task.isSuccessful() && task.getResult() != null) {
                                notifText = task.getResult().getString("name");
                                notificationText.setText(notifText);
                            }
                        });
                    }else {
                        notificationText.setText("No Alerts");
                    }
                    notificationText.setTextColor(android.graphics.Color.WHITE);
                }
            }

            @Override
            public void onFailure(Call<WAlertsResponse> call, Throwable t) {
                notificationText.setText("Notifications Error");
            }
        });
    }

    private void fetchNoaaWeather(double lat, double lon) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.weather.gov/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        WeatherApiService service = retrofit.create(WeatherApiService.class);

        service.getPoints(lat, lon).enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().properties != null) {
                    String forecastUrl = response.body().properties.forecastUrl;
                    getActualForecast(service, forecastUrl);
                } else {
                    Toast.makeText(MainActivity.this, "NOAA Point Error", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Network Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getActualForecast(WeatherApiService service, String url) {
        service.getForecast(url).enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().properties != null
                    && response.body().properties.periods != null && !response.body().properties.periods.isEmpty()) {
                    
                    WeatherResponse.Period current = response.body().properties.periods.get(0);
                    
                    if (tempText != null) tempText.setText(getString(R.string.weather_format, current.temperature));
                    if (weatherDesc != null) weatherDesc.setText(current.shortForecast);
                    
                    // Display Precipitation Chance
                    if (rainInfo != null) {
                        if (current.probabilityOfPrecipitation != null && current.probabilityOfPrecipitation.value != null) {
                            rainInfo.setText(String.format(Locale.US, "Rain: %d%%", current.probabilityOfPrecipitation.value));
                        } else {
                            rainInfo.setText("Rain: 0%");
                        }
                    }

                    if (weatherIcon != null && current.icon != null) {
                        GlideUrl glideUrl = new GlideUrl(current.icon, new LazyHeaders.Builder()
                                .addHeader("User-Agent", USER_AGENT)
                                .build());

                        Glide.with(MainActivity.this)
                                .load(glideUrl)
                                .into(weatherIcon);
                    }
                }
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Forecast Error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}

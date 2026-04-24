package com.example.ctrl_alt_elite;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import com.google.android.gms.location.Priority;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;

import android.content.pm.PackageManager;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import android.Manifest;

public class MainActivity extends BaseActivity {
    //Variables
    private TextView tempText;
    private TextView weatherDesc;
    private TextView rainInfo;
    private ImageView weatherIcon;
    private ImageView linkToNoaa;
    private ImageView mapView;
    private FusedLocationProviderClient fusedLocationClient;
    private static final String USER_AGENT = "TeamCtrlAltElite/1.0 (contact@example.com)";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setActivityContent(R.layout.activity_main);

        // Initialize Weather Views with your updated XML IDs
        tempText = findViewById(R.id.weather_temp);
        weatherDesc = findViewById(R.id.weather_desc);
        rainInfo = findViewById(R.id.RainInfo);
        weatherIcon = findViewById(R.id.weather_icon);
        linkToNoaa = findViewById(R.id.LinkToNOAA);

        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        // Fetch weather using coordinates and finding location
        getLastLocation();

        // Set up the link to Map Activity
        mapView = findViewById(R.id.MapView);
        if (mapView != null){
            mapView.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, evansMapActivity.class);
                startActivity(intent);
            });
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
            } else {
                Toast.makeText(MainActivity.this, "Requesting fresh location...", Toast.LENGTH_SHORT).show();
                requestFreshLocation();
            }
        });
    }

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
                    updateWeatherData(locationResult.getLastLocation().getLatitude(), locationResult.getLastLocation().getLongitude());
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
                    notificationText.setText("No Alerts");
                    notificationText.setTextColor(android.graphics.Color.BLACK);
                }
            }

            @Override
            public void onFailure(Call<WAlertsResponse> call, Throwable t) {
                notificationText.setText("Hello User!");
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
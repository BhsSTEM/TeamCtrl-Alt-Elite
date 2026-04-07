package com.example.ctrl_alt_elite;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import android.Manifest;

public class MainActivity extends BaseActivity {

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

        // Fetch weather using coordinates (Davenport, IA area)
        double latitude = 41.5245;
        double longitude = -90.5157;
        fetchNoaaWeather(latitude, longitude);

        // Set up the link to weather.gov
        if (linkToNoaa != null) {
            linkToNoaa.setOnClickListener(v -> {
                String url = String.format(Locale.US, "https://forecast.weather.gov/MapClick.php?lat=%f&lon=%f", latitude, longitude);
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            });
        }
        // Set up the link to Map Activity
        mapView = findViewById(R.id.MapView);
        if (mapView != null){
            mapView.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, evansMapActivity.class);
                startActivity(intent);
            });
        }

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
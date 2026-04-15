package com.example.ctrl_alt_elite;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Path;
import retrofit2.http.Url;

public interface WeatherApiService {
    @Headers("User-Agent: TeamCtrlAltElite/1.0 (contact@example.com)")
    @GET("points/{latitude},{longitude}")
    Call<WeatherResponse> getPoints(
        @Path("latitude") double lat,
        @Path("longitude") double lon
    );

    @Headers("User-Agent: TeamCtrlAltElite/1.0 (contact@example.com)")
    @GET
    Call<WeatherResponse> getForecast(@Url String url);

    @Headers("User-Agent: TeamCtrlAltElite/1.0 (contact@example.com)")
    @GET("alerts/active?point={lat},{lon}")
    Call<WAlertsResponse> getActiveAlerts(@Path("lat") double lat, @Path("lon") double lon);
}
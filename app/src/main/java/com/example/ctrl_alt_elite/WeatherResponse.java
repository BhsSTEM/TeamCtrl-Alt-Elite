package com.example.ctrl_alt_elite;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class WeatherResponse {
    @SerializedName("properties")
    public Properties properties;

    public static class Properties {
        @SerializedName("forecast")
        public String forecastUrl;
        
        @SerializedName("periods")
        public List<Period> periods;
    }

    public static class Period {
        @SerializedName("temperature")
        public int temperature;
        
        @SerializedName("shortForecast")
        public String shortForecast;
        
        @SerializedName("icon")
        public String icon;

        @SerializedName("probabilityOfPrecipitation")
        public Precipitation probabilityOfPrecipitation;
    }

    public static class Precipitation {
        @SerializedName("unitCode")
        public String unitCode;
        
        @SerializedName("value")
        public Integer value; // Use Integer because it can be null
    }
}
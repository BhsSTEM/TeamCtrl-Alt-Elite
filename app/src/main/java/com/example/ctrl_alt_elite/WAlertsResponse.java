package com.example.ctrl_alt_elite;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class WAlertsResponse {
    @SerializedName("features")
    public List<AlertFeature> features;

    public static class AlertFeature {
        @SerializedName("properties")
        public AlertProperties properties;
    }

    public static class AlertProperties {
        @SerializedName("event")
        public String event;

        @SerializedName("headline")
        public String headline;

        @SerializedName("description")
        public String description;

        @SerializedName("severity")
        public String severity;

        @SerializedName("certainty")
        public String certainty;

        @SerializedName("areaDesc")
        public String areaDesc;

        @SerializedName("effective")
        public String effective;

        @SerializedName("onset")
        public String onset;

        @SerializedName("expires")
        public String expires;

        @SerializedName("ends")
        public String ends;
    }
}
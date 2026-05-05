package com.example.ctrl_alt_elite;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;
import java.io.Serializable;

public class Tractor implements Serializable {
    private String documentId;
    private String name;
    private String model;
    private int year;
    private String status;
    private int fuel;
    private String lastUpdated;
    private String pin;
    private String location;
    private String imageUrl;
    private String userId;
    private double engineHours;

    public Tractor() {}

    @Exclude
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    @PropertyName("tracterName")
    public String getName() { return name; }

    @PropertyName("tracterName")
    public void setName(String name) { this.name = name; }

    @PropertyName("modelNumber")
    public String getModel() { return model; }

    @PropertyName("modelNumber")
    public void setModel(String model) { this.model = model; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getFuel() { return fuel; }
    public void setFuel(int fuel) { this.fuel = fuel; }

    public String getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(String lastUpdated) { this.lastUpdated = lastUpdated; }

    public String getPin() { return pin; }
    public void setPin(String pin) { this.pin = pin; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    @PropertyName("imageUrl")
    public String getImageUrl() { return imageUrl; }

    @PropertyName("imageUrl")
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    @PropertyName("user")
    public String getUser() { return userId; }
    
    @PropertyName("user")
    public void setUser(String userId) { this.userId = userId; }

    public double getEngineHours() { return engineHours; }
    public void setEngineHours(double engineHours) { this.engineHours = engineHours; }
}
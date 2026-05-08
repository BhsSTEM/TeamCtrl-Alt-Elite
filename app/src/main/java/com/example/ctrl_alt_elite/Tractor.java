package com.example.ctrl_alt_elite;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    private String softwareStatus;
    private String firmwareStatus;
    private String guideUrl;
    
    // Warning flags
    private boolean maintenanceWarning;
    private boolean softwareWarning;
    private boolean firmwareWarning;

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

    public String getSoftwareStatus() { return softwareStatus; }
    public void setSoftwareStatus(String softwareStatus) { this.softwareStatus = softwareStatus; }

    public String getFirmwareStatus() { return firmwareStatus; }
    public void setFirmwareStatus(String firmwareStatus) { this.firmwareStatus = firmwareStatus; }

    public String getGuideUrl() { 
        // Always try to generate the most accurate URL based on current name first
        String dynamic = generateDynamicGuideUrl();
        // If we found a specific mapping, use it. 
        // Otherwise, if we have a saved override URL, use that.
        // Fallback to the base URL if nothing else works.
        if (!dynamic.endsWith("quick-reference-guides/")) {
            return dynamic;
        }
        return (guideUrl != null && !guideUrl.isEmpty()) ? guideUrl : dynamic; 
    }
    public void setGuideUrl(String guideUrl) { this.guideUrl = guideUrl; }

    public boolean isMaintenanceWarning() { return maintenanceWarning; }
    public void setMaintenanceWarning(boolean maintenanceWarning) { this.maintenanceWarning = maintenanceWarning; }

    public boolean isSoftwareWarning() { return softwareWarning; }
    public void setSoftwareWarning(boolean softwareWarning) { this.softwareWarning = softwareWarning; }

    public boolean isFirmwareWarning() { return firmwareWarning; }
    public void setFirmwareWarning(boolean firmwareWarning) { this.firmwareWarning = firmwareWarning; }

    @Exclude
    public String generateDynamicGuideUrl() {
        String baseUrl = "https://www.deere.com/en/parts-and-service/manuals-and-training/quick-reference-guides/";
        Map<String, String> mapping = new LinkedHashMap<>();
        // Use directory paths as they are the reliable canonical links on the JD site
        mapping.put("Compact Utility", "compact-utility-tractors/");
        mapping.put("Row Crop", "row-crop-tractors/");
        mapping.put("Scraper Special", "scraper-special-tractors/");
        mapping.put("4WD", "4wd-and-track-tractors/");
        mapping.put("Track", "4wd-and-track-tractors/");
        mapping.put("Specialty", "specialty-tractors/");
        mapping.put("Utility", "utility-tractors/");
        mapping.put("Harvester", "combines/");
        mapping.put("Combine", "combines/");
        mapping.put("PowerTech", "engines/");

        if (name == null || name.isEmpty()) return baseUrl;
        String lowerName = name.toLowerCase();
        for (Map.Entry<String, String> entry : mapping.entrySet()) {
            if (lowerName.contains(entry.getKey().toLowerCase())) {
                return baseUrl + entry.getValue();
            }
        }
        return baseUrl;
    }
}
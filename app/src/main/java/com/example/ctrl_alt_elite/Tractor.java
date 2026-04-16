package com.example.ctrl_alt_elite;
import com.google.firebase.firestore.PropertyName;

    public class Tractor {
        private String name;
        private String model;
        private int year;
        private String status;
        private int fuel;
        private String lastUpdated;
        private String pin;

        private String imageUrl;

        public Tractor() {}
//data
        @com.google.firebase.firestore.PropertyName("tracterName")
        public String getName() { return name; }
        @com.google.firebase.firestore.PropertyName("tracterName")
        public void setName(String name) { this.name = name; }

        @com.google.firebase.firestore.PropertyName("modelNumber")
        public String getModel() { return model; }
        @com.google.firebase.firestore.PropertyName("modelNumber")
        public void setModel(String model) { this.model = model; }

        @com.google.firebase.firestore.PropertyName("imageUrl")
        public String getImageUrl() { return imageUrl; }
        @com.google.firebase.firestore.PropertyName("imageUrl")
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public int getFuel() { return fuel; }
        public void setFuel(int fuel) { this.fuel = fuel; }
        public String getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(String lastUpdated) { this.lastUpdated = lastUpdated; }
        public int getYear() { return year; }
        public void setYear(int year) { this.year = year; }
        public String getPin() { return pin; }
        public void setPin(String pin) { this.pin = pin; }

}



/////
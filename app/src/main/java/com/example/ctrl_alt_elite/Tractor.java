package com.example.ctrl_alt_elite;

public class Tractor {
    private String name;
    private String year;
    private String model;

    public Tractor(String name, String year, String model) {
        this.name = name;
        this.year = year;
        this.model = model;
    }

    public String getName() { return name; }
    public String getYear() { return year; }
    public String getModel() { return model; }
}

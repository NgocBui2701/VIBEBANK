package com.example.vibebank.model;

import com.google.android.gms.maps.model.LatLng;

public class Branch {
    private String id;
    private String name;
    private String address;
    private LatLng location;
    private String phone;
    private String openingHours;
    private float distanceInKm; // Khoảng cách từ vị trí hiện tại (km)

    public Branch(String id, String name, String address, LatLng location, String phone, String openingHours) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.location = location;
        this.phone = phone;
        this.openingHours = openingHours;
        this.distanceInKm = 0;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public LatLng getLocation() {
        return location;
    }

    public void setLocation(LatLng location) {
        this.location = location;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getOpeningHours() {
        return openingHours;
    }

    public void setOpeningHours(String openingHours) {
        this.openingHours = openingHours;
    }

    public float getDistanceInKm() {
        return distanceInKm;
    }

    public void setDistanceInKm(float distanceInKm) {
        this.distanceInKm = distanceInKm;
    }

    public String getDistanceText() {
        if (distanceInKm < 1) {
            return String.format("%.0f m", distanceInKm * 1000);
        } else {
            return String.format("%.1f km", distanceInKm);
        }
    }
}


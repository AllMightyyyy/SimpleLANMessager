package org.zakaria;

public class User {
    private String userName;
    private double latitude;
    private double longitude;

    public User(String userName, double latitude, double longitude) {
        this.userName = userName;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getUserName() {
        return userName;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
}
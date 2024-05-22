package com.example.meuapp;

public class Region {
    private static int lastUserCode = 0; // Static variable to keep track of last assigned user code
    private String name;
    private double latitude;
    private double longitude;
    private int userCode;
    private long timeStamp = System.nanoTime();

    public Region(String name, double latitude, double longitude) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.userCode = ++lastUserCode;
    }

    public Region(String name, double latitude, double longitude, int userCode) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.userCode = userCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public int getUserCode() { return userCode; }

    public void setUserCode(int userCode) { this.userCode = userCode; }

    public long getTimeStamp() { return timeStamp; }

    public void setTimeStamp(long timeStamp) { this.timeStamp = timeStamp; }
}

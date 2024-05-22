package com.example.meuscalculos;

import android.location.Location;

public class Calculos {

    public static boolean isRegionWithinRadius(double Lati, double Longi, Location currentLocation) {
        final int MAX_DISTANCE_METERS = 30;
        float[] distanceInMeters = new float[1];
        Location.distanceBetween(Lati,
                Longi,
                currentLocation.getLatitude(),
                currentLocation.getLongitude(),
                distanceInMeters);

        // If found a region within 30-meter radius, return true
        return distanceInMeters[0] <= MAX_DISTANCE_METERS;
    }

}

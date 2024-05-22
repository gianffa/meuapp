package com.example.meuapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.CountDownLatch;

import com.example.meuscalculos.Calculos;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    // Declare a boolean flag to track if the initial location has been set
    private boolean initialLocationSet = false;
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String TAG = "MainActivityOnClickDbButton";
    private TextView latitudeTextView, longitudeTextView;
    private GoogleMap googleMap;
    private Queue<Region> regionQueue;
    private LocationManager locationManager;
    private Marker userMarker;
    private Semaphore semaphore;
    private boolean regionFoundInQueue;
    private boolean regionFoundInFirestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        latitudeTextView = findViewById(R.id.latitudeTextView);
        longitudeTextView = findViewById(R.id.longitudeTextView);
        Button addLocationButton = findViewById(R.id.addLocationButton);
        Button storeOnDatabaseButton = findViewById(R.id.storeOnDatabaseButton);
        regionQueue = new LinkedList<>();
        semaphore = new Semaphore(1); // Initialize semaphore with 1 permit

        storeOnDatabaseButton.setOnClickListener(v ->{

            // Start a new thread to handle database operations
            new Thread(() -> {
                try {
                    semaphore.acquire();
                    // Access Firestore instance
                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    if(!regionQueue.isEmpty()) {
                        // Iterate through the queue and store regions in Firestore
                        while (!regionQueue.isEmpty()) {
                            Region region = regionQueue.poll();

                            // Create a document in Firestore for the region
                            DocumentReference docRef = db.collection("regions").document();
                            Map<String, Object> data = new HashMap<>();
                            assert region != null : "Region object is null";
                            data.put("latitude", region.getLatitude());
                            data.put("longitude", region.getLongitude());
                            data.put("name", region.getName());
                            data.put("timestamp", formatTime(region.getTimeStamp()));
                            data.put("userCode", region.getUserCode());
                            docRef.set(data)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "Region stored successfully");
                                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Region stored successfully", Toast.LENGTH_SHORT).show());
                                        // Handle success if needed
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error storing region: " + e.getMessage());
                                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error storing region", Toast.LENGTH_SHORT).show());
                                        // Handle failure if needed
                                    });
                        }
                    }
                    else{runOnUiThread(() -> Toast.makeText(MainActivity.this, "No location to be stored", Toast.LENGTH_SHORT).show());}

                } catch (Exception e) {
                    Log.e(TAG, "Error storing regions: " + e.getMessage());
                    // Handle any other exceptions
                } finally {
                    semaphore.release();
                }
            }).start();

        });

        addLocationButton.setOnClickListener(v -> {

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED) {
                // Request the permission if it is not granted
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        PERMISSION_REQUEST_CODE);
            }

            locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            // Check the queue
            Thread queueCheckThread = new Thread(() -> {
                try {
                    semaphore.acquire();
                    regionFoundInQueue = false;
                    for (Region region : regionQueue) {
                        assert lastKnownLocation != null;
                        if (Calculos.isRegionWithinRadius(region.getLatitude(), region.getLongitude(), lastKnownLocation)){
                            regionFoundInQueue = true;
                            runOnUiThread(() -> {
                                Toast.makeText(MainActivity.this, "Region already within 30-meter radius in the queue", Toast.LENGTH_SHORT).show();
                            });
                            break;
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    Log.e(TAG, "Thread interrupted", e);
                } finally {
                    semaphore.release();
                }
            });

            // Check the Firestore
            Thread firestoreCheckThread = new Thread(() -> {
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                CollectionReference regionsRef = db.collection("regions");

                CountDownLatch latch = new CountDownLatch(1);

                regionsRef.get().addOnSuccessListener(querySnapshot -> {
                    regionFoundInFirestore = false;
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        Double latitude = document.getDouble("latitude");
                        Double longitude = document.getDouble("longitude");

                        if (latitude != null && longitude != null) {
                            assert lastKnownLocation != null;
                            if(Calculos.isRegionWithinRadius(latitude, longitude, lastKnownLocation)){
                                regionFoundInFirestore = true;
                                runOnUiThread(() -> {
                                    Toast.makeText(MainActivity.this, "Region already within 30-meter radius in the database", Toast.LENGTH_SHORT).show();
                                });
                                break;
                            }
                        }
                    }
                    latch.countDown();
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Error retrieving documents: " + e.getMessage());
                    latch.countDown();
                });

                try {
                    latch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    Log.e(TAG, "Thread interrupted", e);
                }
            });

            if (lastKnownLocation != null) {
                new Thread(() -> {
                    try {
                        // Start both threads
                        queueCheckThread.start();
                        firestoreCheckThread.start();

                        // Wait for both threads to finish
                        queueCheckThread.join();
                        firestoreCheckThread.join();

                        // If neither found a region within 30 meters, add the new region
                        if (!regionFoundInQueue && !regionFoundInFirestore) {
                            try {
                                semaphore.acquire();
                                regionQueue.add(new Region("RegionName", lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()));
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                Log.e(TAG, "Thread interrupted", e);
                            } finally {
                                semaphore.release();
                            }
                            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Region added to queue", Toast.LENGTH_SHORT).show());
                        } else {
                            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Region not added to queue due to proximity", Toast.LENGTH_SHORT).show());
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        Log.e(TAG, "Thread interrupted", e);
                    }
                }).start();
            } else {
                Toast.makeText(MainActivity.this, "Last known location is null", Toast.LENGTH_SHORT).show();
            }
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Log.e("MainActivity", "Map fragment is null");
        }

    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.googleMap = googleMap; // Assign the GoogleMap parameter to the class-level variable

        // Check if permission is not granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            // Request the permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST_CODE);
        } else {
            // Permission is granted, enable the "My Location" layer on the map
            googleMap.setMyLocationEnabled(true);
            // Start receiving location updates
            getLocationInBackground();
        }
    }

    // Method to retrieve location
    private void getLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        LocationListener locationListener = new LocationListener() {

            @Override
            public void onLocationChanged(@NonNull Location location) {
                // Update UI with latitude and longitude
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                String latitudeText = getString(R.string.latitude_text, latitude);
                String longitudeText = getString(R.string.longitude_text, longitude);
                latitudeTextView.setText(latitudeText);
                longitudeTextView.setText(longitudeText);
                
                LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());

                // Check if the initial location has been set
                if (!initialLocationSet) {
                    // Add a marker at the user's current location and move the camera
                    userMarker = googleMap.addMarker(new MarkerOptions().position(userLocation).title("Your Location"));
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f));
                    // Set the flag to true to indicate that the initial location has been set
                    initialLocationSet = true;
                } else if (userMarker != null){
                    // Update the position of the existing marker
                        userMarker.setPosition(userLocation);
                }
            }

            @Override
            public void onProviderEnabled(@NonNull String provider) {}

            @Override
            public void onProviderDisabled(@NonNull String provider) {}

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}
        };

        // Request location updates
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locationListener);
        }
    }
    

    // Method to retrieve location in a background thread
    private void getLocationInBackground() {
        Thread thread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(1000); // Wait for 1 second before getting location
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                runOnUiThread(this::getLocation);
            }
        });
        thread.start();
    }

    // Handle permission request result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, get location
                getLocationInBackground();
            } else {
                // Permission denied, show a toast or handle as needed
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public static String formatTime(long nanoseconds) {
        // Convert nanoseconds to seconds
        long totalSeconds = nanoseconds / 1_000_000_000;

        // Calculate hours
        long hours = totalSeconds / 3600;

        // Calculate remaining seconds after removing hours
        long remainingSeconds = totalSeconds % 3600;

        // Calculate minutes
        long minutes = remainingSeconds / 60;

        // Calculate remaining seconds after removing minutes
        long seconds = remainingSeconds % 60;

        // Build the formatted string
        StringBuilder formattedTime = new StringBuilder();
        if (hours > 0) {
            formattedTime.append(hours).append("h");
        }
        if (minutes > 0 || hours > 0) {
            formattedTime.append(String.format("%02d", minutes)).append("m");
        }
        formattedTime.append(String.format("%02d", seconds)).append("s");

        return formattedTime.toString();
    }

}
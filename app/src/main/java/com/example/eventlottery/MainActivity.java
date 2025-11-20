package com.example.eventlottery;


import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

// Fragment imports
import com.example.eventlottery.fragments.BrowseFragment;
import com.example.eventlottery.fragments.EventHistoryFragment;
import com.example.eventlottery.fragments.NotificationFragment;
import com.example.eventlottery.fragments.OrganizerDashboardFragment;
import com.example.eventlottery.fragments.ProfileFragment;

/**
 * Holds the global top bar and bottom nav bar
 */
public class MainActivity extends AppCompatActivity {


    private ImageView profileIcon, notificationIcon;

    private TextView navBrowse;
    private TextView navMyEvents;
    private TextView navScan;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestLocationPermission();

        profileIcon = findViewById(R.id.profileIcon);
        notificationIcon = findViewById(R.id.notificationIcon);


        navBrowse = findViewById(R.id.nav_browse);
        navMyEvents = findViewById(R.id.nav_my_events);
        navScan = findViewById(R.id.nav_scan);

        setupBottomNavigation();
        setupTopBar();


        // Default fragment loading (common to both)
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new BrowseFragment())
                    .commit();
        }
    }
    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void setupTopBar() {
        profileIcon.setOnClickListener(v -> {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new ProfileFragment())
                    .addToBackStack(null)
                    .commit();
        });
        notificationIcon.setOnClickListener(v -> {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new NotificationFragment())
                    .addToBackStack(null)
                    .commit();
        });
    }


    private void setupBottomNavigation() {
        navBrowse.setOnClickListener(v -> {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new BrowseFragment())
                    .commit();
        });

        navMyEvents.setOnClickListener(v -> {
            // Check user role from SharedPreferences
            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            String userRole = prefs.getString("userRole", "entrant");

            // Load appropriate fragment based on role
            if ("organizer".equals(userRole)) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, new OrganizerDashboardFragment())
                        .commit();
            } else {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, new EventHistoryFragment())
                        .commit();
            }
        });
        navScan.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, QrScannerActivity.class);
            startActivity(intent);
        });


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Location permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Location permission denied - some features may be limited",
                        Toast.LENGTH_LONG).show();
            }
        }


    }
}




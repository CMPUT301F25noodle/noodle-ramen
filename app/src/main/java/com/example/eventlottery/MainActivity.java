package com.example.eventlottery;

// Imports from both branches
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
// Common androidx imports
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

// Fragment imports
import com.example.eventlottery.fragments.CreateEventFragment;
import com.example.eventlottery.fragments.BrowseFragment;
import com.example.eventlottery.fragments.EventHistoryFragment;
import com.example.eventlottery.fragments.OrganizerDashboardFragment;
import com.example.eventlottery.fragments.ProfileFragment;

public class MainActivity extends AppCompatActivity {
    private ImageView profileIcon, notificationIcon;

    private TextView navBrowse;
     private TextView navMyEvents;
     private TextView navScan;

    // New variable from feature branch



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

    private void setupTopBar() {
        profileIcon.setOnClickListener(v-> {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new ProfileFragment())
                    .addToBackStack(null)
                    .commit();
        });
        notificationIcon.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, NotificationActivity.class);
            startActivity(intent);
        });
    }

    private void setupBottomNavigation() {
        navBrowse.setOnClickListener(v-> {
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
            // TODO: Implement camera/QR scanning functionality
            // This will be implemented later
            android.widget.Toast.makeText(this, "Scan feature coming soon", android.widget.Toast.LENGTH_SHORT).show();
        });


    }


}
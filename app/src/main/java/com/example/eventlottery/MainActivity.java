package com.example.eventlottery;

// Imports from both branches
import android.content.Intent; // From feature branch
import android.os.Bundle;
import android.widget.ImageView; // From feature branch
import android.widget.TextView; // From dev branch (commented out)

// Common androidx imports
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

// Fragment imports
import com.example.eventlottery.fragments.CreateEventFragment; // From dev branch (unused)
import com.example.eventlottery.fragments.BrowseFragment;

public class MainActivity extends AppCompatActivity {

    // Variables from dev branch (commented out)
    //private TextView navBrowse;
    // private TextView navMyEvents;
    // private TextView navScan;

    // New variable from feature branch
    private ImageView profileIcon;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Commented-out code from dev branch
        // navBrowse = findViewById(R.id.nav_browse);
        //  navMyEvents = findViewById(R.id.nav_my_events);
        // navScan = findViewById(R.id.nav_scan);


        // Default fragment loading (common to both)
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new BrowseFragment())
                    .commit();
        }
    }
}
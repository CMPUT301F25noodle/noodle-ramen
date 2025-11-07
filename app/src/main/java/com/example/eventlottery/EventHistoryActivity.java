package com.example.eventlottery;

import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eventlottery.fragments.BrowseFragment;
import com.example.eventlottery.fragments.EventHistoryFragment;
import com.google.android.material.button.MaterialButtonToggleGroup;
/**
 * EventHistoryActivity - Handles event history page
 * Created by: Jana
 * Handles the tabs that show events that the user registered, won, lost, and is pending in.
 */

public class EventHistoryActivity extends AppCompatActivity {
    private Button myEventsButton, browseButton, scanButton;

    // This is the corrected onCreate method
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.event_history_page);
        myEventsButton = findViewById(R.id.my_events_bottom_nav_button);
        browseButton = findViewById(R.id.browse_bottom_nav_button);
        scanButton = findViewById(R.id.scan_bottom_nav_button);

        // Load EventHistoryFragment by default when the activity starts
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new EventHistoryFragment())
                    .commit();
        }

        browseButton.setOnClickListener(v -> {
            // Switch to the BrowseFragment
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new BrowseFragment())
                    .commit();
        });

        myEventsButton.setOnClickListener(v -> {
            // Switch back to the EventHistoryFragment
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new EventHistoryFragment())
                    .commit();
        });

        // scanButton.setOnClickListener(v -> { /* TODO: Implement scan functionality */ });
    }
}

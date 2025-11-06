package com.example.eventlottery;

import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eventlottery.fragments.BrowseFragment;
import com.example.eventlottery.fragments.EventHistoryFragment;
import com.google.android.material.button.MaterialButtonToggleGroup;

public class EventHistoryActivity extends AppCompatActivity {
    /*
    *put the browse, scan, and my events fragment here
    *
    */
    private Button myEventsButton, browseButton, scanButton, toggleButtons;

    @Override
    protected void onCreate(Bundle savedInstanceState, Object findviewbyID) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.event_history_page);
        myEventsButton = findViewById(R.id.my_events_bottom_nav_button);
        browseButton = findViewById(R.id.browse_bottom_nav_button);

        scanButton = findViewById(R.id.scan_bottom_nav_button);
        //myEventsButton.setOnClickListener(v -> {//shows events registered by user}

        browseButton.setOnClickListener(v -> {
            //shows events registered by user
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new BrowseFragment())
                    .commit();
        });
        //scanButton.setOnClickListener(v -> {}


    }

}

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


    // This is the corrected onCreate method
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.event_history_page);

        // Load EventHistoryFragment by default when the activity starts
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new EventHistoryFragment())
                    .commit();
        }



        // scanButton.setOnClickListener(v -> { /* TODO: Implement scan functionality */ });
    }
}

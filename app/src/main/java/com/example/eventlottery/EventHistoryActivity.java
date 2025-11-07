package com.example.eventlottery;

import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eventlottery.fragments.BrowseFragment;
import com.example.eventlottery.fragments.EventHistoryFragment;
import com.google.android.material.button.MaterialButtonToggleGroup;

public class EventHistoryActivity extends AppCompatActivity {

    /**
     * acts as container for displayers events from the user event history
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     *
     */
    /*
     *put the browse, scan, and my events fragment here
     *
     */


    // This is the corrected onCreate method

    /**
     * sets the content viw
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     *
     */
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

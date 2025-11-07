package com.example.eventlottery.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventlottery.R;
import com.example.eventlottery.event_classes.EventAdapter;
import com.example.eventlottery.event_classes.EventViewModel;

/**
 * EventHistoryFragment - Handles event history page
 * Created by: Jana
 *Handles the tabs that show events that the user registered, won, lost, and is pending in.
 */
public class EventHistoryFragment extends Fragment implements EventAdapter.OnEventClickListener {
    private Button pendingButton;
    private Button wonButton;
    private Button lostButton;
    private Button registeredButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.event_history_page, container, false);
        pendingButton = view.findViewById(R.id.pending_button);
        wonButton = view.findViewById(R.id.won_button);
        lostButton = view.findViewById(R.id.lost_button);
        registeredButton = view.findViewById(R.id.registered_button);

        // Set up click listeners for buttons
        setupClickListeners();

        return view;
    }

    private void setupClickListeners() {
        // Set up click listeners for buttons
        pendingButton.setOnClickListener(v -> {
            // Handle pending button click
        });

        wonButton.setOnClickListener(v -> {
            // Handle won button click
        });

        lostButton.setOnClickListener(v -> {
            // Handle lost button click
        });

        registeredButton.setOnClickListener(v -> {
            // Handle registered button click
        });
    } // The closing brace for setupClickListeners() was missing here.

    @Override
    public void onJoinWaitlistClick(EventViewModel event) { // Added 'event' as the variable name
        // Handle join waitlist click
        // TODO: Implement join waitlist logic using the 'event' object
    }

    @Override
    public void onEventPageClick(EventViewModel event) { // Added 'event' as the variable name
        // Handle event page click
        // TODO: Navigate to event detail page using the 'event' object
    }
}

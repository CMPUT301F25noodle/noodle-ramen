package com.example.eventlottery.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.eventlottery.Event;
import com.example.eventlottery.EventAdapter;
import com.example.eventlottery.R;
import java.util.ArrayList;
import java.util.List;

public class BrowseFragment extends Fragment implements EventAdapter.OnEventClickListener {

    private RecyclerView recyclerView;
    private EventAdapter eventAdapter;
    private EditText searchEditText;
    private Button allEventsButton;
    private ImageView filterIcon;
    private ImageView notificationIcon;
    private ImageView profileIcon;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_browse, container, false);

        // Initialize views and Setup Recycler View
        initViews(view);
        setupRecyclerView();

        // Load sample events (replace with real data later)
        loadSampleEvents();

        // Set up click listeners
        setupClickListeners();

        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.eventsRecyclerView);
        searchEditText = view.findViewById(R.id.searchEditText);
        allEventsButton = view.findViewById(R.id.allEventsButton);
        filterIcon = view.findViewById(R.id.filterIcon);
        notificationIcon = view.findViewById(R.id.notificationIcon);
        profileIcon = view.findViewById(R.id.profileIcon);
    }

    private void setupRecyclerView() {
        // Set layout manager
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);

        // Initialize adapter with empty list
        eventAdapter = new EventAdapter(new ArrayList<>(), this);
        recyclerView.setAdapter(eventAdapter);
    }

    private void loadSampleEvents() {
        // Create sample events (replace with database/API calls later)
        List<Event> sampleEvents = new ArrayList<>();

        sampleEvents.add(new Event(
                "1",
                "Event 1",
                "Organization Name",
                "Southgate Mall",
                "10/15/2025",
                "10/21/2025",
                "",
                25,
                45,
                15,
                65.0,
                "Closed",
                true
        ));

        sampleEvents.add(new Event(
                "2",
                "Event 3",
                "Organization Name",
                "West Edmonton Mall",
                "10/15/2025",
                "10/21/2025",
                "",
                100,
                100,
                60,
                0.0,
                "2 days left",
                false
        ));

        sampleEvents.add(new Event(
                "3",
                "Event 4",
                "Organization Name",
                "Lendrum Place",
                "10/15/2025",
                "10/21/2025",
                "",
                105,
                150,
                75,
                15.0,
                "3 days left",
                false
        ));

        // Update adapter with sample data
        eventAdapter.updateEvents(sampleEvents);
    }

    private void setupClickListeners() {
        // Search functionality
        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            String query = searchEditText.getText().toString();
            performSearch(query);
            return true;
        });

        // Filter button
        allEventsButton.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Filter: All Events", Toast.LENGTH_SHORT).show();
            // TODO: Implement filter logic
        });

        // Filter icon
        filterIcon.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Open filters", Toast.LENGTH_SHORT).show();
            // TODO: Open filter dialog/bottom sheet
        });

        // Notification icon
        notificationIcon.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Notifications", Toast.LENGTH_SHORT).show();
            // TODO: Navigate to notifications
        });

        // Profile icon
        profileIcon.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Profile", Toast.LENGTH_SHORT).show();
            // TODO: Navigate to profile
        });
    }

    private void performSearch(String query) {
        Toast.makeText(getContext(), "Searching for: " + query, Toast.LENGTH_SHORT).show();
        // TODO: Implement search functionality
        // Filter events based on query
    }

    // EventAdapter.OnEventClickListener implementation
    @Override
    public void onJoinWaitlistClick(Event event) {
        Toast.makeText(getContext(), "Joining waitlist for: " + event.getTitle(),
                Toast.LENGTH_SHORT).show();
        // TODO: Implement join waitlist logic
        // Update event status
        // Refresh RecyclerView
    }

    @Override
    public void onEventPageClick(Event event) {
        Toast.makeText(getContext(), "Opening: " + event.getTitle(),
                Toast.LENGTH_SHORT).show();
        // TODO: Navigate to event detail page
        // Pass event ID to detail fragment/activity
    }
}
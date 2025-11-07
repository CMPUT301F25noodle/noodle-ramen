package com.example.eventlottery.fragments;

import android.content.Intent;
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

import com.example.eventlottery.NotificationActivity;
import com.example.eventlottery.event_classes.Event;
import com.example.eventlottery.event_classes.EventAdapter;
import com.example.eventlottery.event_classes.EventViewModel;
import com.example.eventlottery.event_classes.EventDates;
import com.example.eventlottery.event_classes.EventStatus;
import com.example.eventlottery.event_classes.Location;
import com.example.eventlottery.event_classes.Money;
import com.example.eventlottery.event_classes.Waitlist;
import com.example.eventlottery.R;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.example.eventlottery.managers.WaitlistManager;
import com.example.eventlottery.ProfileActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;



public class BrowseFragment extends Fragment implements EventAdapter.OnEventClickListener {

    private RecyclerView recyclerView;
    private EventAdapter eventAdapter;
    private EditText searchEditText;
    private Button allEventsButton;
    private ImageView filterIcon;
    private WaitlistManager waitlistManager;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String currentUserId;
    private List<EventViewModel> currentEventViewModels = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_browse, container, false);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        waitlistManager = WaitlistManager.getInstance();

        if (auth.getCurrentUser() != null) {
            currentUserId = auth.getCurrentUser().getUid();
        }

        // Initialize views and Setup Recycler View
        initViews(view);
        setupRecyclerView();

        // Load events from Firestore
        loadEventsFromFirebase();

        // Set up click listeners
        setupClickListeners();

        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.eventsRecyclerView);
        searchEditText = view.findViewById(R.id.searchEditText);
        allEventsButton = view.findViewById(R.id.allEventsButton);
        filterIcon = view.findViewById(R.id.filterIcon);
    }

    private void setupRecyclerView() {
        // Set layout manager
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);

        // Initialize adapter with empty list
        eventAdapter = new EventAdapter(new ArrayList<>(), this);
        recyclerView.setAdapter(eventAdapter);
    }

    /**
     * Loads events from Firestore and displays them.
     */
    private void loadEventsFromFirebase() {
        db.collection("events")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<EventViewModel> eventViewModels = new ArrayList<>();

                    queryDocumentSnapshots.forEach(document -> {
                        try {
                            // Extract fields from Firestore document
                            String id = document.getId();
                            String eventName = document.getString("eventName");
                            String organizer = document.getString("organizer");
                            String description = document.getString("description");
                            String eligibility = document.getString("eligibility");
                            String locationStr = document.getString("location");
                            String startDate = document.getString("startDate");
                            String endDate = document.getString("endDate");
                            String priceStr = document.getString("price");
                            String waitlistLimitStr = document.getString("waitlistLimit");
                            String entrantMaxStr = document.getString("entrantMaxCapacity");
                            Boolean geolocationRequired = document.getBoolean("geolocationRequired");

                            // Convert to proper types with defaults
                            double price = 0.0;
                            if (priceStr != null && !priceStr.isEmpty()) {
                                price = Double.parseDouble(priceStr);
                            }

                            int waitlistLimit = 0;
                            if (waitlistLimitStr != null && !waitlistLimitStr.isEmpty()) {
                                waitlistLimit = Integer.parseInt(waitlistLimitStr);
                            }

                            int entrantMax = 0;
                            if (entrantMaxStr != null && !entrantMaxStr.isEmpty()) {
                                entrantMax = Integer.parseInt(entrantMaxStr);
                            }

                            // Create Event object
                            Event event = new Event(
                                    id,
                                    eventName != null ? eventName : "Untitled Event",
                                    organizer != null ? organizer : "Unknown Organizer",
                                    description != null ? description : "",
                                    eligibility != null ? eligibility : "",
                                    new Location(locationStr != null ? locationStr : "TBD"),
                                    new EventDates(
                                            startDate != null ? startDate : "",
                                            endDate != null ? endDate : ""
                                    ),
                                    "", // imageUrl - empty for now
                                    new Waitlist(0, waitlistLimit, entrantMax), // currentCount starts at 0
                                    new Money(price),
                                    EventStatus.OPEN, // All events are OPEN by default for MVP
                                    geolocationRequired != null ? geolocationRequired : false
                            );

                            // TODO: Check if user is actually on waitlist for this event
                            EventViewModel viewModel = new EventViewModel(event, false);
                            eventViewModels.add(viewModel);

                        } catch (Exception e) {
                            // exception to log

                        }
                    });

                    // Update UI with loaded events
                    currentEventViewModels = eventViewModels;
                    eventAdapter.updateEvents(eventViewModels);

                    if (eventViewModels.isEmpty()) {
                        Toast.makeText(getContext(), "No events found. Long press profile icon to seed data.",
                                Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to load events: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
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
            // TODO: Implement filter logic
        });

        // Filter icon
        filterIcon.setOnClickListener(v -> {
            // TODO: Open filter dialog/bottom sheet
        });

    }

    private void performSearch(String query) {
        // TODO: Implement search functionality
        // Filter events based on query
    }

    // EventAdapter.OnEventClickListener implementation
    @Override
    public void onJoinWaitlistClick(EventViewModel eventViewModel) {
        if (currentUserId == null) {
            Toast.makeText(getContext(), "Please log in to join waitlist",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if user is already on waitlist
        if (eventViewModel.isUserOnWaitlist()) {
            // User wants to LEAVE waitlist
            leaveWaitlist(eventViewModel);
        } else {
            // User wants to JOIN waitlist
            joinWaitlist(eventViewModel);
        }
    }

    @Override
    public void onEventPageClick(EventViewModel eventViewModel) {
        // TODO: Navigate to event detail page
        // Pass event ID to detail fragment/activity
    }

    /**
     * join event wailist
     *
     * @param eventViewModel
     */

    private void joinWaitlist(EventViewModel eventViewModel) {
        String eventId = eventViewModel.getId();

        waitlistManager.joinWaitlist(eventId, new WaitlistManager.WaitlistCallback() {
            @Override
            public void onSuccess() {
                // Create new ViewModel with updated waitlist status
                EventViewModel updatedViewModel = eventViewModel.withWaitlistStatus(true);

                // Update the list
                updateEventViewModel(eventId, updatedViewModel);

                Toast.makeText(getContext(),
                        "Successfully joined waitlist for " + eventViewModel.getTitle(),
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(getContext(),
                        "Failed to join waitlist: " + error,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * leave event wait list
     * @param eventViewModel
     */
    private void leaveWaitlist(EventViewModel eventViewModel) {
        String eventId = eventViewModel.getId();

        waitlistManager.leaveWaitlist(eventId, new WaitlistManager.WaitlistCallback() {
            @Override
            public void onSuccess() {
                // Create new ViewModel with updated waitlist status
                EventViewModel updatedViewModel = eventViewModel.withWaitlistStatus(false);

                // Update the list
                updateEventViewModel(eventId, updatedViewModel);

                Toast.makeText(getContext(),
                        "Successfully left waitlist for " + eventViewModel.getTitle(),
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(getContext(),
                        "Failed to leave waitlist: " + error,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateEventViewModel(String eventId, EventViewModel updatedViewModel) {
        List<EventViewModel> updatedList = new ArrayList<>();

        for (EventViewModel vm : currentEventViewModels) {
            if (vm.getId().equals(eventId)) {
                updatedList.add(updatedViewModel);
            } else {
                updatedList.add(vm);
            }
        }

        currentEventViewModels = updatedList;
        eventAdapter.updateEvents(updatedList);
    }




}
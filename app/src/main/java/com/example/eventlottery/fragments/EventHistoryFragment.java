package com.example.eventlottery.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventlottery.EventDetailActivity;
import com.example.eventlottery.R;
import com.example.eventlottery.adapters.HistoryEventAdapter;
import com.example.eventlottery.models.HistoryEventViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * EventHistoryFragment - Displays user's event history in three tabs
 * PENDING: Events user is waitlisted for or has pending lottery selection
 * WON: Events user won and accepted
 * LOST: Events user lost or declined
 */
public class EventHistoryFragment extends Fragment {
    private static final String TAG = "EventHistoryFragment";

    private MaterialButtonToggleGroup toggleGroup;
    private MaterialButton pendingButton, wonButton, lostButton;
    private RecyclerView eventsRecyclerView;
    private TextView emptyStateText;
    private ProgressBar progressBar;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String userId;

    private HistoryEventAdapter adapter;
    private List<HistoryEventViewModel> eventsList;

    private enum Tab { PENDING, WON, LOST }
    private Tab currentTab = Tab.PENDING;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.event_history_page, container, false);

        initializeViews(view);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        setupRecyclerView();
        setupClickListeners();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getUserIdAndLoadEvents();
    }

    private void initializeViews(View view) {
        toggleGroup = view.findViewById(R.id.toggleButton_group);
        pendingButton = view.findViewById(R.id.pending_button);
        wonButton = view.findViewById(R.id.won_button);
        lostButton = view.findViewById(R.id.lost_button);
        eventsRecyclerView = view.findViewById(R.id.events_recycler_view);
        emptyStateText = view.findViewById(R.id.empty_state_text);
        progressBar = view.findViewById(R.id.progress_bar);
    }

    private void setupRecyclerView() {
        eventsList = new ArrayList<>();
        adapter = new HistoryEventAdapter(eventsList, event -> {
            // Click on event card - navigate to detail
            Intent intent = new Intent(getContext(), EventDetailActivity.class);
            intent.putExtra("eventId", event.getEventId());
            startActivity(intent);
        });
        eventsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        eventsRecyclerView.setAdapter(adapter);
    }

    private void setupClickListeners() {
        pendingButton.setOnClickListener(v -> {
            currentTab = Tab.PENDING;
            loadEventsForCurrentTab();
        });

        wonButton.setOnClickListener(v -> {
            currentTab = Tab.WON;
            loadEventsForCurrentTab();
        });

        lostButton.setOnClickListener(v -> {
            currentTab = Tab.LOST;
            loadEventsForCurrentTab();
        });

        // Set pending as default selected
        toggleGroup.check(R.id.pending_button);
    }

    private void getUserIdAndLoadEvents() {
        if (auth.getCurrentUser() != null) {
            userId = auth.getCurrentUser().getUid();
            Log.d(TAG, "Using FirebaseAuth userId: " + userId);
            loadEventsForCurrentTab();
        } else {
            Log.e(TAG, "No authenticated user found");
            Toast.makeText(getContext(), "Please sign in to view event history", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadEventsForCurrentTab() {
        if (userId == null) {
            Log.e(TAG, "Cannot load events: userId is null");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        eventsRecyclerView.setVisibility(View.GONE);
        emptyStateText.setVisibility(View.GONE);

        Log.d(TAG, "Loading events for tab: " + currentTab);

        switch (currentTab) {
            case PENDING:
                loadPendingEvents();
                break;
            case WON:
                loadWonEvents();
                break;
            case LOST:
                loadLostEvents();
                break;
        }
    }

    /**
     * PENDING: User in waitlistUsers OR in selected with status "pending"
     */
    private void loadPendingEvents() {
        Log.d(TAG, "Loading pending events for userId: " + userId);
        eventsList.clear();

        // Query ALL events and check if user is in waitlistUsers or selected
        db.collection("events")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Found " + queryDocumentSnapshots.size() + " total events");

                    for (DocumentSnapshot eventDoc : queryDocumentSnapshots) {
                        @SuppressWarnings("unchecked")
                        List<String> waitlistUsers = (List<String>) eventDoc.get("waitlistUsers");
                        @SuppressWarnings("unchecked")
                        Map<String, Object> selected = (Map<String, Object>) eventDoc.get("selected");

                        boolean isPending = false;

                        // Check if user is on waitlist
                        if (waitlistUsers != null && waitlistUsers.contains(userId)) {
                            isPending = true;
                            Log.d(TAG, "User is on waitlist for: " + eventDoc.getId());
                        }
                        // Check if user has pending selection
                        else if (selected != null && selected.containsKey(userId)) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> userSelection = (Map<String, Object>) selected.get(userId);
                            String status = userSelection != null ? (String) userSelection.get("status") : null;
                            if ("pending".equals(status)) {
                                isPending = true;
                                Log.d(TAG, "User has pending selection for: " + eventDoc.getId());
                            }
                        }

                        if (isPending) {
                            HistoryEventViewModel event = documentToEvent(eventDoc);
                            if (event != null) {
                                eventsList.add(event);
                            }
                        }
                    }

                    Log.d(TAG, "Found " + eventsList.size() + " pending events");
                    adapter.notifyDataSetChanged();
                    updateUIState();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading pending events", e);
                    showEmptyState("Error loading events");
                });
    }

    /**
     * WON: User in selected with status "accepted"
     */
    private void loadWonEvents() {
        Log.d(TAG, "Loading won events for userId: " + userId);
        eventsList.clear();

        db.collection("events")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot eventDoc : queryDocumentSnapshots) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> selected = (Map<String, Object>) eventDoc.get("selected");

                        if (selected != null && selected.containsKey(userId)) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> userSelection = (Map<String, Object>) selected.get(userId);
                            String status = userSelection != null ? (String) userSelection.get("status") : null;

                            if ("accepted".equals(status)) {
                                HistoryEventViewModel event = documentToEvent(eventDoc);
                                if (event != null) {
                                    eventsList.add(event);
                                    Log.d(TAG, "User won event: " + eventDoc.getId());
                                }
                            }
                        }
                    }

                    Log.d(TAG, "Found " + eventsList.size() + " won events");
                    adapter.notifyDataSetChanged();
                    updateUIState();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading won events", e);
                    showEmptyState("Error loading events");
                });
    }

    /**
     * LOST: User in selected with status "declined"
     */
    private void loadLostEvents() {
        Log.d(TAG, "Loading lost events for userId: " + userId);
        eventsList.clear();

        db.collection("events")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot eventDoc : queryDocumentSnapshots) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> selected = (Map<String, Object>) eventDoc.get("selected");

                        if (selected != null && selected.containsKey(userId)) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> userSelection = (Map<String, Object>) selected.get(userId);
                            String status = userSelection != null ? (String) userSelection.get("status") : null;

                            if ("declined".equals(status)) {
                                HistoryEventViewModel event = documentToEvent(eventDoc);
                                if (event != null) {
                                    eventsList.add(event);
                                    Log.d(TAG, "User lost event: " + eventDoc.getId());
                                }
                            }
                        }
                    }

                    Log.d(TAG, "Found " + eventsList.size() + " lost events");
                    adapter.notifyDataSetChanged();
                    updateUIState();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading lost events", e);
                    showEmptyState("Error loading events");
                });
    }

    private HistoryEventViewModel documentToEvent(DocumentSnapshot doc) {
        try {
            HistoryEventViewModel event = new HistoryEventViewModel();
            event.setEventId(doc.getId());
            event.setEventName(doc.getString("eventName"));
            event.setLocation(doc.getString("location"));
            event.setStartDate(doc.getString("startDate"));
            event.setEndDate(doc.getString("endDate"));
            event.setPrice(doc.getString("price"));

            return event;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing event document", e);
            return null;
        }
    }

    private void updateUIState() {
        progressBar.setVisibility(View.GONE);

        if (eventsList.isEmpty()) {
            eventsRecyclerView.setVisibility(View.GONE);
            emptyStateText.setVisibility(View.VISIBLE);
            emptyStateText.setText(getEmptyMessage());
        } else {
            eventsRecyclerView.setVisibility(View.VISIBLE);
            emptyStateText.setVisibility(View.GONE);
        }
    }

    private void showEmptyState(String message) {
        progressBar.setVisibility(View.GONE);
        eventsRecyclerView.setVisibility(View.GONE);
        emptyStateText.setVisibility(View.VISIBLE);
        emptyStateText.setText(message);
    }

    private String getEmptyMessage() {
        switch (currentTab) {
            case PENDING:
                return "No pending events";
            case WON:
                return "No won events yet";
            case LOST:
                return "No lost events";
            default:
                return "No events found";
        }
    }
}

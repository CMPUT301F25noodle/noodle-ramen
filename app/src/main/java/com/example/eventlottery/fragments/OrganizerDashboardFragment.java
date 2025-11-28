package com.example.eventlottery.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.eventlottery.R;
import com.example.eventlottery.QrGenerator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * OrganizerDashboardFragment - handles organizer dashboard and event management
 * created by: ibrahim
 */

public class OrganizerDashboardFragment extends Fragment {

    // ui elements
    private LinearLayout btnCreateEvent;
    private Button tabRegistered, tabWon, tabLost, tabPending;
    private LinearLayout myEventsContainer, eventHistoryContainer;
    private android.widget.TextView emptyMyEventsText, emptyEventHistoryText;

    // firebase
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentUserId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_organizer_dashboard, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() != null) {
            currentUserId = mAuth.getCurrentUser().getUid();
        }

        initializeViews(view);
        setupListeners();
        loadMyEvents();
        loadEventHistory("pending");

        return view;
    }

    /**
     * connects xml elements with java
     */
    private void initializeViews(View view) {
        btnCreateEvent = view.findViewById(R.id.btn_create_event);

        tabRegistered = view.findViewById(R.id.tab_registered);
        tabWon = view.findViewById(R.id.tab_won);
        tabLost = view.findViewById(R.id.tab_lost);
        tabPending = view.findViewById(R.id.tab_pending);

        myEventsContainer = view.findViewById(R.id.my_events_container);
        eventHistoryContainer = view.findViewById(R.id.event_history_container);

        // Create empty state text views
        emptyMyEventsText = new android.widget.TextView(getContext());
        emptyMyEventsText.setText("No events created yet");
        emptyMyEventsText.setTextColor(0xFF666666);
        emptyMyEventsText.setPadding(16, 32, 16, 32);
        emptyMyEventsText.setGravity(android.view.Gravity.CENTER);

        emptyEventHistoryText = new android.widget.TextView(getContext());
        emptyEventHistoryText.setText("No events found");
        emptyEventHistoryText.setTextColor(0xFF666666);
        emptyEventHistoryText.setPadding(16, 32, 16, 32);
        emptyEventHistoryText.setGravity(android.view.Gravity.CENTER);
    }

    /**
     * sets up button actions
     */
    private void setupListeners() {
        if (btnCreateEvent != null) {
            btnCreateEvent.setOnClickListener(v -> {
                if (getActivity() != null) {
                    getActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragmentContainer, new CreateEventFragment())
                            .commit();
                }
            });
        }

        tabRegistered.setOnClickListener(v -> {
            updateTabSelection(tabRegistered);
            loadEventHistory("registered");
        });

        tabWon.setOnClickListener(v -> {
            updateTabSelection(tabWon);
            loadEventHistory("won");
        });

        tabLost.setOnClickListener(v -> {
            updateTabSelection(tabLost);
            loadEventHistory("lost");
        });

        tabPending.setOnClickListener(v -> {
            updateTabSelection(tabPending);
            loadEventHistory("pending");
        });
    }

    /**
     * updates tab button styles
     */
    private void updateTabSelection(Button selectedTab) {
        tabRegistered.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), android.R.color.darker_gray));
        tabRegistered.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));

        tabWon.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), android.R.color.darker_gray));
        tabWon.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));

        tabLost.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), android.R.color.darker_gray));
        tabLost.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));

        tabPending.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), android.R.color.darker_gray));
        tabPending.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));

        selectedTab.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), android.R.color.holo_purple));
        selectedTab.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
    }

    /**
     * loads organizer's created events from firestore
     */
    private void loadMyEvents() {
        if (currentUserId == null) {
            myEventsContainer.removeAllViews();
            myEventsContainer.addView(emptyMyEventsText);
            return;
        }

        db.collection("events")
                .whereEqualTo("organizer", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    myEventsContainer.removeAllViews();

                    if (queryDocumentSnapshots.isEmpty()) {
                        myEventsContainer.addView(emptyMyEventsText);
                        return;
                    }

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String eventId = document.getId();
                        String eventName = document.getString("eventName");
                        String startDate = document.getString("startDate");
                        String endDate = document.getString("endDate");

                        // Get waitlist count
                        Long waitlistCountLong = document.getLong("waitlistCount");
                        int waitlistCount = waitlistCountLong != null ? waitlistCountLong.intValue() : 0;

                        addEventCard(eventId, eventName, startDate, endDate, waitlistCount);
                    }
                })
                .addOnFailureListener(e -> {
                    myEventsContainer.removeAllViews();
                    myEventsContainer.addView(emptyMyEventsText);
                    Toast.makeText(getContext(), "Failed to load events", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * adds an event card to the my events container
     */
    private void addEventCard(String eventId, String eventName, String startDate, String endDate, int waitlistCount) {
        View eventCard = LayoutInflater.from(getContext()).inflate(R.layout.item_organizer_event_card, myEventsContainer, false);

        // Find views in the card
        android.widget.TextView tvEventTitle = eventCard.findViewById(R.id.tv_event_title);
        android.widget.TextView tvStatusBadge = eventCard.findViewById(R.id.tv_status_badge);
        android.widget.TextView tvDateRange = eventCard.findViewById(R.id.tv_date_range);
        android.widget.TextView tvWaitlistCount = eventCard.findViewById(R.id.tv_waitlist_count);
        Button btnManage = eventCard.findViewById(R.id.btn_manage);
        Button btnEdit = eventCard.findViewById(R.id.btn_edit);

        // Set event title
        if (tvEventTitle != null) {
            tvEventTitle.setText(eventName != null ? eventName : "Untitled Event");
        }

        // Set date range
        if (tvDateRange != null) {
            String dateRangeText = formatDateRange(startDate, endDate);
            tvDateRange.setText(dateRangeText);
        }

        // Calculate and set days until event
        if (tvStatusBadge != null) {
            String daysText = calculateDaysUntilEvent(startDate);
            tvStatusBadge.setText(daysText);
        }

        // Set waitlist count
        if (tvWaitlistCount != null) {
            tvWaitlistCount.setText(waitlistCount + " on waitlist");
        }

        // Set up Manage button
        if (btnManage != null) {
            btnManage.setOnClickListener(v -> {
                if (getActivity() != null) {
                    EventManagementFragment fragment = EventManagementFragment.newInstance(eventId);
                    getActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragmentContainer, fragment)
                            .addToBackStack(null)
                            .commit();
                }
            });
        }

        // Set up Edit button
        if (btnEdit != null) {
            btnEdit.setOnClickListener(v -> {
                if (getActivity() != null) {
                    CreateEventFragment fragment = new CreateEventFragment();
                    Bundle args = new Bundle();
                    args.putString("eventId", eventId);
                    fragment.setArguments(args);

                    getActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragmentContainer, fragment)
                            .addToBackStack(null)
                            .commit();
                }
            });
        }

        myEventsContainer.addView(eventCard);
    }

    /**
     * Formats date range for display
     */
    private String formatDateRange(String startDate, String endDate) {
        if (startDate == null || startDate.isEmpty()) {
            return "Date TBD";
        }
        if (endDate == null || endDate.isEmpty() || startDate.equals(endDate)) {
            return startDate;
        }
        return startDate + " - " + endDate;
    }

    /**
     * Calculates days until event and returns formatted string
     */
    private String calculateDaysUntilEvent(String startDate) {
        if (startDate == null || startDate.isEmpty()) {
            return "Date TBD";
        }

        try {
            // Use d/M/yyyy format to match the DatePicker format (day/month/year without zero-padding)
            SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy", Locale.US);
            Date eventDate = sdf.parse(startDate);
            Date today = new Date();

            if (eventDate != null) {
                long diffInMillis = eventDate.getTime() - today.getTime();
                long daysUntil = TimeUnit.MILLISECONDS.toDays(diffInMillis);

                if (daysUntil < 0) {
                    return "Event passed";
                } else if (daysUntil == 0) {
                    return "Today";
                } else if (daysUntil == 1) {
                    return "1 day left";
                } else {
                    return daysUntil + " days left";
                }
            }
        } catch (ParseException e) {
            return "Invalid date";
        }

        return "Date error";
    }

    /**
     * loads event history based on selected tab
     */
    private void loadEventHistory(String category) {
        eventHistoryContainer.removeAllViews();

        if (currentUserId == null) {
            emptyEventHistoryText.setText("Please log in to view event history");
            eventHistoryContainer.addView(emptyEventHistoryText);
            return;
        }

        db.collection("users")
                .document(currentUserId)
                .collection("eventHistory")
                .whereEqualTo("status", category)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    eventHistoryContainer.removeAllViews();

                    if (queryDocumentSnapshots.isEmpty()) {
                        emptyEventHistoryText.setText("No " + category + " events found");
                        eventHistoryContainer.addView(emptyEventHistoryText);
                        return;
                    }

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String eventId = document.getString("eventId");
                        String eventName = document.getString("eventName");

                        // TODO: Add event cards for history
                        // For now just showing a toast
                        Toast.makeText(getContext(), "loaded " + category + " event: " + eventName, Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    eventHistoryContainer.removeAllViews();
                    emptyEventHistoryText.setText("Failed to load events");
                    eventHistoryContainer.addView(emptyEventHistoryText);
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
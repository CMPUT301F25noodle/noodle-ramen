package com.example.eventlottery.fragments;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.eventlottery.EventDetailActivity;
import com.example.eventlottery.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

/**
 * AdminEventsFragment provides an interface for administrators to manage events.
 * It allows viewing a list of all events, searching/filtering them, viewing details, and deleting events.
 */

public class AdminEventsFragment extends Fragment {

    private TextView eventsCount;
    private EditText searchEvents;
    private LinearLayout eventsList;
    private ProgressBar loadingSpinner;
    private TextView emptyMessage;
    private FirebaseFirestore db;
    private ListenerRegistration eventsListener;

    private final List<EventData> allEvents = new ArrayList<>();
    private final List<EventData> filteredEvents = new ArrayList<>();
    /**
     * Initializes the fragment's UI components and sets up listeners for data loading and user interaction.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container          If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return The View for the fragment's UI.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_events, container, false);

        db = FirebaseFirestore.getInstance();

        eventsCount = view.findViewById(R.id.eventsCount);
        searchEvents = view.findViewById(R.id.searchEvents);
        eventsList = view.findViewById(R.id.eventsList);
        loadingSpinner = view.findViewById(R.id.loadingSpinner);
        emptyMessage = view.findViewById(R.id.emptyMessage);

        setupSearchListener();
        loadEventsFromFirestore();

        return view;
    }
    /**
     * Sets up a TextWatcher on the search bar to filter the event list as the user types.
     */
    private void setupSearchListener() {
        searchEvents.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterEvents(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }
    /**
     * Connects to Firestore to listen for real-time updates to the "events" collection.
     * When data changes, it updates the local list and refreshes the display.
     */
    private void loadEventsFromFirestore() {

        showLoading(true);


        eventsListener = db.collection("events")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        showLoading(false);
                        showError("Failed to load events: " + error.getMessage());
                        return;
                    }

                    if (value != null) {
                        allEvents.clear();

                        for (QueryDocumentSnapshot document : value) {
                            String eventId = document.getId();
                            String eventName = document.getString("eventName");
                            String organizerName = document.getString("organizerName");
                            Long participantsCount = document.getLong("participantsCount");

                            EventData event = new EventData(
                                    eventId,
                                    eventName != null ? eventName : "Untitled Event",
                                    organizerName != null ? organizerName : "Unknown",
                                    participantsCount != null ? participantsCount.intValue() : 0
                            );

                            allEvents.add(event);
                        }


                        eventsCount.setText(String.valueOf(allEvents.size()));


                        filterEvents(searchEvents.getText().toString());

                        showLoading(false);
                    }
                });
    }
    /**
     * Filters the list of events based on the search query.
     * Matches against event name or organizer name.
     *
     * @param query The search string entered by the user.
     */
    private void filterEvents(String query) {
        filteredEvents.clear();

        if (query.isEmpty()) {
            filteredEvents.addAll(allEvents);
        } else {
            String lowerQuery = query.toLowerCase();
            for (EventData event : allEvents) {
                if (event.eventName.toLowerCase().contains(lowerQuery) ||
                        event.organizerName.toLowerCase().contains(lowerQuery)) {
                    filteredEvents.add(event);
                }
            }
        }

        displayEvents();
    }
    /**
     * Clears the current list view and repopulates it with cards for the filtered events.
     */
    private void displayEvents() {

        eventsList.removeAllViews();


        if (filteredEvents.isEmpty()) {
            showEmptyMessage();
            return;
        }

        for (EventData event : filteredEvents) {
            addEventCard(event);
        }
    }
    /**
     * Inflates a new event card view, populates it with data, and adds it to the list layout.
     * Sets up click listeners for the "View Details" and "Delete" buttons.
     *
     * @param event The EventData object containing details to display.
     */
    @SuppressLint("SetTextI18n")
    private void addEventCard(EventData event) {

        View eventCard = LayoutInflater.from(getContext()).inflate(R.layout.item_admin_event_card, eventsList, false);

        TextView eventNameText = eventCard.findViewById(R.id.eventName);
        TextView organizerText = eventCard.findViewById(R.id.organizerName);
        TextView participantsText = eventCard.findViewById(R.id.participantsCount);
        Button viewDetailsBtn = eventCard.findViewById(R.id.viewDetailsBtn);
        Button deleteBtn = eventCard.findViewById(R.id.deleteEventBtn);


        eventNameText.setText(event.eventName);
        organizerText.setText("By " + event.organizerName);
        participantsText.setText("Participants: " + event.participantsCount);


        viewDetailsBtn.setOnClickListener(v -> viewEventDetails(event.eventId, event.eventName));


        deleteBtn.setOnClickListener(v -> deleteEvent(event.eventId, event.eventName));


        eventsList.addView(eventCard);
    }
    /**
     * Navigates to the EventDetailActivity to show more information about the selected event.
     *
     * @param eventId   The ID of the event to view.
     * @param eventName The name of the event (passed for context).
     */
    private void viewEventDetails(String eventId, String eventName) {

        Intent intent = new Intent(getContext(), EventDetailActivity.class);
        intent.putExtra("eventId", eventId);
        intent.putExtra("eventName", eventName);
        startActivity(intent);

    }
    /**
     * Shows a confirmation dialog to delete an event. If confirmed, deletes the event from Firestore.
     *
     * @param eventId   The ID of the event to delete.
     * @param eventName The name of the event (used in the confirmation message).
     */

    private void deleteEvent(String eventId, String eventName) {

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Delete Event")
                .setMessage("Are you sure you want to delete '" + eventName + "'?\n\nThis action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {

                    Toast.makeText(getContext(), "Deleting event...", Toast.LENGTH_SHORT).show();

                    db.collection("events").document(eventId)
                            .delete()
                            .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Event deleted successfully", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to delete event: " + e.getMessage(), Toast.LENGTH_LONG).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    /**
     * Toggles the visibility of the loading spinner and the event list.
     *
     * @param show True to show the loading spinner, false to show the list.
     */
    private void showLoading(boolean show) {
        if (loadingSpinner != null) {
            loadingSpinner.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        eventsList.setVisibility(show ? View.GONE : View.VISIBLE);
    }
    /**
     * Displays a message indicating that no events match the current search or exist in the database.
     */
    private void showEmptyMessage() {
        if (emptyMessage != null) {
            emptyMessage.setVisibility(View.VISIBLE);
            emptyMessage.setText(searchEvents.getText().toString().isEmpty()
                    ? "No events found in database"
                    : "No events match your search");
        } else {

            TextView emptyText = new TextView(getContext());
            emptyText.setText(searchEvents.getText().toString().isEmpty()
                    ? "No events found in database"
                    : "No events match your search");
            emptyText.setTextSize(16);
            emptyText.setTextColor(0xFF999999);
            emptyText.setPadding(16, 32, 16, 32);
            emptyText.setGravity(android.view.Gravity.CENTER);
            eventsList.addView(emptyText);
        }
    }
    /**
     * Displays a toast message with an error description.
     *
     * @param message The error message to display.
     */
    private void showError(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
    }
    /**
     * Cleans up resources when the fragment view is destroyed, specifically removing the Firestore listener.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (eventsListener != null) {
            eventsListener.remove();
        }
    }


    private static class EventData {
        String eventId;
        String eventName;
        String organizerName;
        int participantsCount;

        EventData(String eventId, String eventName, String organizerName, int participantsCount) {
            this.eventId = eventId;
            this.eventName = eventName;
            this.organizerName = organizerName;
            this.participantsCount = participantsCount;
        }
    }
}
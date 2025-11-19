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

/**
 * OrganizerDashboardFragment - handles organizer dashboard and event management
 * created by: ibrahim
 */

public class OrganizerDashboardFragment extends Fragment {

    // ui elements
    private LinearLayout btnCreateEvent;
    private Button tabRegistered, tabWon, tabLost, tabPending;
    private LinearLayout myEventsContainer, eventHistoryContainer;

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
            Toast.makeText(getContext(), "please log in to view your events", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("events")
                .whereEqualTo("organizer", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    myEventsContainer.removeAllViews();

                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(getContext(), "no events created yet", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String eventName = document.getString("eventName");
                        String location = document.getString("location");
                        String eventId = document.getId();

                        addEventCard(eventId, eventName, location);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "failed to load events: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /**
     * adds an event card to the my events container
     */
    private void addEventCard(String eventId, String eventName, String location) {
        View eventCard = LayoutInflater.from(getContext()).inflate(R.layout.item_organizer_event_card, myEventsContainer, false);

        // find textviews in the card and set the event data
        android.widget.TextView tvEventTitle = eventCard.findViewById(R.id.tv_event_title);
        android.widget.TextView tvWaitlistCount = eventCard.findViewById(R.id.tv_waitlist_count);

        if (tvEventTitle != null) {
            tvEventTitle.setText(eventName);
        }
        if (tvWaitlistCount != null) {
            tvWaitlistCount.setText(location);
        }

        Button  btnManage = eventCard.findViewById(R.id.btn_manage);
        Button btnEdit = eventCard.findViewById(R.id.btn_edit);

        // button for the manage to go do the draw
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
    // button to edit the event
        if (btnEdit != null) {
            btnEdit.setOnClickListener(v -> {
                if (getActivity() != null) {
                    // Create CreateEventFragment and pass the event ID for editing
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






        // add click listener to view event details
        eventCard.setOnClickListener(v -> {
            Toast.makeText(getContext(), "clicked on " + eventName, Toast.LENGTH_SHORT).show();
        });

        myEventsContainer.addView(eventCard);
    }

    /**
     * loads event history based on selected tab
     */
    private void loadEventHistory(String category) {
        if (currentUserId == null) {
            Toast.makeText(getContext(), "please log in to view event history", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users")
                .document(currentUserId)
                .collection("eventHistory")
                .whereEqualTo("status", category)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(getContext(), "no " + category + " events found", Toast.LENGTH_SHORT).show();
                        eventHistoryContainer.removeAllViews();
                        return;
                    }

                    eventHistoryContainer.removeAllViews();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String eventId = document.getString("eventId");
                        String eventName = document.getString("eventName");

                        Toast.makeText(getContext(), "loaded " + category + " event: " + eventName, Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "failed to load event history: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
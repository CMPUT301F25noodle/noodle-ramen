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
    private ImageView ivNotifications, ivProfile;
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
        setupBottomNavigation();
        loadMyEvents();
        loadEventHistory("pending");

        return view;
    }

    /**
     * connects xml elements with java
     */
    private void initializeViews(View view) {
        btnCreateEvent = view.findViewById(R.id.btn_create_event);
        ivNotifications = view.findViewById(R.id.iv_notifications);
        ivProfile = view.findViewById(R.id.iv_profile);

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
        btnCreateEvent.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new CreateEventFragment())
                    .commit();
        });

        ivNotifications.setOnClickListener(v -> {
            Toast.makeText(getContext(), "notifications page not linked yet", Toast.LENGTH_SHORT).show();
        });

        ivProfile.setOnClickListener(v -> {
            Toast.makeText(getContext(), "profile page not linked yet", Toast.LENGTH_SHORT).show();
        });

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
     * sets up bottom navigation from mainactivity
     */
    private void setupBottomNavigation() {
        if (getActivity() != null) {
            View rootView = getActivity().findViewById(android.R.id.content);
            LinearLayout mainLayout = (LinearLayout) rootView.getRootView().findViewById(R.id.fragmentContainer).getParent();

            if (mainLayout != null && mainLayout.getChildCount() > 1) {
                LinearLayout navBar = (LinearLayout) mainLayout.getChildAt(1);

                View navBrowse = navBar.getChildAt(0);
                View navMyEvents = navBar.getChildAt(1);
                View navScan = navBar.getChildAt(2);

                navBrowse.setOnClickListener(v -> {
                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.fragmentContainer, new BrowseFragment())
                            .commit();
                });

                navMyEvents.setOnClickListener(v -> {
                    Toast.makeText(getContext(), "already on my events page", Toast.LENGTH_SHORT).show();
                });

                navScan.setOnClickListener(v -> {
                    Intent intent = new Intent(getActivity(), QrGenerator.class);
                    startActivity(intent);
                });
            }
        }
    }

    /**
     * updates tab button styles
     */
    private void updateTabSelection(Button selectedTab) {
        tabRegistered.setBackgroundTintList(getResources().getColorStateList(android.R.color.darker_gray));
        tabRegistered.setTextColor(getResources().getColor(android.R.color.white));

        tabWon.setBackgroundTintList(getResources().getColorStateList(android.R.color.darker_gray));
        tabWon.setTextColor(getResources().getColor(android.R.color.white));

        tabLost.setBackgroundTintList(getResources().getColorStateList(android.R.color.darker_gray));
        tabLost.setTextColor(getResources().getColor(android.R.color.white));

        tabPending.setBackgroundTintList(getResources().getColorStateList(android.R.color.darker_gray));
        tabPending.setTextColor(getResources().getColor(android.R.color.white));

        selectedTab.setBackgroundTintList(getResources().getColorStateList(android.R.color.holo_purple));
        selectedTab.setTextColor(getResources().getColor(android.R.color.white));
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
                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(getContext(), "no events created yet", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    myEventsContainer.removeAllViews();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String eventName = document.getString("eventName");
                        String location = document.getString("location");
                        String eventId = document.getId();

                        Toast.makeText(getContext(), "loaded event: " + eventName, Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "failed to load events: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
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


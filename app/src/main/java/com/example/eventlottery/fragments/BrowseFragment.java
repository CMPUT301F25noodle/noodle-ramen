package com.example.eventlottery.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
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
import android.Manifest;
import android.content.pm.PackageManager;
import android.util.Log;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.GeoPoint;

import com.example.eventlottery.EventDetailActivity;
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
import java.util.List;

import com.example.eventlottery.managers.WaitlistManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Fragment for browse functionaltiy, shows on main page
 * ALlows user to scroll through event cards and select the event they want to join
 */


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
    private List<EventViewModel> allEventViewModels = new ArrayList<>(); // Store all events for filtering

    private  static final String TAG = "BrowseFragment";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final int SEARCH_DEBOUNCE_DELAY_MS = 300; // 300ms debounce delay

    private FusedLocationProviderClient fusedLocationClient;
    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    private EventViewModel pendingJoinEvent = null;



    /**
     * fragment instantiates the interface
     * @param inflater The LayoutInflater object that can be used to inflate
     * any views in the fragment,
     * @param container If non-null, this is the parent view that the fragment's
     * UI should be attached to.  The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     *
     * @return
     */
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


        initViews(view);
        setupRecyclerView();


        loadEventsFromFirebase();


        setupClickListeners();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());


        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Remove any pending search callbacks to prevent memory leaks
        if (searchHandler != null && searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
    }

    /**
     * intilazes the UI componenets
     * @param view
     */

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.events_recycler_view);
        searchEditText = view.findViewById(R.id.search_edit_text);
        allEventsButton = view.findViewById(R.id.filter_status_badge);
        filterIcon = view.findViewById(R.id.fitler_button);
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
                            // Changed from organizer to organizationName which was causing
                            // The display to show organizerId and not the organizationName
                            String organizationName = document.getString("organizerName");
                            String description = document.getString("description");
                            String eligibility = document.getString("eligibility");
                            String locationStr = document.getString("location");
                            String startDate = document.getString("startDate");
                            String endDate = document.getString("endDate");
                            String priceStr = document.getString("price");
                            String waitlistLimitStr = document.getString("waitlistLimit");
                            String entrantMaxStr = document.getString("entrantMaxCapacity");
                            Boolean geolocationRequired = document.getBoolean("geolocationRequired");

                            // Check if current user is on the waitlist for this event
                            boolean isUserOnWaitlist = false;
                            Object waitlistUsersObj = document.get("waitlistUsers");
                            if (currentUserId != null && waitlistUsersObj instanceof java.util.List) {
                                isUserOnWaitlist = ((java.util.List<?>) waitlistUsersObj).contains(currentUserId);
                            }

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

                            // Get actual waitlist count from Firestore
                            Long waitlistCountLong = document.getLong("waitlistCount");
                            int waitlistCount = waitlistCountLong != null ? waitlistCountLong.intValue() : 0;

                            // Create Event object
                            Event event = new Event(
                                    id,
                                    eventName != null ? eventName : "Untitled Event",
                                    organizationName != null ? organizationName : "Unknown Organizer",
                                    description != null ? description : "",
                                    eligibility != null ? eligibility : "",
                                    new Location(locationStr != null ? locationStr : "TBD"),
                                    new EventDates(
                                            startDate != null ? startDate : "",
                                            endDate != null ? endDate : ""
                                    ),
                                    "", // imageUrl - empty for now
                                    new Waitlist(waitlistCount, waitlistLimit, entrantMax),
                                    new Money(price),
                                    EventStatus.OPEN, // All events are OPEN by default for MVP
                                    geolocationRequired != null ? geolocationRequired : false
                            );

                            // Create EventViewModel with actual waitlist status
                            EventViewModel viewModel = new EventViewModel(event, isUserOnWaitlist);
                            eventViewModels.add(viewModel);

                        } catch (Exception e) {
                            // exception to log

                        }
                    });

                    // Update UI with loaded events
                    allEventViewModels = new ArrayList<>(eventViewModels); // Store all events
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

    /**
     * sets up click listeners for the search bar and filter buttons
     */
    private void setupClickListeners() {
        // Search functionality with debouncing
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Remove any pending search callbacks
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }

                // Create a new runnable for the search
                searchRunnable = () -> performSearch(s.toString());

                // Post the search with a delay (debounce)
                searchHandler.postDelayed(searchRunnable, SEARCH_DEBOUNCE_DELAY_MS);
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not needed
            }
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

    /**
     * Performs search/filtering on events based on the search query.
     * Filters events by event name (case-insensitive).
     * If query is empty, shows all events.
     *
     * @param query the search query string
     */
    private void performSearch(String query) {
        // If query is empty or null, show all events
        if (query == null || query.trim().isEmpty()) {
            currentEventViewModels = new ArrayList<>(allEventViewModels);
            eventAdapter.updateEvents(currentEventViewModels);
            return;
        }

        // Filter events by title (case-insensitive)
        String lowerCaseQuery = query.toLowerCase().trim();
        List<EventViewModel> filteredEvents = new ArrayList<>();

        for (EventViewModel event : allEventViewModels) {
            if (event.getTitle().toLowerCase().contains(lowerCaseQuery)) {
                filteredEvents.add(event);
            }
        }

        // Update the current list and adapter
        currentEventViewModels = filteredEvents;
        eventAdapter.updateEvents(filteredEvents);
    }

    // EventAdapter.OnEventClickListener implementation

    /**
     * handles the click on joinwaitlist and leave waitlist functionaltiy
     * @param eventViewModel the event that was clicked
     */
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

    /**
     * hanldes the clicking on the main part of the event card body
     * @param eventViewModel the event that was clicked
     */
    @Override
    public void onEventPageClick(EventViewModel eventViewModel) {
        Intent intent = new Intent(getActivity(), EventDetailActivity.class);
        intent.putExtra("eventId", eventViewModel.getId());
        startActivity(intent);
    }

    /**
     * join event wailist for specified event
     *
     * @param eventViewModel
     */

    /**
     * join event waitlist for specified event
     *
     * @param eventViewModel
     */
    private void joinWaitlist(EventViewModel eventViewModel) {
        String eventId = eventViewModel.getId();

        // Check if geolocation is required
        if (eventViewModel.isGeolocationRequired()) {
            // Check if we already have permission
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                // Store the event for later and request permission
                pendingJoinEvent = eventViewModel;
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_PERMISSION_REQUEST_CODE);
            } else {
                // Permission already granted, get location and join
                joinWithLocation(eventViewModel);
            }
        } else {
            // Geolocation NOT required, join without it
            performJoinWaitlist(eventViewModel, null, null);
        }
    }

    /**
     * Join waitlist with location tracking
     */
    private void joinWithLocation(EventViewModel eventViewModel) {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        GeoPoint geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());

                        // Get user name for location tracking
                        db.collection("users").document(currentUserId)
                                .get()
                                .addOnSuccessListener(userDoc -> {
                                    String userName = userDoc.exists() ? userDoc.getString("name") : "Unknown";
                                    performJoinWaitlist(eventViewModel, geoPoint, userName);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error getting user data", e);
                                    performJoinWaitlist(eventViewModel, geoPoint, "Unknown");
                                });
                    } else {
                        Toast.makeText(getContext(), "Unable to determine location. Ensure GPS is on.",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting location", e);
                    Toast.makeText(getContext(), "Error getting location: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Actually perform the join operation with optional location data
     */
    private void performJoinWaitlist(EventViewModel eventViewModel, GeoPoint location, String userName) {
        String eventId = eventViewModel.getId();

        waitlistManager.joinWaitlist(eventId, location, userName, new WaitlistManager.WaitlistCallback() {
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
    /**
     * Handle the result of the permission request
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed to join with location
                if (pendingJoinEvent != null) {
                    joinWithLocation(pendingJoinEvent);
                    pendingJoinEvent = null;
                }
            } else {
                // Permission denied
                Toast.makeText(getContext(), "Location permission is required to join this event.",
                        Toast.LENGTH_LONG).show();
                pendingJoinEvent = null;
            }
        }
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
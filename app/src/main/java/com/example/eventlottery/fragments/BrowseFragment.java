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
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.firebase.firestore.GeoPoint;

import com.example.eventlottery.EventDetailActivity;
import com.example.eventlottery.EventFilter;
import com.example.eventlottery.FilterDialogFragment;
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
import com.example.eventlottery.managers.ImageManager;
import com.example.eventlottery.models.Image;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.concurrent.atomic.AtomicInteger;

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
    private EventFilter currentFilter = new EventFilter(); // Current filter criteria

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
    public void onResume() {
        super.onResume();
        // Reload events when fragment becomes visible to catch newly created events
        loadEventsFromFirebase();
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
                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(getContext(), "No events found.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    List<EventViewModel> eventViewModels = new ArrayList<>();

                    // Loop through all documents
                    queryDocumentSnapshots.forEach(document -> {
                        try {
                            // 1. Extract fields (Same logic as before)
                            String id = document.getId();
                            String eventName = document.getString("eventName");
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
                            String category = document.getString("category");

                            // 2. Check Waitlist Status
                            boolean isUserOnWaitlist = false;
                            Object waitlistUsersObj = document.get("waitlistUsers");
                            if (currentUserId != null && waitlistUsersObj instanceof java.util.List) {
                                isUserOnWaitlist = ((java.util.List<?>) waitlistUsersObj).contains(currentUserId);
                            }

                            // 3. Parse Numbers
                            double price = (priceStr != null && !priceStr.isEmpty()) ? Double.parseDouble(priceStr) : 0.0;
                            int waitlistLimit = (waitlistLimitStr != null && !waitlistLimitStr.isEmpty()) ? Integer.parseInt(waitlistLimitStr) : 0;
                            int entrantMax = (entrantMaxStr != null && !entrantMaxStr.isEmpty()) ? Integer.parseInt(entrantMaxStr) : 0;
                            Long waitlistCountLong = document.getLong("waitlistCount");
                            int waitlistCount = waitlistCountLong != null ? waitlistCountLong.intValue() : 0;

                            // 4. Create Event Object
                            Event event = new Event(
                                    id,
                                    eventName != null ? eventName : "Untitled Event",
                                    organizationName != null ? organizationName : "Unknown Organizer",
                                    description != null ? description : "",
                                    eligibility != null ? eligibility : "",
                                    new Location(locationStr != null ? locationStr : "TBD"),
                                    new EventDates(startDate != null ? startDate : "", endDate != null ? endDate : ""),
                                    "", // Image URL is empty/unused here
                                    new Waitlist(waitlistCount, waitlistLimit, entrantMax),
                                    new Money(price),
                                    EventStatus.OPEN,
                                    geolocationRequired != null ? geolocationRequired : false,
                                    category != null ? category : "Other"
                            );

                            // 5. Create ViewModel without waiting for image
                            // Pass 'null' for image data implicitly by using this constructor
                            EventViewModel viewModel = new EventViewModel(event, isUserOnWaitlist);

                            eventViewModels.add(viewModel);

                        } catch (Exception e) {
                            Log.e(TAG, "Error processing event document", e);
                        }
                    });

                    // 6. Update the list immediately after parsing is done
                    updateEventsList(eventViewModels);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to load events: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
    /**
     * Updates the events list and adapter with loaded events
     */
    private void updateEventsList(List<EventViewModel> eventViewModels) {
        allEventViewModels = new ArrayList<>(eventViewModels);
        currentEventViewModels = eventViewModels;
        eventAdapter.updateEvents(eventViewModels);
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
            openFilterDialog();
        });

        // Filter icon
        filterIcon.setOnClickListener(v -> {
            openFilterDialog();
        });

    }

    /**
     * Performs search/filtering on events based on the search query.
     * Works in combination with other filters.
     *
     * @param query the search query string
     */
    private void performSearch(String query) {
        // Use the combined filter and search method
        applyFiltersAndSearch();
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
                requestPermissions(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION},
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

        // Set loading state
        updateEventViewModel(eventViewModel.getId(), eventViewModel.withLoadingState(true));

        // Try to get last location first (faster)
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        // We have a recent location, use it
                        processLocationAndJoin(eventViewModel, location);
                    } else {
                        // No cached location, request current location
                        requestCurrentLocation(eventViewModel);
                    }
                })
                .addOnFailureListener(e -> {
                    // Failed to get last location, try current location
                    Log.w(TAG, "Failed to get last location, requesting current location", e);
                    requestCurrentLocation(eventViewModel);
                });
    }

    /**
     * Request current location actively (not from cache)
     */
    private void requestCurrentLocation(EventViewModel eventViewModel) {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();

        fusedLocationClient.getCurrentLocation(
                LocationRequest.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.getToken())
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        processLocationAndJoin(eventViewModel, location);
                    } else {
                        // Clear loading state
                        updateEventViewModel(eventViewModel.getId(), eventViewModel.withLoadingState(false));
                        Toast.makeText(getContext(),
                                "Unable to get location. Please ensure GPS is enabled and try again.",
                                Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    // Clear loading state
                    updateEventViewModel(eventViewModel.getId(), eventViewModel.withLoadingState(false));
                    Log.e(TAG, "Error getting current location", e);
                    Toast.makeText(getContext(),
                            "Error getting location: " + e.getMessage() + ". Please ensure GPS is enabled.",
                            Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Process the location and join the waitlist
     */
    private void processLocationAndJoin(EventViewModel eventViewModel, android.location.Location location) {
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
    }

    /**
     * Actually perform the join operation with optional location data
     */
    private void performJoinWaitlist(EventViewModel eventViewModel, GeoPoint location, String userName) {
        String eventId = eventViewModel.getId();

        waitlistManager.joinWaitlist(eventId, location, userName, new WaitlistManager.WaitlistCallback() {
            @Override
            public void onSuccess() {
                // Create new ViewModel with updated waitlist status and clear loading state
                EventViewModel updatedViewModel = eventViewModel.withWaitlistStatus(true).withLoadingState(false);

                // Update the list
                updateEventViewModel(eventId, updatedViewModel);
            }

            @Override
            public void onFailure(String error) {
                // Clear loading state on failure
                updateEventViewModel(eventId, eventViewModel.withLoadingState(false));

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

    /**
     * Opens the filter dialog
     */
    private void openFilterDialog() {
        FilterDialogFragment filterDialog = FilterDialogFragment.newInstance(currentFilter);
        filterDialog.setFilterAppliedListener(filter -> {
            currentFilter = filter;
            applyFiltersAndSearch();
        });
        filterDialog.show(getParentFragmentManager(), "FilterDialog");
    }

    /**
     * Applies both search query and filters to the event list
     */
    private void applyFiltersAndSearch() {
        String searchQuery = searchEditText != null ? searchEditText.getText().toString() : "";

        // Start with all events
        List<EventViewModel> filteredEvents = new ArrayList<>(allEventViewModels);

        // Apply search filter first
        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            String lowerCaseQuery = searchQuery.toLowerCase().trim();
            List<EventViewModel> searchFiltered = new ArrayList<>();
            for (EventViewModel event : filteredEvents) {
                if (event.getTitle().toLowerCase().contains(lowerCaseQuery)) {
                    searchFiltered.add(event);
                }
            }
            filteredEvents = searchFiltered;
        }

        // Apply category filter
        if (currentFilter.getActivityType() != null) {
            List<EventViewModel> categoryFiltered = new ArrayList<>();
            for (EventViewModel eventVM : filteredEvents) {
                if (eventVM.getCategory() != null &&
                    eventVM.getCategory().equals(currentFilter.getActivityType())) {
                    categoryFiltered.add(eventVM);
                }
            }
            filteredEvents = categoryFiltered;
        }

        // Apply price filter
        if (currentFilter.getMinPrice() != null || currentFilter.getMaxPrice() != null) {
            List<EventViewModel> priceFiltered = new ArrayList<>();
            for (EventViewModel eventVM : filteredEvents) {
                double price = eventVM.getPrice();
                boolean matchesMin = currentFilter.getMinPrice() == null || price >= currentFilter.getMinPrice();
                boolean matchesMax = currentFilter.getMaxPrice() == null || price <= currentFilter.getMaxPrice();
                if (matchesMin && matchesMax) {
                    priceFiltered.add(eventVM);
                }
            }
            filteredEvents = priceFiltered;
        }

        // Apply date filter
        if (currentFilter.getStartDate() != null || currentFilter.getEndDate() != null) {
            List<EventViewModel> dateFiltered = new ArrayList<>();
            for (EventViewModel eventVM : filteredEvents) {
                String eventStartDate = eventVM.getStartDate();
                String eventEndDate = eventVM.getEndDate();

                boolean matchesStartDate = currentFilter.getStartDate() == null ||
                    (eventStartDate != null && eventStartDate.compareTo(currentFilter.getStartDate()) >= 0);
                boolean matchesEndDate = currentFilter.getEndDate() == null ||
                    (eventEndDate != null && eventEndDate.compareTo(currentFilter.getEndDate()) <= 0);

                if (matchesStartDate && matchesEndDate) {
                    dateFiltered.add(eventVM);
                }
            }
            filteredEvents = dateFiltered;
        }

        // Apply location filter
        if (currentFilter.getLocation() != null && !currentFilter.getLocation().trim().isEmpty()) {
            String locationQuery = currentFilter.getLocation().toLowerCase().trim();
            List<EventViewModel> locationFiltered = new ArrayList<>();
            for (EventViewModel eventVM : filteredEvents) {
                if (eventVM.getLocationText().toLowerCase().contains(locationQuery)) {
                    locationFiltered.add(eventVM);
                }
            }
            filteredEvents = locationFiltered;
        }

        // Update the current list and adapter
        currentEventViewModels = filteredEvents;
        eventAdapter.updateEvents(filteredEvents);
    }



}
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

public class BrowseFragment extends Fragment implements EventAdapter.OnEventClickListener {

    private RecyclerView recyclerView;
    private EventAdapter eventAdapter;
    private EditText searchEditText;
    private Button allEventsButton;
    private ImageView filterIcon;
    private ImageView notificationIcon;
    private ImageView profileIcon;

    private WaitlistManager waitlistManager;
    private FirebaseAuth auth;
    private String currentUserId;
    private List<EventViewModel> currentEventViewModels = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_browse, container, false);

        auth = FirebaseAuth.getInstance();
        waitlistManager = WaitlistManager.getInstance();


        if (auth.getCurrentUser() != null) {
            currentUserId = auth.getCurrentUser().getUid();
        }

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
        // Create sample events using modern approach with value objects
        List<EventViewModel> sampleEventViewModels = new ArrayList<>();

        // Event 1 - User is on waitlist
        Event event1 = new Event(
                "1",
                "Event 1",
                "Organization Name",
                new Location("Southgate Mall"),
                new EventDates("10/15/2025", "10/21/2025"),
                "",
                new Waitlist(25, 45, 15),
                new Money(65.0),
                EventStatus.CLOSED
        );
        sampleEventViewModels.add(new EventViewModel(event1, true, status));

        // Event 2 - User is NOT on waitlist
        Event event2 = new Event(
                "2",
                "Event 3",
                "Organization Name",
                new Location("West Edmonton Mall"),
                new EventDates("10/15/2025", "10/21/2025"),
                "",
                new Waitlist(100, 100, 60),
                new Money(0.0),
                EventStatus.ENDING_SOON
        );
        sampleEventViewModels.add(new EventViewModel(event2, false, status));

        // Event 3 - User is NOT on waitlist
        Event event3 = new Event(
                "3",
                "Event 4",
                "Organization Name",
                new Location("Lendrum Place"),
                new EventDates("10/15/2025", "10/21/2025"),
                "",
                new Waitlist(105, 150, 75),
                new Money(15.0),
                new EventStatus("3 days left")
        );
        sampleEventViewModels.add(new EventViewModel(event3, false, status));

        currentEventViewModels = sampleEventViewModels;

        // Update adapter with sample data
        eventAdapter.updateEvents(sampleEventViewModels);
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

        // Notification icon
        notificationIcon.setOnClickListener(v -> {
            // TODO: Go to notifications
        });

        // Profile icon
        profileIcon.setOnClickListener(v -> {
            // TODO: Go to profile
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
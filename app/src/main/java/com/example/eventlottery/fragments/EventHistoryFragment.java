package com.example.eventlottery.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventlottery.R;
import com.example.eventlottery.event_classes.EventAdapter;
import com.example.eventlottery.event_classes.EventViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * EventHistoryFragment - Handles event history page
 * Created by: Jana
 *Handles the tabs that show events that the user registered, won, lost, and is pending in.
 */
public class EventHistoryFragment extends Fragment implements EventAdapter.OnEventClickListener {
    private Button pendingButton;
    private Button wonButton;
    private Button lostButton;
    private Button registeredButton;
    private RecyclerView eventRecyclerView;
    private EventAdapter eventAdapter;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.event_history_page, container, false);
        pendingButton = view.findViewById(R.id.pending_button);
        wonButton = view.findViewById(R.id.won_button);
        lostButton = view.findViewById(R.id.lost_button);
        registeredButton = view.findViewById(R.id.registered_button);
        eventRecyclerView = view.findViewById(R.id.eventsRecyclerView);

        setupRecyclerView();

        // Set up click listeners for buttons
        setupClickListeners();

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Load the default event list ("Registered") when the fragment starts
        loadEvents("Registered");
        registeredButton.setSelected(true);
        pendingButton.setSelected(false);
        wonButton.setSelected(false);
        lostButton.setSelected(false);
    }

    private void setupRecyclerView() {
        // 1. Create an initial empty list for the adapter
        List<EventViewModel> initialList = new ArrayList<>();

        // 2. Initialize your EventAdapter (using the constructor you provided)
        // 'this' works because this Fragment implements OnEventClickListener
        eventAdapter = new EventAdapter(initialList, this);

        // 3. Set the layout manager and adapter
        eventRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        eventRecyclerView.setAdapter(eventAdapter);
    }

    private void setupClickListeners() {
        pendingButton.setOnClickListener(v -> {
            loadEvents("Pending");
            pendingButton.setSelected(true);
            wonButton.setSelected(false);
            lostButton.setSelected(false);
            registeredButton.setSelected(false);
        });

        wonButton.setOnClickListener(v -> {
            loadEvents("Won");
            pendingButton.setSelected(false);
            wonButton.setSelected(true);
            lostButton.setSelected(false);
            registeredButton.setSelected(false);
        });

        lostButton.setOnClickListener(v -> {
            loadEvents("Lost");
            pendingButton.setSelected(false);
            wonButton.setSelected(false);
            lostButton.setSelected(true);
            registeredButton.setSelected(false);
        });

        registeredButton.setOnClickListener(v -> {
            loadEvents("Registered");
            pendingButton.setSelected(false);
            wonButton.setSelected(false);
            lostButton.setSelected(false);
            registeredButton.setSelected(true);
        });
    }

    /**
     * Fetches event data based on the filter type and updates the adapter.
     */
    // Inside EventHistoryFragment
    private void loadEvents(String eventType) {
        List<EventViewModel> newEventList = new ArrayList<>();

        if (eventType.equals("Pending")) {
            // The adapter will see "PENDING" and automatically show "Waitlisted" + "Leave" buttons
            newEventList.add(new EventViewModel("Super Concert", "Southgate", "PENDING"));
            newEventList.add(new EventViewModel("Comedy Night", "Downtown", "PENDING"));

        } else if (eventType.equals("Won")) {
            // The adapter will see "WON" and automatically show "Accept" + "Decline" buttons
            newEventList.add(new EventViewModel("Oilers Game", "Rogers Place", "WON"));

        } else if (eventType.equals("Lost")) {
            // The adapter will see "LOST" and show "Opt-in" button
            newEventList.add(new EventViewModel("Folk Fest", "Hill Park", "LOST"));

        } else if (eventType.equals("Registered")) {
            // The adapter sees "REGISTERED" and shows the "Attending" badge
            newEventList.add(new EventViewModel("Career Fair", "UofA", "REGISTERED"));
        }

        eventAdapter.updateEvents(newEventList);
    }

    //Implementation of OnEventClickListener methods
    @Override
    public void onPrimaryActionClick(EventViewModel viewModel) {
        // This handles Join, Accept, Opt-in, etc. based on status
        String status = viewModel.getStatus();
        if ("WON".equals(status)) {
            // Handle accept logic
            System.out.println("Accepted event: " + viewModel.getTitle());
        } else if ("LOST".equals(status)) {
            // Handle opt-in logic
            System.out.println("Opted-in for retry: " + viewModel.getTitle());
        }
        // etc...
    }

    @Override
    public void onSecondaryActionClick(EventViewModel viewModel) {
        // This handles Decline, Leave Waitlist, etc.
        String status = viewModel.getStatus();
        if ("WON".equals(status)) {
            // Handle decline logic
            System.out.println("Declined event: " + viewModel.getTitle());
        } else if ("PENDING".equals(status)) {
            // Handle leave waitlist logic
            System.out.println("Left waitlist for: " + viewModel.getTitle());
        }
    }
    @Override
    public void onEventPageClick(EventViewModel eventViewModel) {
        // Logic to navigate to the event's detail page
        // Example: Intent intent = new Intent(getActivity(), EventDetailActivity.class);
        //intent.putExtra("EVENT_ID", eventViewModel.getId());
        // startActivity(intent);
    }
}

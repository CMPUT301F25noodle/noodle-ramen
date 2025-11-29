package com.example.eventlottery.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.example.eventlottery.R;
import com.google.firebase.firestore.FirebaseFirestore;
/**
 * AdminDashboardFragment displays the main dashboard for administrators.
 * It shows statistics such as the total count of profiles, organizers, events, images, and notifications.
 */


public class AdminDashboardFragment extends Fragment {

    private TextView countProfiles, countOrganizers, countEvents, countImages, countNotifications;
    private FirebaseFirestore db;

    /**
     * Initializes the fragment view, sets up UI components, and triggers the data loading process.
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
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_admin_dashboard, container, false);

        db = FirebaseFirestore.getInstance();

        countProfiles = v.findViewById(R.id.countProfiles);
        countOrganizers = v.findViewById(R.id.countOrganizers);
        countEvents = v.findViewById(R.id.countEvents);
        countImages = v.findViewById(R.id.countImages);
        countNotifications = v.findViewById(R.id.countNotifications);

        loadCountsFromFirestore();

        return v;
    }
    /**
     * Fetches counts for profiles, organizers, events, images, and notifications from Firestore
     * and updates the corresponding TextViews in the UI.
     */


    private void loadCountsFromFirestore() {

        // Count of Profiles
        db.collection("users").get()
                .addOnSuccessListener(query -> {
                    countProfiles.setText(String.valueOf(query.size()));
                });

        // Count of Organizers
        db.collection("users").whereEqualTo("role", "organizer").get()
                .addOnSuccessListener(query -> {
                    countOrganizers.setText(String.valueOf(query.size()));
                });

        // Count of Events
        db.collection("events").get()
                .addOnSuccessListener(query -> {
                    countEvents.setText(String.valueOf(query.size()));
                });

        // Count of Images
        db.collection("images").get()
                .addOnSuccessListener(query -> {
                    countImages.setText(String.valueOf(query.size()));
                });

        // Count of Notifications
        db.collection("notifications").get()
                .addOnSuccessListener(query -> {
                    countNotifications.setText(String.valueOf(query.size()));
                });

    }
}

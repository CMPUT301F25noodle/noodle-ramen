package com.example.eventlottery.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.example.eventlottery.R;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminDashboardFragment extends Fragment {

    private TextView countProfiles, countOrganizers, countEvents, countImages, countNotifications;
    private FirebaseFirestore db;

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

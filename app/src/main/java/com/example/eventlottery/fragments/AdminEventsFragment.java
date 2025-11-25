package com.example.eventlottery.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.eventlottery.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class AdminEventsFragment extends Fragment {

    private TextView eventsCount;
    private LinearLayout eventsList;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_events, container, false);

        db = FirebaseFirestore.getInstance();

        eventsCount = view.findViewById(R.id.eventsCount);
        eventsList = view.findViewById(R.id.eventsList);

        loadEventsFromFirestore();

        return view;
    }

    private void loadEventsFromFirestore() {
        db.collection("events").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    eventsCount.setText(String.valueOf(queryDocumentSnapshots.size()));


                    eventsList.removeAllViews();


                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String eventId = document.getId();
                        String eventName = document.getString("eventName");
                        String organizerName = document.getString("organizerName");
                        Long participantsCount = document.getLong("participantsCount");


                        addEventCard(eventId, eventName, organizerName, participantsCount != null ? participantsCount.intValue() : 0);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to load events: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void addEventCard(String eventId, String eventName, String organizerName, int participantsCount) {

        View eventCard = LayoutInflater.from(getContext()).inflate(R.layout.item_admin_event_card, eventsList, false);

        TextView eventNameText = eventCard.findViewById(R.id.eventName);
        TextView organizerText = eventCard.findViewById(R.id.organizerName);
        TextView participantsText = eventCard.findViewById(R.id.participantsCount);
        Button viewDetailsBtn = eventCard.findViewById(R.id.viewDetailsBtn);
        Button deleteBtn = eventCard.findViewById(R.id.deleteEventBtn);


        eventNameText.setText(eventName != null ? eventName : "Untitled Event");
        organizerText.setText("By " + (organizerName != null ? organizerName : "Unknown"));
        participantsText.setText("Participants: " + participantsCount);


        viewDetailsBtn.setOnClickListener(v -> {
            Toast.makeText(getContext(), "View details for: " + eventName, Toast.LENGTH_SHORT).show();

        });

        deleteBtn.setOnClickListener(v -> {
            deleteEvent(eventId, eventName);
        });

        eventsList.addView(eventCard);
    }

    private void deleteEvent(String eventId, String eventName) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Delete Event")
                .setMessage("Are you sure you want to delete '" + eventName + "'?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.collection("events").document(eventId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(getContext(), "Event deleted successfully", Toast.LENGTH_SHORT).show();
                                loadEventsFromFirestore();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(), "Failed to delete event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
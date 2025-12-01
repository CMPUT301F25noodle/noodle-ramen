package com.example.eventlottery.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.eventlottery.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;



/**
 * AdminLogsFragment displays a log of system notifications for administrators.
 * It listens to the "notifications" collection in Firestore and displays them in a scrollable list,
 * styling each card based on the notification type (e.g., winning, losing, or general info).
 */
public class AdminLogsFragment extends Fragment {

    private TextView notificationsCount;
    private TextView emptyMessage;
    private LinearLayout notificationsList;
    private ProgressBar loadingSpinner;

    private FirebaseFirestore db;
    private final List<LogData> allLogs = new ArrayList<>();
    private static final String TAG = "AdminLogsFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_admin_logs, container, false);

        db = FirebaseFirestore.getInstance();

        notificationsCount = view.findViewById(R.id.notificationsCount);
        emptyMessage = view.findViewById(R.id.emptyMessage);
        notificationsList = view.findViewById(R.id.notificationsList);
        loadingSpinner = view.findViewById(R.id.loadingSpinner);

        loadLogsFromFirestore();

        return view;
    }

    /**
     * Connects to Firestore to listen for real-time updates to the "notifications" collection.
     * Parses the documents into LogData objects and updates the UI.
     */
    // pull notifications from firestore
    private void loadLogsFromFirestore() {
        showLoading(true);
        allLogs.clear();

        // Use collectionGroup to get ALL messages from ALL users at once
        db.collectionGroup("messages")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        showLoading(false);
                        notificationsCount.setText("0");
                        showLogs();
                        return;
                    }

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        try {
                            // Extract Message Data
                            String id = doc.getId();
                            String eventName = doc.getString("eventName");
                            String message = doc.getString("message");
                            String type = doc.getString("type");
                            String organizerName = doc.getString("organizerName");

                            // Extract Recipient ID from the path: notifications/{userId}/messages/{messageId}
                            // parent() = messages, parent().parent() = userId document
                            String recipientId = "Unknown";
                            if (doc.getReference().getParent().getParent() != null) {
                                recipientId = doc.getReference().getParent().getParent().getId();
                            }

                            // Build Log Title
                            String title = getNotificationTitle(type) + " - " + (eventName != null ? eventName : "Event");

                            // Build Subtitle
                            // Note: We display User ID here. Fetching names for every log
                            // individually causes performance issues.
                            String subtitle = "From: " + (organizerName != null ? organizerName : "System") +
                                    " | To ID: " + recipientId;

                            LogData log = new LogData(
                                    id,
                                    title,
                                    subtitle,
                                    message != null ? message : "",
                                    type != null ? type : ""
                            );

                            allLogs.add(log);

                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing log: " + e.getMessage());
                        }
                    }

                    // Update UI
                    notificationsCount.setText(String.valueOf(allLogs.size()));
                    showLogs();
                    showLoading(false);
                })
                .addOnFailureListener(error -> {
                    showLoading(false);
                    Log.e(TAG, "Firestore Error: " + error.getMessage());
                    showError("Failed to load logs. Check Logcat.");
                });
    }

    private String getNotificationTitle(String type) {
        if (type == null) return "Notification";
        switch (type.toLowerCase()) {
            case "win": return "Winner Selected";
            case "loss": return "Not Selected";
            case "replacement": return "Replacement Selected";
            default: return "Notification";
        }
    }

    private void showLogs() {
        notificationsList.removeAllViews();

        if (allLogs.isEmpty()) {
            emptyMessage.setVisibility(View.VISIBLE);
            return;
        }

        emptyMessage.setVisibility(View.GONE);

        for (LogData log : allLogs) {
            addNotificationCard(log);
        }
    }


    /**
     * Inflates and populates a single notification card view.
     * Sets the card background color based on the notification type (win, lose, draw, etc.).
     *
     * @param log The LogData object containing the notification details.
     */
    private void addNotificationCard(LogData log) {
        View card = LayoutInflater.from(getContext())
                .inflate(R.layout.item_admin_log_card, notificationsList, false);

        LinearLayout cardRoot = card.findViewById(R.id.logCardRoot);
        TextView titleText = card.findViewById(R.id.logTitle);
        TextView subtitleText = card.findViewById(R.id.logSubtitle);
        TextView messageText = card.findViewById(R.id.logMessage);

        titleText.setText(log.title);
        subtitleText.setText(log.subtitle);
        messageText.setText(log.message);

        // Styling based on type
        String t = log.type.toLowerCase();
        if (t.contains("win") || t.contains("congrat")) {
            cardRoot.setBackgroundResource(R.drawable.card_congratulations);
        } else if (t.contains("loss") || t.contains("sorry")) {
            cardRoot.setBackgroundResource(R.drawable.card_sorry);
        } else if (t.contains("replace")) {
            cardRoot.setBackgroundResource(R.drawable.card_drawing);
        } else {
            cardRoot.setBackgroundResource(R.drawable.card_bg);
        }

        notificationsList.addView(card);
    }

    private void showLoading(boolean show) {
        if (loadingSpinner != null) {
            loadingSpinner.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (notificationsList != null) {
            notificationsList.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    private void showError(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    // Data Holder
    private static class LogData {
        String id;
        String title;
        String subtitle;
        String message;
        String type;

        LogData(String id, String title, String subtitle, String message, String type) {
            this.id = id;
            this.title = title;
            this.subtitle = subtitle;
            this.message = message;
            this.type = type;
        }
    }
}
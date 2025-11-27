package com.example.eventlottery.fragments;

import android.os.Bundle;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AdminLogsFragment extends Fragment {

    private TextView notificationsCount;
    private TextView emptyMessage;
    private LinearLayout notificationsList;
    private ProgressBar loadingSpinner;

    private FirebaseFirestore db;
    private ListenerRegistration logsListener;

    // list of all notifications from firestore
    private final List<LogData> allLogs = new ArrayList<>();

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

    // pull notifications from firestore
    private void loadLogsFromFirestore() {
        showLoading(true);

        logsListener = db.collection("notifications")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        showLoading(false);
                        showError("failed to load notifications: " + error.getMessage());
                        return;
                    }

                    allLogs.clear();

                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {

                            String id = doc.getId();
                            String title = doc.getString("title");
                            String subtitle = doc.getString("subtitle");
                            String message = doc.getString("message");
                            String type = doc.getString("type"); // "congrats", "sorry", "drawing"

                            LogData log = new LogData(
                                    id,
                                    title != null ? title : "notification",
                                    subtitle != null ? subtitle : "",
                                    message != null ? message : "",
                                    type != null ? type : ""
                            );

                            allLogs.add(log);
                        }
                    }

                    notificationsCount.setText(String.valueOf(allLogs.size()));
                    showLogs();

                    showLoading(false);
                });
    }

    // show all logs in the list
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

    // add one card to the list
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

        // pick card color based on type
        String t = log.type.toLowerCase();
        if (t.contains("congrats") || t.contains("win")) {
            cardRoot.setBackgroundResource(R.drawable.card_congratulations);
        } else if (t.contains("sorry") || t.contains("lose")) {
            cardRoot.setBackgroundResource(R.drawable.card_sorry);
        } else if (t.contains("draw") || t.contains("info")) {
            cardRoot.setBackgroundResource(R.drawable.card_drawing);
        } else {
            cardRoot.setBackgroundResource(R.drawable.card_bg);
        }

        notificationsList.addView(card);
    }

    // simple helper to show and hide loading spinner
    private void showLoading(boolean show) {
        if (loadingSpinner != null) {
            loadingSpinner.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        notificationsList.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void showError(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (logsListener != null) {
            logsListener.remove();
        }
    }

    // simple data holder for one notification
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

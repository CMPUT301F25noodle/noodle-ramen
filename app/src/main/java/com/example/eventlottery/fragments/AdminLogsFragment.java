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
    private ListenerRegistration logsListener;

    // list of all notifications from firestore
    private final List<LogData> allLogs = new ArrayList<>();
    /**
     * Initializes the fragment's UI components and triggers the data loading process.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container          If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return The View for the fragment's UI.
     */
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
    /**
     * Renders the list of notification logs into the LinearLayout container.
     * Displays an empty message if no logs are found.
     */
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
    /**
     * Toggles the visibility of the loading spinner and the notifications list.
     *
     * @param show True to show the loading spinner, false to show the list.
     */
    private void showLoading(boolean show) {
        if (loadingSpinner != null) {
            loadingSpinner.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        notificationsList.setVisibility(show ? View.GONE : View.VISIBLE);
    }
    /**
     * Displays a toast message with an error description.
     *
     * @param message The error message to display.
     */
    private void showError(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
    }
    /**
     * Cleans up resources when the fragment view is destroyed, removing the Firestore listener.
     */
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

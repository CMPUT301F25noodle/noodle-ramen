package com.example.eventlottery.fragments;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventlottery.Notification;
import com.example.eventlottery.NotificationAdapter;
import com.example.eventlottery.R;
import com.example.eventlottery.managers.LotteryManager;
import com.example.eventlottery.managers.NotificationManager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

import static android.content.Context.MODE_PRIVATE;

/**
 * notificaiton fragment for contains the notifications that are sent to user
 * they can access the notifcaitons and enroll in events from there
 * uses lottery manager as logic for win or lose
 */

public class NotificationFragment extends Fragment {
    private static final String TAG = "NotificationsFragment";

    private RecyclerView notificationsRecyclerView;
    private NotificationAdapter adapter;
    private ProgressBar progressBar;
    private TextView emptyStateTextView;

    private FirebaseFirestore db;
    private String userId;

    private LotteryManager lotteryManager;
    private NotificationManager notificationManager;

    private List<Notification> notificationsList;

    /**
     * instanties the fragment into the user interface
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
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.notification_fragment, container, false);
    }

    /**
     *
     * @param view The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

        initializeViews(view);
        setupRecyclerView();
        getUserIdAndLoadNotifications();

        lotteryManager = new LotteryManager();
        notificationManager = new NotificationManager();
    }
/**
 * intializes UI compoenents finding frm fragment
 */
    private void initializeViews(View view) {
        notificationsRecyclerView = view.findViewById(R.id.notifications_recycler_view);
        progressBar = view.findViewById(R.id.progress_bar);
        emptyStateTextView = view.findViewById(R.id.empty_state_text);
    }

    /**
     * configref the recycler viewwith listetning for if accepted or declined
     */
    private void setupRecyclerView() {
        notificationsList = new ArrayList<>();

        adapter = new NotificationAdapter(notificationsList, new NotificationAdapter.OnNotificationClickListener() {
            @Override
            public void onAcceptClicked(Notification notification) {
                handleAcceptClicked(notification);
            }

            @Override
            public void onDeclineClicked(Notification notification) {
                handleDeclineClicked(notification);
            }
        });

        notificationsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        notificationsRecyclerView.setAdapter(adapter);
    }

    /**
     * add a listener to get the notifcaitons taht have been sent to the user
     */
    private void loadNotifications() {
        Log.d(TAG, "Loading notifications for user: " + userId);

        progressBar.setVisibility(View.VISIBLE);
        emptyStateTextView.setVisibility(View.GONE);

        db.collection("notifications")
                .document(userId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((queryDocumentSnapshots, error) -> {
                    progressBar.setVisibility(View.GONE);

                    if (error != null) {
                        Log.e(TAG, "Error loading notifications", error);
                        Toast.makeText(getContext(), "Error loading notifications", Toast.LENGTH_SHORT).show();
                        showEmptyState();
                        return;
                    }

                    if (queryDocumentSnapshots == null || queryDocumentSnapshots.isEmpty()) {
                        Log.d(TAG, "No notifications found");
                        showEmptyState();
                        return;
                    }

                    notificationsList.clear();
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        Notification notification = documentToNotification(document);
                        if (notification != null) {
                            notificationsList.add(notification);
                        }
                    }

                    Log.d(TAG, "Loaded " + notificationsList.size() + " notifications");

                    if (notificationsList.isEmpty()) {
                        showEmptyState();
                    } else {
                        notificationsRecyclerView.setVisibility(View.VISIBLE);
                        emptyStateTextView.setVisibility(View.GONE);
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    /**
     * converts the firestore object into an object that can be shown to user
     * @param document
     * @return
     */
    private Notification documentToNotification(DocumentSnapshot document) {
        try {
            Notification notification = new Notification();
            notification.setId(document.getId());
            notification.setType(document.getString("type"));
            notification.setEventId(document.getString("eventId"));
            notification.setEventName(document.getString("eventName"));
            notification.setMessage(document.getString("message"));
            notification.setTimestamp(document.getLong("timestamp"));
            notification.setRead(document.getBoolean("read"));
            notification.setResponded(document.getBoolean("responded"));
            return notification;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing notification", e);
            return null;
        }
    }

    private void showEmptyState() {
        notificationsRecyclerView.setVisibility(View.GONE);
        emptyStateTextView.setVisibility(View.VISIBLE);
        emptyStateTextView.setText("No notifications yet");
    }

    /**
     * handles the accept button click, checking db to see if they have already accepted
     * @param notification
     */
    private void handleAcceptClicked(Notification notification) {
        Log.d(TAG, "Accept clicked for notification: " + notification.getId());

        if (notification.isResponded()) {
            Toast.makeText(getContext(), "You've already responded to this invitation", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(getContext())
                .setTitle("Accept Invitation")
                .setMessage("Are you sure you want to accept the invitation for \"" +
                        notification.getEventName() + "\"?")
                .setPositiveButton("Accept", (dialog, which) -> acceptInvitation(notification))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void acceptInvitation(Notification notification) {
        progressBar.setVisibility(View.VISIBLE);

        lotteryManager.acceptInvitation(notification.getEventId(), userId,
                new LotteryManager.StatusCallback() {
                    @Override
                    public void onSuccess(String message) {
                        Log.d(TAG, "Invitation accepted successfully");
                        markNotificationAsResponded(notification.getId());
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(),
                                "Invitation accepted! You're signed up for " + notification.getEventName(),
                                Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Error accepting invitation: " + error);
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(),
                                "Error: " + error,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * decline button is clicked handles logic for it
     * @param notification
     */
    private void handleDeclineClicked(Notification notification) {
        Log.d(TAG, "Decline clicked for notification: " + notification.getId());

        if (notification.isResponded()) {
            Toast.makeText(getContext(), "You've already responded to this invitation", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(getContext())
                .setTitle("Decline Invitation")
                .setMessage("Are you sure you want to decline the invitation for \"" +
                        notification.getEventName() + "\"? This will give your spot to another person.")
                .setPositiveButton("Decline", (dialog, which) -> declineInvitation(notification))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void declineInvitation(Notification notification) {
        progressBar.setVisibility(View.VISIBLE);

        lotteryManager.declineInvitation(notification.getEventId(), userId,
                new LotteryManager.StatusCallback() {
                    @Override
                    public void onSuccess(String message) {
                        Log.d(TAG, "Invitation declined successfully");
                        markNotificationAsResponded(notification.getId());
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(),
                                "Invitation declined. Your spot has been given to another person.",
                                Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Error declining invitation: " + error);
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(),
                                "Error: " + error,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void markNotificationAsResponded(String notificationId) {
        db.collection("notifications")
                .document(userId)
                .collection("messages")
                .document(notificationId)
                .update("responded", true, "read", true)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Notification marked as responded"))
                .addOnFailureListener(e -> Log.e(TAG, "Error marking notification as responded", e));
    }

    /**
     * gets the current user ID so user can be assigned to the correct evvent
     */
    private void getUserIdAndLoadNotifications() {
        SharedPreferences prefs = getActivity().getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String cachedUserId = prefs.getString("userId", null);

        if (cachedUserId != null && !cachedUserId.isEmpty()) {
            userId = cachedUserId;
            Log.d(TAG, "Using cached userId: " + userId);
            loadNotifications();
            return;
        }

        String deviceId = android.provider.Settings.Secure.getString(
                getActivity().getContentResolver(),
                android.provider.Settings.Secure.ANDROID_ID
        );

        Log.d(TAG, "Device ID: " + deviceId + ", querying Firebase for userId...");
        progressBar.setVisibility(View.VISIBLE);

        db.collection("users")
                .whereEqualTo("deviceId", deviceId)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot userDoc = querySnapshot.getDocuments().get(0);
                        userId = userDoc.getId();
                        Log.d(TAG, "Found userId: " + userId);
                        prefs.edit().putString("userId", userId).apply();
                        loadNotifications();
                    } else {
                        progressBar.setVisibility(View.GONE);
                        Log.e(TAG, "No user found with deviceId: " + deviceId);
                        Toast.makeText(getContext(), "User not registered. Please register first.", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Error querying user by deviceId", e);
                    Toast.makeText(getContext(), "Error loading user data", Toast.LENGTH_SHORT).show();
                });
    }
}

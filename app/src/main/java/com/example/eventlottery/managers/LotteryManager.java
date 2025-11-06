package com.example.eventlottery.managers;
import android.app.NotificationManager;
import android.util.Log;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lottery manager - handles lottery logic for events
 * Runs the inital lottery draw
 * Manages taking entrants from waitlsit to accepted or decliend
 * draws replacements when entrants decline
 */

public class LotteryManager {

    private static final String TAG = "LotteryManager";
    private final FirebaseFirestore db;
    private final NotificationManager notificationManager;

    /**
     * Constructor
     */
    public LotteryManager() {
        this.db = FirebaseFirestore.getInstance();
        this.notificationManager = new NotificationManager();
    }

    /**
     * fetches the entrants from waiting lsit
     * shuffles the list
     *
     * @param eventId
     * @param sampleSize
     * @param callback
     */
    public void initializeLottery(String eventId, int sampleSize, LotteryCallback callback) {
        if (eventId == null || eventId.isEmpty()) {
            callback.onError("Invalid event ID");
            return;
        }

        if (sampleSize <= 0) {
            callback.onError("Sample size must be greater than 0");
            return;
        }

        Log.d(TAG, "Initializing lottery for event: " + eventId + " with sample size: " + sampleSize);

        // get entrants from waitingLists that are tied to an eventID
        DocumentReference waitingListRef = db.collection("waitingLists").document(eventId);

        waitingListRef.get().addOnSuccessListener(documentSnapshot -> {
            if (!documentSnapshot.exists()) {
                callback.onError("Waiting list not found for event");
                return;
            }

            // Get entrants array
            List<String> entrants = (List<String>) documentSnapshot.get("entrants");

            if (entrants == null || entrants.isEmpty()) {
                callback.onError("No entrants in waiting list");
                return;
            }

            if (sampleSize > entrants.size()) {
                callback.onError("Sample size (" + sampleSize + ") exceeds number of entrants (" + entrants.size() + ")");
                return;
            }

            //  shuffles the list
            List<String> shuffledEntrants = new ArrayList<>(entrants);
            Collections.shuffle(shuffledEntrants);

            Log.d(TAG, "Shuffled " + shuffledEntrants.size() + " entrants");

            // tkaes the sample size that the orgnaizer selected
            List<String> winners = shuffledEntrants.subList(0, sampleSize);
            List<String> losers = shuffledEntrants.subList(sampleSize, shuffledEntrants.size());

            Log.d(TAG, "Winners: " + winners.size() + ", Losers: " + losers.size());

            // updats that into firebase so we can save this
            WriteBatch batch = db.batch();

            // Store the shuffled lottery order for future replacement draws
            batch.update(waitingListRef, "lotteryOrder", shuffledEntrants);

            // sets the point wheer we would redraw from
            batch.update(waitingListRef, "currentDrawIndex", sampleSize);

            // Create selected map with status="pending" for each winner
            Map<String, Object> selectedMap = new HashMap<>();
            for (String userId : winners) {
                Map<String, String> userStatus = new HashMap<>();
                userStatus.put("status", "pending");
                userStatus.put("timestamp", String.valueOf(System.currentTimeMillis()));
                selectedMap.put(userId, userStatus);
            }
            batch.update(waitingListRef, "selected", selectedMap);

            // empty array for the peope tat click accept
            batch.update(waitingListRef, "accepted", new ArrayList<String>());

            // Commit the batch
            batch.commit().addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Lottery initialization successful");

                // sends the notifications and gets the name of the event for the noticifcaiont
                db.collection("events").document(eventId).get()
                        .addOnSuccessListener(eventDoc -> {
                            String eventName = eventDoc.getString("name");
                            if (eventName == null) eventName = "Event";

                            // Send win notifications to winners
                            for (String winnerId : winners) {
                                notificationManager.sendWinNotification(winnerId, eventId, eventName);
                            }

                            // Send loss notifications to losers
                            notificationManager.notifyAllLosers(eventId, losers);

                            callback.onSuccess("Lottery completed: " + winners.size() + " winners selected");
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error fetching event name", e);
                            callback.onSuccess("Lottery completed but notification failed");
                        });

            }).addOnFailureListener(e -> {
                Log.e(TAG, "Error committing lottery batch", e);
                callback.onError("Failed to save lottery results: " + e.getMessage());
            });

        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error fetching waiting list", e);
            callback.onError("Failed to fetch waiting list: " + e.getMessage());
        });
    }

    /**
     *handles re running the code if someone declines the event
     * @param eventId The event ID
     * @param callback Callback with replacement userId or error
     */
    public void drawReplacement(String eventId, ReplacementCallback callback) {
        if (eventId == null || eventId.isEmpty()) {
            callback.onError("Invalid event ID");
            return;
        }

        Log.d(TAG, "Drawing replacement for event: " + eventId);

        DocumentReference waitingListRef = db.collection("waitingLists").document(eventId);

        waitingListRef.get().addOnSuccessListener(documentSnapshot -> {
            if (!documentSnapshot.exists()) {
                callback.onError("Waiting list not found");
                return;
            }

            // gets current posioiont that we are at in the array of the selected
            Long currentIndexLong = documentSnapshot.getLong("currentDrawIndex");
            if (currentIndexLong == null) {
                callback.onError("Lottery not initialized");
                return;
            }
            int currentDrawIndex = currentIndexLong.intValue();

            // gets the order of the lotto
            List<String> lotteryOrder = (List<String>) documentSnapshot.get("lotteryOrder");
            if (lotteryOrder == null || lotteryOrder.isEmpty()) {
                callback.onError("Lottery order not found");
                return;
            }

            // checks to see if we have already hit the end of the peopel in wiating list
            if (currentDrawIndex >= lotteryOrder.size()) {
                callback.onNoMoreEntrants("No more entrants available");
                return;
            }

            // takes the next user in list
            String replacementUserId = lotteryOrder.get(currentDrawIndex);

            Log.d(TAG, "Drawing replacement: " + replacementUserId + " at index " + currentDrawIndex);

            // adds them into pending status
            Map<String, Object> updates = new HashMap<>();

            // puts that user into the map of the pendings entrants
            Map<String, String> userStatus = new HashMap<>();
            userStatus.put("status", "pending");
            userStatus.put("timestamp", String.valueOf(System.currentTimeMillis()));
            updates.put("selected." + replacementUserId, userStatus);

            // Increment draw index
            updates.put("currentDrawIndex", currentDrawIndex + 1);

            waitingListRef.update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Replacement drawn successfully: " + replacementUserId);

                        // Send notification to replacement
                        db.collection("events").document(eventId).get()
                                .addOnSuccessListener(eventDoc -> {
                                    String eventName = eventDoc.getString("name");
                                    if (eventName == null) eventName = "Event";

                                    notificationManager.sendReplacementNotification(replacementUserId, eventId, eventName);
                                    callback.onSuccess(replacementUserId);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error fetching event for notification", e);
                                    callback.onSuccess(replacementUserId); // Still return success
                                });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error drawing replacement", e);
                        callback.onError("Failed to draw replacement: " + e.getMessage());
                    });

        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error fetching waiting list", e);
            callback.onError("Failed to fetch waiting list: " + e.getMessage());
        });
    }

    /**
     * Moves people who have accepted from being in penidng status to being in accepted status so they lockedin their spot
     *
     * @param eventId The event ID
     * @param userId The user accepting
     * @param callback Callback for success/error
     */
    public void acceptInvitation(String eventId, String userId, StatusCallback callback) {
        if (eventId == null || eventId.isEmpty() || userId == null || userId.isEmpty()) {
            callback.onError("Invalid event ID or user ID");
            return;
        }

        Log.d(TAG, "User " + userId + " accepting invitation for event " + eventId);

        DocumentReference waitingListRef = db.collection("waitingLists").document(eventId);

        waitingListRef.get().addOnSuccessListener(documentSnapshot -> {
            if (!documentSnapshot.exists()) {
                callback.onError("Waiting list not found");
                return;
            }

            // Verify user is in selected with pending status
            Map<String, Object> selected = (Map<String, Object>) documentSnapshot.get("selected");
            if (selected == null || !selected.containsKey(userId)) {
                callback.onError("User not found in selected list");
                return;
            }

            Map<String, Object> userStatus = (Map<String, Object>) selected.get(userId);
            String status = (String) userStatus.get("status");

            if (!"pending".equals(status)) {
                callback.onError("Invitation already responded to");
                return;
            }

            // changes the status to accepted for a user
            // adds them into an array of the accepeted people
            Map<String, Object> updates = new HashMap<>();
            userStatus.put("status", "accepted");
            userStatus.put("acceptedTimestamp", String.valueOf(System.currentTimeMillis()));
            updates.put("selected." + userId, userStatus);
            updates.put("accepted", FieldValue.arrayUnion(userId));

            waitingListRef.update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "User accepted successfully");
                        callback.onSuccess("Invitation accepted");
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error accepting invitation", e);
                        callback.onError("Failed to accept invitation: " + e.getMessage());
                    });

        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error fetching waiting list", e);
            callback.onError("Failed to fetch waiting list: " + e.getMessage());
        });
    }

    /**
     * Handle entrant declining invitation
     *
     * sets the user as delcined and initites the redraw process
     *
     * @param eventId The event ID
     * @param userId The user declining
     * @param callback Callback for success/error
     */
    public void declineInvitation(String eventId, String userId, StatusCallback callback) {
        if (eventId == null || eventId.isEmpty() || userId == null || userId.isEmpty()) {
            callback.onError("Invalid event ID or user ID");
            return;
        }

        Log.d(TAG, "User " + userId + " declining invitation for event " + eventId);

        DocumentReference waitingListRef = db.collection("waitingLists").document(eventId);

        waitingListRef.get().addOnSuccessListener(documentSnapshot -> {
            if (!documentSnapshot.exists()) {
                callback.onError("Waiting list not found");
                return;
            }

            // Verify user is in selected with pending status
            Map<String, Object> selected = (Map<String, Object>) documentSnapshot.get("selected");
            if (selected == null || !selected.containsKey(userId)) {
                callback.onError("User not found in selected list");
                return;
            }

            Map<String, Object> userStatus = (Map<String, Object>) selected.get(userId);
            String status = (String) userStatus.get("status");

            if (!"pending".equals(status)) {
                callback.onError("Invitation already responded to");
                return;
            }

            // updates the status to decliend
            userStatus.put("status", "declined");
            userStatus.put("declinedTimestamp", String.valueOf(System.currentTimeMillis()));

            waitingListRef.update("selected." + userId, userStatus)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "User declined successfully, drawing replacement");

                        // draw their replacement
                        drawReplacement(eventId, new ReplacementCallback() {
                            @Override
                            public void onSuccess(String replacementUserId) {
                                Log.d(TAG, "Replacement drawn: " + replacementUserId);
                                callback.onSuccess("Invitation declined, replacement drawn");
                            }

                            @Override
                            public void onNoMoreEntrants(String message) {
                                Log.d(TAG, "No more entrants available for replacement");
                                callback.onSuccess("Invitation declined, no more entrants available");
                            }

                            @Override
                            public void onError(String error) {
                                Log.e(TAG, "Error drawing replacement: " + error);
                                callback.onSuccess("Invitation declined, but failed to draw replacement");
                            }
                        });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error declining invitation", e);
                        callback.onError("Failed to decline invitation: " + e.getMessage());
                    });

        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error fetching waiting list", e);
            callback.onError("Failed to fetch waiting list: " + e.getMessage());
        });
    }

    //call backs

    /**
     * Callback for lottery initialization
     */
    public interface LotteryCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    /**
     * Callback for drawing replacement
     */
    public interface ReplacementCallback {
        void onSuccess(String replacementUserId);
        void onNoMoreEntrants(String message);
        void onError(String error);
    }

    /**
     * Callback for status changes (accept/decline)
     */
        public interface StatusCallback {
        void onSuccess(String message);
        void onError(String error);
    }
}
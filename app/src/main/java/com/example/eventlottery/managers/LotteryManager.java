package com.example.eventlottery.managers;

import android.util.Log;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lottery manager - handles lottery logic for events
 * Stores all lists (accepted, declined, retry, selected) directly in the 'events' collection.
 */
public class LotteryManager {

    private static final String TAG = "LotteryManager";
    private final FirebaseFirestore db;
    private final NotificationManager notificationManager;

    public LotteryManager() {
        this.db = FirebaseFirestore.getInstance();
        this.notificationManager = new NotificationManager();
    }

    /**
     * Fetches entrants from the event's waitlistUsers field, shuffles them, and selects winners.
     * Saves the results back to the 'events' document.
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

        Log.d(TAG, "Initializing lottery for event: " + eventId);

        // Reference  event document
        DocumentReference eventRef = db.collection("events").document(eventId);

        eventRef.get().addOnSuccessListener(eventSnapshot -> {
            if (!eventSnapshot.exists()) {
                callback.onError("Event not found");
                return;
            }

            // gets users from wiatlist
            List<String> entrants = (List<String>) eventSnapshot.get("waitlistUsers");

            if (entrants == null || entrants.isEmpty()) {
                callback.onError("No entrants in waiting list");
                return;
            }

            if (sampleSize > entrants.size()) {
                callback.onError("Sample size (" + sampleSize + ") exceeds number of entrants");
                return;
            }

            // does the shuffle and sperates winners and lsoers
            List<String> shuffledEntrants = new ArrayList<>(entrants);
            Collections.shuffle(shuffledEntrants);

            List<String> winners = shuffledEntrants.subList(0, sampleSize);
            List<String> losers = shuffledEntrants.subList(sampleSize, shuffledEntrants.size());

            Log.d(TAG, "Winners: " + winners.size() + ", Losers: " + losers.size());

            // updates for the event
            WriteBatch batch = db.batch();

            // Store lottery order and index
            batch.update(eventRef, "lotteryOrder", shuffledEntrants);
            batch.update(eventRef, "currentDrawIndex", sampleSize);

            // Initialize accepted list as empty
            batch.update(eventRef, "accepted", new ArrayList<String>());
            // Initialize declined list as empty
            batch.update(eventRef, "declined", new ArrayList<String>());

            // Create 'selected' map to track status of winners
            Map<String, Object> selectedMap = new HashMap<>();
            for (String userId : winners) {
                Map<String, String> userStatus = new HashMap<>();
                userStatus.put("status", "pending");
                userStatus.put("timestamp", String.valueOf(System.currentTimeMillis()));
                selectedMap.put(userId, userStatus);
            }
            batch.update(eventRef, "selected", selectedMap);


            batch.commit().addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Lottery initialization successful");


                String eventName = eventSnapshot.getString("eventName");
                if (eventName == null) eventName = "Event";

                // send noticaitons to the winners
                for (String winnerId : winners) {
                    notificationManager.sendWinNotification(winnerId, eventId, eventName);
                }
                notificationManager.notifyAllLosers(eventId, losers);

                callback.onSuccess("Lottery completed: " + winners.size() + " winners selected");

            }).addOnFailureListener(e -> {
                Log.e(TAG, "Error committing lottery batch", e);
                callback.onError("Failed to save lottery results: " + e.getMessage());
            });

        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error fetching event", e);
            callback.onError("Failed to fetch event: " + e.getMessage());
        });
    }

    /**
     * Draws the next person from lotteryOrder ONLY IF they are in the 'retryEntrants' list.
     * Skips users who did not opt-in to the retry pool.
     */
    public void drawReplacement(String eventId, ReplacementCallback callback) {
        if (eventId == null || eventId.isEmpty()) {
            callback.onError("Invalid event ID");
            return;
        }

        DocumentReference eventRef = db.collection("events").document(eventId);

        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(eventRef);

            if (!snapshot.exists()) {
                throw new RuntimeException("Event not found");
            }


            List<String> lotteryOrder = (List<String>) snapshot.get("lotteryOrder");
            List<String> retryEntrants = (List<String>) snapshot.get("retryEntrants"); // Fetch the retry list
            Long currentIndexLong = snapshot.getLong("currentDrawIndex");

            if (lotteryOrder == null || currentIndexLong == null) {
                throw new RuntimeException("Lottery data missing");
            }

            int index = currentIndexLong.intValue();
            String replacementUserId = null;
            int newIndex = index;

            //  Loop through lotteryOrder to find the next candidate who IS in retryEntrants
            // If retryEntrants is null (no one joined), we can't pick anyone
            if (retryEntrants != null) {
                while (newIndex < lotteryOrder.size()) {
                    String candidateId = lotteryOrder.get(newIndex);

                    // Check if this candidate joined the retry pool
                    if (retryEntrants.contains(candidateId)) {
                        replacementUserId = candidateId;
                        break; // Found a match!
                    }

                    // If not in retry pool, skip them and keep looking
                    newIndex++;
                }
            }

            if (replacementUserId == null) {
                throw new RuntimeException("NO_REPLACEMENTS_AVAILABLE");
            }

            //  Update the found user to pending
            Map<String, Object> selectedMap = (Map<String, Object>) snapshot.get("selected");
            if (selectedMap == null) selectedMap = new HashMap<>();

            Map<String, String> userStatus = new HashMap<>();
            userStatus.put("status", "pending");
            userStatus.put("timestamp", String.valueOf(System.currentTimeMillis()));

            selectedMap.put(replacementUserId, userStatus);

            //  Write changes back
            transaction.update(eventRef, "selected", selectedMap);
            transaction.update(eventRef, "currentDrawIndex", newIndex + 1); // Update index to after this user

            // Return the userId and eventName for the callback
            String eventName = snapshot.getString("eventName");
            return new String[]{replacementUserId, eventName};

        }).addOnSuccessListener(result -> {

            String userId = result[0];
            String eventName = result[1] != null ? result[1] : "Event";

            notificationManager.sendReplacementNotification(userId, eventId, eventName);
            callback.onSuccess(userId);

        }).addOnFailureListener(e -> {
            if (e.getMessage() != null && e.getMessage().contains("NO_REPLACEMENTS_AVAILABLE")) {
                callback.onNoMoreEntrants("No valid entrants in retry pool");
            } else {
                callback.onError("Failed to draw replacement: " + e.getMessage());
            }
        });
    }

    public void acceptInvitation(String eventId, String userId, StatusCallback callback) {
        updateUserStatus(eventId, userId, "accepted", callback);
    }

    public void declineInvitation(String eventId, String userId, StatusCallback callback) {

        updateUserStatus(eventId, userId, "declined", new StatusCallback() {
            @Override
            public void onSuccess(String message) {
                // send notifcaton to rreplacement
                drawReplacement(eventId, new ReplacementCallback() {
                    @Override
                    public void onSuccess(String replacementUserId) {
                        callback.onSuccess("Declined and replacement drawn");
                    }

                    @Override
                    public void onNoMoreEntrants(String msg) {
                        callback.onSuccess("Declined (no replacement available)");
                    }

                    @Override
                    public void onError(String error) {
                        // User was successfully declined, even if replacement failed
                        callback.onSuccess("Declined (replacement error: " + error + ")");
                    }
                });
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    private void updateUserStatus(String eventId, String userId, String newStatus, StatusCallback callback) {
        DocumentReference eventRef = db.collection("events").document(eventId);

        db.runTransaction(transaction -> {
                    DocumentSnapshot snapshot = transaction.get(eventRef);
                    if (!snapshot.exists()) throw new RuntimeException("Event not found");

                    Map<String, Object> selected = (Map<String, Object>) snapshot.get("selected");
                    if (selected == null || !selected.containsKey(userId)) {
                        throw new RuntimeException("User not in selected list");
                    }

                    Map<String, Object> userStatusMap = (Map<String, Object>) selected.get(userId);
                    String currentStatus = (String) userStatusMap.get("status");

                    if (!"pending".equals(currentStatus)) {
                        throw new RuntimeException("User already responded");
                    }

                    // Update status in the map
                    userStatusMap.put("status", newStatus);
                    userStatusMap.put("timestamp", String.valueOf(System.currentTimeMillis()));
                    transaction.update(eventRef, "selected." + userId, userStatusMap);

                    // Add to specific list (accepted or declined)
                    transaction.update(eventRef, newStatus, FieldValue.arrayUnion(userId));

                    return null;
                }).addOnSuccessListener(result -> callback.onSuccess("Status updated to " + newStatus))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void joinRetryList(String eventId, String userId, StatusCallback callback) {
        if (eventId == null || userId == null) {
            callback.onError("Invalid ID");
            return;
        }
        // retryEntrants is stored on the event document
        db.collection("events").document(eventId)
                .update("retryEntrants", FieldValue.arrayUnion(userId))
                .addOnSuccessListener(a -> callback.onSuccess("Joined retry pool"))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // Interfaces
    public interface LotteryCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    public interface ReplacementCallback {
        void onSuccess(String replacementUserId);
        void onNoMoreEntrants(String message);
        void onError(String error);
    }

    public interface StatusCallback {
        void onSuccess(String message);
        void onError(String error);
    }
}
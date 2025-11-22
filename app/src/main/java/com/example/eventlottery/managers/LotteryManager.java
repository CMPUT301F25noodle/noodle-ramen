package com.example.eventlottery.managers;

import android.util.Log;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Lottery manager - handles lottery logic for events
 * Aligning with: events/{id}/waitlist -> Winners | Losers -> (Losers click Retry) -> events/{id}/retry list
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
     * 1. Access the waitlist from the Event
     * 2. Shuffle and Select Winners
     * 3. Save state (including an empty retry list for Option B)
     */
    public void initializeLottery(String eventId, int sampleSize, LotteryCallback callback) {
        if (eventId == null || eventId.isEmpty()) {
            callback.onError("Invalid event ID");
            return;
        }

        DocumentReference eventRef = db.collection("events").document(eventId);

        eventRef.get().addOnSuccessListener(eventSnapshot -> {
            if (!eventSnapshot.exists()) {
                callback.onError("Event not found");
                return;
            }

            List<String> entrants = (List<String>) eventSnapshot.get("waitlistUsers");

            if (entrants == null || entrants.isEmpty()) {
                callback.onError("No entrants in waiting list");
                return;
            }

            // --- FIX FOR RED LINE ---
            // Create a final variable for the lambda to use
            int calculatedSize = sampleSize;
            if (calculatedSize > entrants.size()) {
                calculatedSize = entrants.size();
            }
            final int finalSampleSize = calculatedSize;
            // ------------------------

            // Shuffle
            List<String> shuffledEntrants = new ArrayList<>(entrants);
            Collections.shuffle(shuffledEntrants);

            // Split
            List<String> winners = shuffledEntrants.subList(0, finalSampleSize);
            List<String> losers = shuffledEntrants.subList(finalSampleSize, shuffledEntrants.size());

            // Save State
            Map<String, Object> lotteryState = new HashMap<>();

            // OPTION B CHANGE: We don't rely on 'lotteryOrder' for replacements anymore.
            // We verify 'retryParticipants' exists for people to join later.
            lotteryState.put("retryParticipants", new ArrayList<String>());

            Map<String, Object> selectedMap = new HashMap<>();
            for (String userId : winners) {
                Map<String, String> userStatus = new HashMap<>();
                userStatus.put("status", "pending");
                selectedMap.put(userId, userStatus);
            }
            lotteryState.put("selected", selectedMap);

            db.collection("waitingLists").document(eventId)
                    .set(lotteryState)
                    .addOnSuccessListener(aVoid -> {
                        String eventName = eventSnapshot.getString("name");
                        if (eventName == null) eventName = "Event";

                        // Notify Winners
                        for (String winnerId : winners) {
                            notificationManager.sendWinNotification(winnerId, eventId, eventName);
                        }

                        // Notify Losers (Prompting them to Retry)
                        notificationManager.notifyAllLosers(eventId, losers);

                        callback.onSuccess("Lottery completed: " + winners.size() + " winners selected");
                    })
                    .addOnFailureListener(e -> callback.onError("Failed to save lottery results"));

        }).addOnFailureListener(e -> callback.onError("Failed to fetch event"));
    }

    /**
     * OPTION B NEW METHOD:
     * Call this when a user clicks "Join Retry Pool" / "Retry" on their loss notification.
     */
    public void joinRetryList(String eventId, String userId, StatusCallback callback) {
        DocumentReference waitingListRef = db.collection("waitingLists").document(eventId);

        // Add user to the explicit retry list
        waitingListRef.update("retryParticipants", FieldValue.arrayUnion(userId))
                .addOnSuccessListener(aVoid -> callback.onSuccess("Joined retry list"))
                .addOnFailureListener(e -> callback.onError("Failed to join retry list"));
    }

    /**
     * OPTION B UPDATE:
     * Picks a replacement specifically from the 'retryParticipants' list.
     */
    public void drawReplacement(String eventId, ReplacementCallback callback) {
        DocumentReference waitingListRef = db.collection("waitingLists").document(eventId);

        waitingListRef.get().addOnSuccessListener(documentSnapshot -> {
            // Get the list of people who explicitly asked to retry
            List<String> retryCandidates = (List<String>) documentSnapshot.get("retryParticipants");

            if (retryCandidates == null || retryCandidates.isEmpty()) {
                callback.onNoMoreEntrants("No users currently in retry list");
                return;
            }

            // Pick a random person from the retry list (Fairness)
            int randomIndex = new Random().nextInt(retryCandidates.size());
            String replacementUserId = retryCandidates.get(randomIndex);

            WriteBatch batch = db.batch();

            // 1. Move them to 'selected' status
            Map<String, String> userStatus = new HashMap<>();
            userStatus.put("status", "pending");
            batch.update(waitingListRef, "selected." + replacementUserId, userStatus);

            // 2. Remove them from the retry list so they aren't picked again
            batch.update(waitingListRef, "retryParticipants", FieldValue.arrayRemove(replacementUserId));

            batch.commit().addOnSuccessListener(aVoid -> {
                // Send Notification
                db.collection("events").document(eventId).get().addOnSuccessListener(eventDoc -> {
                    String name = eventDoc.getString("name");
                    notificationManager.sendReplacementNotification(replacementUserId, eventId, name);
                    callback.onSuccess(replacementUserId);
                });
            }).addOnFailureListener(e -> callback.onError("Failed to draw replacement"));
        });
    }

    public void acceptInvitation(String eventId, String userId, StatusCallback callback) {
        DocumentReference waitingListStateRef = db.collection("waitingLists").document(eventId);

        waitingListStateRef.get().addOnSuccessListener(snapshot -> {
            Map<String, Object> selected = (Map<String, Object>) snapshot.get("selected");
            if (selected == null || !selected.containsKey(userId)) {
                callback.onError("User not pending acceptance");
                return;
            }

            WriteBatch batch = db.batch();

            // Mark as accepted in lottery state
            batch.update(waitingListStateRef, "selected." + userId + ".status", "accepted");
            batch.update(waitingListStateRef, "accepted", FieldValue.arrayUnion(userId));

            // Remove from Waitlist
            DocumentReference waitlistEntryRef = db.collection("events").document(eventId).collection("waitlist").document(userId);
            batch.delete(waitlistEntryRef);

            DocumentReference eventRef = db.collection("events").document(eventId);
            batch.update(eventRef, "waitlistUsers", FieldValue.arrayRemove(userId));
            batch.update(eventRef, "waitlistCount", FieldValue.increment(-1));

            // Add to Accepted Collection
            DocumentReference acceptedEntryRef = db.collection("events").document(eventId).collection("accepted").document(userId);
            Map<String, Object> acceptedData = new HashMap<>();
            acceptedData.put("uid", userId);
            acceptedData.put("timestamp", System.currentTimeMillis());
            acceptedData.put("status", "accepted");
            batch.set(acceptedEntryRef, acceptedData);

            batch.commit()
                    .addOnSuccessListener(aVoid -> callback.onSuccess("Accepted and moved to accepted list"))
                    .addOnFailureListener(e -> callback.onError("Failed to accept"));
        });
    }

    public void declineInvitation(String eventId, String userId, StatusCallback callback) {
        DocumentReference waitingListStateRef = db.collection("waitingLists").document(eventId);

        waitingListStateRef.get().addOnSuccessListener(snapshot -> {
            WriteBatch batch = db.batch();

            batch.update(waitingListStateRef, "selected." + userId + ".status", "declined");

            // Remove from Event Waitlist
            DocumentReference waitlistEntryRef = db.collection("events").document(eventId).collection("waitlist").document(userId);
            batch.delete(waitlistEntryRef);

            DocumentReference eventRef = db.collection("events").document(eventId);
            batch.update(eventRef, "waitlistUsers", FieldValue.arrayRemove(userId));
            batch.update(eventRef, "waitlistCount", FieldValue.increment(-1));

            batch.commit().addOnSuccessListener(aVoid -> {
                // Trigger replacement from the Retry List
                drawReplacement(eventId, new ReplacementCallback() {
                    @Override
                    public void onSuccess(String replacementUserId) {
                        callback.onSuccess("Declined and replacement drawn");
                    }

                    @Override
                    public void onNoMoreEntrants(String message) {
                        // This is valid in Option B: You declined, but nobody has clicked 'Retry' yet.
                        callback.onSuccess("Declined. No retry candidates available yet.");
                    }

                    @Override
                    public void onError(String error) {
                        callback.onSuccess("Declined, but replacement error: " + error);
                    }
                });
            });
        });
    }

    public interface LotteryCallback { void onSuccess(String msg); void onError(String err); }
    public interface StatusCallback { void onSuccess(String msg); void onError(String err); }
    public interface ReplacementCallback { void onSuccess(String uid); void onNoMoreEntrants(String msg); void onError(String err); }
}
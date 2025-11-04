package com.example.eventlottery.managers;

import android.util.Log;

import com.example.eventlottery.WaitlistEntry;
import com.google.firebase.Firebase;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Map;
public class WaitlistManager {
    private static final String TAG = "WaitlistManager";
    private static WaitlistManager instance;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private WaitlistManager() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public static WaitlistManager getInstance() {
        if (instance == null) {
            instance = new WaitlistManager();

        }
        return instance;
    }

    /**
     * join waitlist for an event
     * @param eventId the even we want to join
     * @param callback Result callback
     */

    public void joinWaitlist(String eventId, WaitlistCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onFailure("User not logged in");
            return;
        }

        String userId = currentUser.getUid();

        // First, check if user is already on waitlist
        isUserOnWaitlist(eventId, userId, isOnWaitlist -> {
            if (isOnWaitlist) {
                callback.onFailure("Already on waitlist");
                return;
            }

            // Check event capacity
            checkWaitlistCapacity(eventId, hasCapacity -> {
                if (!hasCapacity) {
                    callback.onFailure("Waitlist is full");
                    return;
                }

                // Perform the join operation
                performJoinWaitlist(eventId, userId, callback);
            });
        });
    }

    /**
     * for leavingna waitlist
     * @param eventId
     * @param callback
     */
    public void leaveWaitlist(String eventId, WaitlistCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onFailure("User not logged in");
            return;
        }

        String userId = currentUser.getUid();

        // Check if user is on waitlist
        isUserOnWaitlist(eventId, userId, isOnWaitlist -> {
            if (!isOnWaitlist) {
                callback.onFailure("Not on waitlist");
                return;
            }

            // Perform the leave operation
            performLeaveWaitlist(eventId, userId, callback);
        });
    }

    /**
     * checks if the user is on the waitlist
     * @param eventId
     * @param callback
     */
    public void isCurrentUserOnWaitlist (String eventId, BooleanCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onResult(false);
            return;
        }
        isUserOnWaitlist(eventId, currentUser.getUid(), callback);

    }

    /**
     * cheks the capacity of an event on waitlsit
     * @param eventId
     * @param callback
     */

    public void getWaitListCount(String eventId, CountCallback callback) {
        db.collection("events").document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Long count = documentSnapshot.getLong("waitlistCount");
                        callback.onResult(count != null ? count.intValue() : 0);
                    } else {
                        callback.onResult(0);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting waitlist count", e);
                    callback.onResult(0);
                });
    }

    private void performJoinWaitlist(String eventId, String userId, WaitlistCallback callback) {

        WaitlistEntry entry = new WaitlistEntry(
                userId,
                System.currentTimeMillis(),
                "waiting",
                null
        );


        WriteBatch batch = db.batch();

        //
        DocumentReference eventWaitlistRef = db.collection("events")
                .document(eventId)
                .collection("waitlist")
                .document(userId);  // ← userId ensures uniqueness
        batch.set(eventWaitlistRef, entry);


        DocumentReference eventRef = db.collection("events").document(eventId);
        batch.update(eventRef, "waitlistCount", FieldValue.increment(1));


        DocumentReference userRef = db.collection("users").document(userId);
        Map<String, Object> waitlistData = new HashMap<>();
        waitlistData.put("joinedAt", System.currentTimeMillis());
        waitlistData.put("status", "waiting");
        batch.update(userRef, "waitingLists." + eventId, waitlistData);

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Successfully joined waitlist: " + eventId);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error joining waitlist", e);
                    callback.onFailure("Failed to join waitlist: " + e.getMessage());
                });
    }

    private void performLeaveWaitlist(String eventId, String userId, WaitlistCallback callback) {

        WriteBatch batch = db.batch();


        DocumentReference eventWaitlistRef = db.collection("events")
                .document(eventId)
                .collection("waitlist")
                .document(userId);
        batch.delete(eventWaitlistRef);


        DocumentReference eventRef = db.collection("events").document(eventId);
        batch.update(eventRef, "waitlistCount", FieldValue.increment(-1));


        DocumentReference userRef = db.collection("users").document(userId);
        batch.update(userRef, "waitingLists." + eventId, FieldValue.delete());


        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Successfully left waitlist: " + eventId);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error leaving waitlist", e);
                    callback.onFailure("Failed to leave waitlist: " + e.getMessage());
                });
    }

    private void isUserOnWaitlist(String eventId, String userId, BooleanCallback callback) {
        db.collection("events").document(eventId)
                .collection("waitlist").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    callback.onResult(documentSnapshot.exists());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking waitlist status", e);
                    callback.onResult(false);
                });
    }

    private void checkWaitlistCapacity(String eventId, BooleanCallback callback) {
        db.collection("events").document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        callback.onResult(false);
                        return;
                    }

                    Long capacity = documentSnapshot.getLong("waitlistCapacity");
                    Long currentCount = documentSnapshot.getLong("waitlistCount");

                    // If capacity is 0 or null, assume unlimited
                    if (capacity == null || capacity == 0) {
                        callback.onResult(true);
                        return;
                    }

                    int count = currentCount != null ? currentCount.intValue() : 0;
                    callback.onResult(count < capacity);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking capacity", e);
                    callback.onResult(false);
                });
    }

    public interface WaitlistCallback {
        void onSuccess();

        void onFailure(String error);
    }

    public interface BooleanCallback {
        void onResult(boolean result) ;

    }

    public interface CountCallback {
        void onResult(int count);

    }

}


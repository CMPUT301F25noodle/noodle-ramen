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

    public void is
}

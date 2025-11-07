package com.example.eventlottery.managers;
import android.util.Log;

import com.google.firebase.Firebase;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * handles sending notificasitons to users
 *
 */
public class NotificationManager {
    private static final String TAG = "NotificationManager";
    private final FirebaseFirestore db;

    // we will send the replacemnts the same messge as acceptors but to keep track and for sake of redundancy we will make sure we label them as people who were sent message after rejection
    public static final String TYPE_WIN = "win";
    public static final String TYPE_LOSS = "loss";
    public static final String TYPE_REPLACEMENT = "replacement";

    public NotificationManager() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * send notificaiton to winner and save that information into db
     * @param userId
     * @param eventId
     * @param eventName
     */
    public void  sendWinNotification(String userId, String eventId, String eventName) {
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "Cannot send win notification: userId is null or empty");
            return;
        }

        Log.d(TAG, "Sending win notification to user: " + userId + " for event: " + eventName);

        String message = "Congratulations! You've been selected for \"" + eventName + "\". " +
                "Please accept or decline your invitation.";

        Map<String, Object> notification = createNotificationData(
                TYPE_WIN,
                eventId,
                eventName,
                message,
                false  // responded = false (needs response)
        );
// saves to firebase the notificatioos that have been sent
        db.collection("notifications")
                .document(userId)
                .collection("messages")
                .add(notification)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Win notification sent successfully to " + userId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error sending win notification to " + userId, e);
                });
    }

    /**
     * sends the loss notification, adds that they may still get in later
     * @param userId
     * @param eventId
     * @param eventName
     */
    public void sendLossNotification(String userId, String eventId, String eventName) {
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "Cannot send loss notification: userId is null or empty");
            return;
        }

        Log.d(TAG, "Sending loss notification to user: " + userId + " for event: " + eventName);

        // Create notification message
        String message = "Unfortunately, you were not selected for \"" + eventName + "\". " +
                "You may still have a chance if selected participants decline.";

        // Create notification document
        Map<String, Object> notification = createNotificationData(
                TYPE_LOSS,
                eventId,
                eventName,
                message,
                true  // responded = true (no response needed)
        );

        // send loss notificaiton to the person for not being selected
        db.collection("notifications")
                .document(userId)
                .collection("messages")
                .add(notification)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Loss notification sent successfully to " + userId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error sending loss notification to " + userId, e);
                });
    }

    /**
     * sends notificaiton to the next selectdd person after the a user rejected
     * seperate for internal retention
     * @param userId
     * @param eventId
     * @param eventName
     */
    public void sendReplacementNotification(String userId, String eventId, String eventName) {
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "Cannot send replacement notification: userId is null or empty");
            return;
        }

        Log.d(TAG, "Sending replacement notification to user: " + userId + " for event: " + eventName);

        // noti is same as win notificaiont
        String message = "Congratulations! You've been selected for \"" + eventName + "\". " +
                "Please accept or decline your invitation.";

        // Create notification document
        // Keep type as "replacement" for internal tracking, but user sees same message as win
        Map<String, Object> notification = createNotificationData(
                TYPE_REPLACEMENT,
                eventId,
                eventName,
                message,
                false  // responded = false (needs response)
        );

        // save to fire base
        db.collection("notifications")
                .document(userId)
                .collection("messages")
                .add(notification)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Replacement notification sent successfully to " + userId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error sending replacement notification to " + userId, e);
                });
    }

    /**
     * sends the inital notificaiton to all people that were not selected in the inital lottery
     * sends as batch
     * @param eventId
     * @param loserIds
     */

    public void notifyAllLosers(String eventId, List<String> loserIds) {
        if (loserIds == null || loserIds.isEmpty()) {
            Log.d(TAG, "No losers to notify for event: " + eventId);
            return;
        }

        Log.d(TAG, "Notifying " + loserIds.size() + " losers for event: " + eventId);

        // gets the event name
        db.collection("events").document(eventId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String eventName = documentSnapshot.getString("name");
                    if (eventName == null) {
                        eventName = "Event";
                    }

                    // Send loss notification to each loser
                    for (String loserId : loserIds) {
                        sendLossNotification(loserId, eventId, eventName);
                    }

                    Log.d(TAG, "All loss notifications queued successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching event name for loss notifications", e);
                    // Still try to send notifications with generic event name
                    for (String loserId : loserIds) {
                        sendLossNotification(loserId, eventId, "Event");
                    }
                });
    }

    /**
     * help method for creatingnthe data strucutre of the noticaiton section in db
     * @param type
     * @param eventId
     * @param eventName
     * @param message
     * @param responded
     * @return
     */

    private Map<String, Object> createNotificationData(String type, String eventId,
                                                       String eventName, String message,
                                                       boolean responded) {
        Map<String, Object> notification = new HashMap<>();

        notification.put("type", type);
        notification.put("eventId", eventId);
        notification.put("eventName", eventName);
        notification.put("message", message);
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("read", false);
        notification.put("responded", responded);

        return notification;
    }

    /**
     * marking the noti as read on our side
     *
     * @param userId The user ID
     * @param notificationId The notification document ID
     */
    public void markAsRead(String userId, String notificationId) {
        db.collection("notifications")
                .document(userId)
                .collection("messages")
                .document(notificationId)
                .update("read", true)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Notification marked as read: " + notificationId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error marking notification as read", e);
                });
    }

    /**
     * if person has reponsded to it, might need to handle people not declining later on idk
     *
     * @param userId The user ID
     * @param notificationId The notification document ID
     */
    public void markAsResponded(String userId, String notificationId) {
        db.collection("notifications")
                .document(userId)
                .collection("messages")
                .document(notificationId)
                .update("responded", true)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Notification marked as responded: " + notificationId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error marking notification as responded", e);
                });
    }


}

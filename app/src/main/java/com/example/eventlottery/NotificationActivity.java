package com.example.eventlottery;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventlottery.managers.LotteryManager;
import com.example.eventlottery.managers.NotificationManager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;


/**
 * NotificationActivity displays a list of notifications for the current user.
 * It handles the logic for accepting or declining event lottery invitations directly from the notification list.
 */
 public class NotificationActivity extends AppCompatActivity{
 private static final String TAG = "NotificationsActivity";


 private RecyclerView notificationsRecyclerView;
 private NotificationAdapter adapter;
 private ProgressBar progressBar;
 private TextView emptyStateTextView;


 private FirebaseFirestore db;
 private String userId;


 private LotteryManager lotteryManager;
 private NotificationManager notificationManager;

 // Data
 private List<Notification> notificationsList;
 /**
  * Initializes the activity, sets up the layout, and begins the process of identifying the user
  * to load their specific notifications.
  *
  * @param savedInstanceState If non-null, this activity is being re-constructed from a previous saved state.
  */
 @Override
 protected void onCreate(Bundle savedInstanceState) {
 super.onCreate(savedInstanceState);
 setContentView(R.layout.notification_fragment);

 db = FirebaseFirestore.getInstance();

 // Initialize views FIRST before trying to use them
 initializeViews();
 setupRecyclerView();



 lotteryManager = new LotteryManager();
 notificationManager = new NotificationManager();

 getUserIdAndLoadNotifications();

 }
 /**
  * Finds and assigns the UI views from the layout resource.
  * Sets up the toolbar title and back button.
  */
 private void initializeViews() {
 notificationsRecyclerView = findViewById(R.id.notifications_recycler_view);
 progressBar = findViewById(R.id.progress_bar);
 emptyStateTextView = findViewById(R.id.empty_state_text);

 // Setup toolbar
 if (getSupportActionBar() != null) {
 getSupportActionBar().setTitle("Notifications");
 getSupportActionBar().setDisplayHomeAsUpEnabled(true);
 }
 }


 /**
  * Configures the RecyclerView with a LayoutManager and the NotificationAdapter.
  * Defines the callback logic for when accept or decline buttons are clicked on a notification.
  */
 private void setupRecyclerView() {
 notificationsList = new ArrayList<>();

 // Create adapter with click listeners
 adapter = new NotificationAdapter(notificationsList, new NotificationAdapter.OnNotificationClickListener() {
 @Override
 public void onAcceptClicked(Notification notification) {
 handleAcceptClicked(notification);
 }

 @Override
 public void onDeclineClicked(Notification notification) {
 handleDeclineClicked(notification);
 }

     @Override
     public void onRetryClicked(Notification notification) {

     }
 });

 // Setup RecyclerView
 notificationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
 notificationsRecyclerView.setAdapter(adapter);
 }

 /**
  * Fetches the user's notifications from the Firestore "notifications" collection.
  * Listens for real-time updates and refreshes the list automatically.
  */
 private void loadNotifications() {
 Log.d(TAG, "Loading notifications for user: " + userId);

 // Show progress
 progressBar.setVisibility(View.VISIBLE);
 emptyStateTextView.setVisibility(View.GONE);

 // get notifcaitons from db
 db.collection("notifications")
 .document(userId)
 .collection("messages")
 .orderBy("timestamp", Query.Direction.DESCENDING)
 .addSnapshotListener((queryDocumentSnapshots, error) -> {
 // Hide progress
 progressBar.setVisibility(View.GONE);

 if (error != null) {
 Log.e(TAG, "Error loading notifications", error);
 Toast.makeText(this, "Error loading notifications", Toast.LENGTH_SHORT).show();
 showEmptyState();
 return;
 }

 if (queryDocumentSnapshots == null || queryDocumentSnapshots.isEmpty()) {
 Log.d(TAG, "No notifications found");
 showEmptyState();
 return;
 }

 // parisng ntoitciaitons
 notificationsList.clear();
 for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
 Notification notification = documentToNotification(document);
 if (notification != null) {
 notificationsList.add(notification);
 }
 }

 Log.d(TAG, "Loaded " + notificationsList.size() + " notifications");

 // Update UI
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
  * Helper method to convert a Firestore DocumentSnapshot into a Notification object.
  *
  * @param document The Firestore document to parse.
  * @return A populated Notification object, or null if parsing fails.
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

 /**
  * Updates the UI to show a "No notifications" message when the list is empty.
  */
 private void showEmptyState() {
 notificationsRecyclerView.setVisibility(View.GONE);
 emptyStateTextView.setVisibility(View.VISIBLE);
 emptyStateTextView.setText("No notifications yet");
 }

 /**
  * Handles the click event for the "Accept" button.
  * Shows a confirmation dialog before proceeding with the acceptance logic.
  *
  * @param notification The notification associated with the accepted event.
  */
 private void handleAcceptClicked(Notification notification) {
 Log.d(TAG, "Accept clicked for notification: " + notification.getId());

 // Check if already responded
 if (notification.isResponded()) {
 Toast.makeText(this, "You've already responded to this invitation", Toast.LENGTH_SHORT).show();
 return;
 }

 //shows the confrimation and asks for are you sure
 new AlertDialog.Builder(this)
 .setTitle("Accept Invitation")
 .setMessage("Are you sure you want to accept the invitation for \"" +
 notification.getEventName() + "\"?")
 .setPositiveButton("Accept", new DialogInterface.OnClickListener() {
 @Override
 public void onClick(DialogInterface dialog, int which) {
 acceptInvitation(notification);
 }
 })
 .setNegativeButton("Cancel", null)
 .show();
 }
 /**
  * Calls the LotteryManager to record the user's acceptance of the event invitation.
  * Updates the UI upon success or failure.
  *
  * @param notification The notification being accepted.
  */
 private void acceptInvitation(Notification notification) {
 // Show progress
 progressBar.setVisibility(View.VISIBLE);

 // accept inviation calls the lotoo mnanger
 lotteryManager.acceptInvitation(notification.getEventId(), userId,
 new LotteryManager.StatusCallback() {
 @Override
 public void onSuccess(String message) {
 Log.d(TAG, "Invitation accepted successfully");

 // update notificaiont status
 markNotificationAsResponded(notification.getId());


 progressBar.setVisibility(View.GONE);


 Toast.makeText(NotificationActivity.this,
 "Invitation accepted! You're signed up for " + notification.getEventName(),
 Toast.LENGTH_LONG).show();


 }

 @Override
 public void onError(String error) {
 Log.e(TAG, "Error accepting invitation: " + error);
 progressBar.setVisibility(View.GONE);
 Toast.makeText(NotificationActivity.this,
 "Error: " + error,
 Toast.LENGTH_SHORT).show();
 }
 });
 }
 /**
  * Handles the click event for the "Decline" button.
  * Shows a confirmation dialog warning the user that their spot will be given away.
  *
  * @param notification The notification associated with the declined event.
  */
 private void handleDeclineClicked(Notification notification) {
 Log.d(TAG, "Decline clicked for notification: " + notification.getId());

 // Check if already responded
 if (notification.isResponded()) {
 Toast.makeText(this, "You've already responded to this invitation", Toast.LENGTH_SHORT).show();
 return;
 }

 // shwos the confirmaiton dialog
 new AlertDialog.Builder(this)
 .setTitle("Decline Invitation")
 .setMessage("Are you sure you want to decline the invitation for \"" +
 notification.getEventName() + "\"? This will give your spot to another person.")
 .setPositiveButton("Decline", new DialogInterface.OnClickListener() {
 @Override
 public void onClick(DialogInterface dialog, int which) {
 declineInvitation(notification);
 }
 })
 .setNegativeButton("Cancel", null)
 .show();
 }
 /**
  * Calls the LotteryManager to record the user's rejection of the event invitation.
  * This may trigger a redraw for a new winner.
  *
  * @param notification The notification being declined.
  */
 private void declineInvitation(Notification notification) {

 progressBar.setVisibility(View.VISIBLE);

 // lotto manager for
 lotteryManager.declineInvitation(notification.getEventId(), userId,
 new LotteryManager.StatusCallback() {
 @Override
 public void onSuccess(String message) {
 Log.d(TAG, "Invitation declined successfully");

 //update ntoficiaiton if the person has responded
 markNotificationAsResponded(notification.getId());


 progressBar.setVisibility(View.GONE);

 // Show success message
 Toast.makeText(NotificationActivity.this,
 "Invitation declined. Your spot has been given to another person.",
 Toast.LENGTH_LONG).show();


 }

 @Override
 public void onError(String error) {
 Log.e(TAG, "Error declining invitation: " + error);
 progressBar.setVisibility(View.GONE);
 Toast.makeText(NotificationActivity.this,
 "Error: " + error,
 Toast.LENGTH_SHORT).show();
 }
 });
 }
 /**
  * Updates the status of a specific notification in Firestore to mark it as 'read' and 'responded'.
  *
  * @param notificationId The ID of the notification to update.
  */
 private void markNotificationAsResponded(String notificationId) {
 db.collection("notifications")
 .document(userId)
 .collection("messages")
 .document(notificationId)
 .update("responded", true, "read", true)
 .addOnSuccessListener(aVoid -> {
 Log.d(TAG, "Notification marked as responded");
 })
 .addOnFailureListener(e -> {
 Log.e(TAG, "Error marking notification as responded", e);
 });
 }

 /**
  * Determines the current user's ID. It first checks for a locally cached ID in SharedPreferences.
  * If not found, it queries Firestore using the device's unique ID to find the user.
  * Once the user ID is found, it triggers the loading of notifications.
  */
private void getUserIdAndLoadNotifications() {
SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);

//check to see if the usre id has been cached
String cachedUserId = prefs.getString("userId", null);

if (cachedUserId != null && !cachedUserId.isEmpty()) {
// Use cached userId
userId = cachedUserId;
Log.d(TAG, "Using cached userId: " + userId);
loadNotifications();
return;
}

// Not cached - need to query Firebase
// Get device ID
String deviceId = android.provider.Settings.Secure.getString(
getContentResolver(),
android.provider.Settings.Secure.ANDROID_ID
);

Log.d(TAG, "Device ID: " + deviceId + ", querying Firebase for userId...");

// Show progress
progressBar.setVisibility(View.VISIBLE);

// Query Firebase for user with this deviceId
db.collection("users")
.whereEqualTo("deviceId", deviceId)
.limit(1)
.get()
.addOnSuccessListener(querySnapshot -> {
if (!querySnapshot.isEmpty()) {

DocumentSnapshot userDoc = querySnapshot.getDocuments().get(0);
userId = userDoc.getId();

Log.d(TAG, "Found userId: " + userId);

// Cache it for next time
prefs.edit().putString("userId", userId).apply();

// Now load notifications
loadNotifications();
} else {
// No user found with this deviceId
progressBar.setVisibility(View.GONE);
Log.e(TAG, "No user found with deviceId: " + deviceId);
Toast.makeText(this, "User not registered. Please register first.", Toast.LENGTH_LONG).show();
finish();
}
})
.addOnFailureListener(e -> {
progressBar.setVisibility(View.GONE);
Log.e(TAG, "Error querying user by deviceId", e);
Toast.makeText(this, "Error loading user data", Toast.LENGTH_SHORT).show();
finish();
});
}

 @Override
 public boolean onSupportNavigateUp() {
 finish();
 return true;
 }


 }

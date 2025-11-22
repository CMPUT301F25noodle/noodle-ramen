package com.example.eventlottery.fragments;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.example.eventlottery.R;
import com.example.eventlottery.managers.CSVDownloadManager;
import com.example.eventlottery.managers.LotteryManager;
import com.example.eventlottery.managers.CSVDownloadManager;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
public class EventManagementFragment extends Fragment implements OnMapReadyCallback{
    private static final String TAG = "EventManagement";
    private static final String ARG_EVENT_ID = "eventId";

    // UI Components
    private TextView tvEventName, tvWaitlistCount, tvPoolSize, tvLotteryStatus;
    private LinearLayout waitlistPreviewContainer, postDrawActionsContainer;
    private Button btnViewAllWaitlist, btnDrawLottery, btnBack;
    private Button btnViewAccepted, btnDownloadAccepted;
    private Button btnViewDeclined, btnDownloadDeclined;
    private Button btnViewRetry, btnDownloadRetry;
    private Button btnDownloadAllWaitlist;
    private CardView mapCard;
    private ProgressBar progressBar;

    // Firebase
    private FirebaseFirestore db;
    private LotteryManager lotteryManager;

    // Data
    private String eventId;
    private String eventName;
    private int poolSize;
    private String lotteryStatus;
    private GoogleMap googleMap;

    // Firestore field names for different entrant categories
    private static final String FIELD_WAITLIST = "waitlistUsers";

    private static final String FIELD_ACCEPTED = "acceptedEntrants";
    private static final String FIELD_DECLINED = "declinedEntrants";
    private static final String FIELD_RETRY = "retryEntrants";


    public static EventManagementFragment newInstance(String eventId) {
        EventManagementFragment fragment = new EventManagementFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EVENT_ID, eventId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        lotteryManager = new LotteryManager();

        if (getArguments() != null) {
            eventId = getArguments().getString(ARG_EVENT_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_event_management, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        setupMap();
        loadEventData();
        setupListeners();
    }

    private void initializeViews(View view) {
        tvEventName = view.findViewById(R.id.tv_event_name);
        tvWaitlistCount = view.findViewById(R.id.tv_waitlist_count);
        tvPoolSize = view.findViewById(R.id.tv_pool_size);
        tvLotteryStatus = view.findViewById(R.id.tv_lottery_status);
        btnBack = view.findViewById(R.id.btn_back);

        waitlistPreviewContainer = view.findViewById(R.id.waitlist_preview_container);
        btnViewAllWaitlist = view.findViewById(R.id.btn_view_all_waitlist);
        btnDownloadAllWaitlist = view.findViewById(R.id.btn_download_all_waitlist);

        mapCard = view.findViewById(R.id.map_card);
        btnDrawLottery = view.findViewById(R.id.btn_draw_lottery);

        postDrawActionsContainer = view.findViewById(R.id.post_draw_actions_container);
        btnViewAccepted = view.findViewById(R.id.btn_view_accepted);
        btnDownloadAccepted = view.findViewById(R.id.btn_download_accepted);
        btnViewDeclined = view.findViewById(R.id.btn_view_declined);
        btnDownloadDeclined = view.findViewById(R.id.btn_download_declined);
        btnViewRetry = view.findViewById(R.id.btn_view_retry);
        btnDownloadRetry = view.findViewById(R.id.btn_download_retry);

        progressBar = view.findViewById(R.id.progress_bar);
    }

    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.map_fragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setScrollGesturesEnabled(true);


        loadEntrantLocations();
    }

    private void loadEventData() {
        progressBar.setVisibility(View.VISIBLE);

        db.collection("events").document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    progressBar.setVisibility(View.GONE);

                    if (!documentSnapshot.exists()) {
                        Toast.makeText(getContext(), "Event not found", Toast.LENGTH_SHORT).show();
                        if (getActivity() != null) {
                            getActivity().getSupportFragmentManager().popBackStack();
                        }
                        return;
                    }

                    eventName = documentSnapshot.getString("eventName");
                    String poolSizeStr = documentSnapshot.getString("entrantMaxCapacity");

                    List<String> waitlistUsers = (List<String>) documentSnapshot.get(FIELD_WAITLIST);
                    int waitlistCount = waitlistUsers != null ? waitlistUsers.size() : 0;

                    lotteryStatus = documentSnapshot.getString("lotteryStatus");
                    if (lotteryStatus == null || lotteryStatus.isEmpty()) {
                        lotteryStatus = "not_run";
                    }

                    // Parse pool size
                    if (poolSizeStr != null && !poolSizeStr.isEmpty()) {
                        try {
                            poolSize = Integer.parseInt(poolSizeStr);
                        } catch (NumberFormatException e) {
                            poolSize = 0;
                        }
                    } else {
                        poolSize = 0;
                    }

                    // updates UI
                    tvEventName.setText(eventName != null ? eventName : "Unknown Event");
                    tvWaitlistCount.setText("Total Entrants: " + waitlistCount);
                    tvPoolSize.setText("Sample Size: " + poolSize);

                    updateLotteryStatusUI();
                    loadWaitlistPreview();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Error loading event", e);
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateLotteryStatusUI() {
        if ("completed".equals(lotteryStatus)) {
            tvLotteryStatus.setText("Lottery: COMPLETED");
            tvLotteryStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            btnDrawLottery.setEnabled(false);
            btnDrawLottery.setText("Lottery Already Run");
            postDrawActionsContainer.setVisibility(View.VISIBLE);
        } else {
            tvLotteryStatus.setText("Lottery: NOT RUN");
            tvLotteryStatus.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
            btnDrawLottery.setEnabled(true);
            btnDrawLottery.setText("Draw Lottery");
            postDrawActionsContainer.setVisibility(View.GONE);
        }
    }
    private void loadWaitlistPreview() {
        db.collection("events").document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    List<String> waitlistUsers = (List<String>) doc.get(FIELD_WAITLIST);

                    if (waitlistUsers == null || waitlistUsers.isEmpty()) {
                        waitlistPreviewContainer.removeAllViews();
                        TextView emptyView = new TextView(getContext());
                        emptyView.setText("No entrants yet");
                        emptyView.setTextColor(Color.GRAY);
                        emptyView.setPadding(16, 16, 16, 16);
                        waitlistPreviewContainer.addView(emptyView);
                        return;
                    }

                    waitlistPreviewContainer.removeAllViews();

                    // Show first 10
                    int limit = Math.min(10, waitlistUsers.size());
                    for (int i = 0; i < limit; i++) {
                        String userId = waitlistUsers.get(i);
                        addEntrantPreviewItem(userId);
                    }
                });
    }

    private void addEntrantPreviewItem(String userId) {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(userDoc -> {
                    String name = userDoc.exists() ? userDoc.getString("name") : "Unknown User";

                    TextView nameView = new TextView(getContext());
                    nameView.setText("• " + name);
                    nameView.setTextSize(14);
                    nameView.setTextColor(Color.BLACK);
                    nameView.setPadding(16, 8, 16, 8);

                    waitlistPreviewContainer.addView(nameView);
                });
    }
    private void loadEntrantLocations() {
        if (googleMap == null) return;

        db.collection("events").document(eventId)
                .collection("entrantLocations")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        Log.d(TAG, "No location data available");
                        return;
                    }

                    LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
                    boolean hasLocations = false;

                    for (DocumentSnapshot locDoc : querySnapshot.getDocuments()) {
                        GeoPoint geoPoint = locDoc.getGeoPoint("location");
                        String userName = locDoc.getString("userName");
                        String userId = locDoc.getId();

                        if (geoPoint != null) {
                            LatLng position = new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude());

                            // Determine marker color based on status
                            float markerColor = getMarkerColorForUser(userId);

                            googleMap.addMarker(new MarkerOptions()
                                    .position(position)
                                    .title(userName != null ? userName : "Entrant")
                                    .icon(BitmapDescriptorFactory.defaultMarker(markerColor)));

                            boundsBuilder.include(position);
                            hasLocations = true;
                        }
                    }

                    // Zoom to show all markers
                    if (hasLocations) {
                        LatLngBounds bounds = boundsBuilder.build();
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading locations", e);
                });
    }
    private float getMarkerColorForUser(String userId) {
        // This will be checked against event document arrays
        // For now, return default blue (will update after implementing status checks)
        return BitmapDescriptorFactory.HUE_AZURE;
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        btnViewAllWaitlist.setOnClickListener(v -> viewEntrantList(FIELD_WAITLIST, "All Waitlist Entrants"));

        btnDownloadAllWaitlist.setOnClickListener(v -> downloadEntrantList(FIELD_WAITLIST, "waitlist_entrants"));

        btnDrawLottery.setOnClickListener(v -> showDrawLotteryDialog());

        // Post-draw buttons
        btnViewAccepted.setOnClickListener(v -> viewEntrantList(FIELD_ACCEPTED, "Accepted Entrants"));
        btnDownloadAccepted.setOnClickListener(v -> downloadEntrantList(FIELD_ACCEPTED, "accepted_entrants"));

        btnViewDeclined.setOnClickListener(v -> viewEntrantList(FIELD_DECLINED, "Declined Entrants"));
        btnDownloadDeclined.setOnClickListener(v -> downloadEntrantList(FIELD_DECLINED, "declined_entrants"));

        btnViewRetry.setOnClickListener(v -> viewEntrantList(FIELD_RETRY, "Retry Entrants"));
        btnDownloadRetry.setOnClickListener(v -> downloadEntrantList(FIELD_RETRY, "retry_entrants"));
    }

    private void viewEntrantList(String fieldName, String title) {
        progressBar.setVisibility(View.VISIBLE);

        // 1. ACCEPTED (Subcollection)
        if (fieldName.equals(FIELD_ACCEPTED)) {
            db.collection("events").document(eventId).collection("accepted").get()
                    .addOnSuccessListener(snapshot -> {
                        List<String> userIds = new ArrayList<>();
                        for(DocumentSnapshot d : snapshot.getDocuments()) userIds.add(d.getId());
                        fetchUserNamesAndShowDialog(userIds, title, fieldName);
                    })
                    .addOnFailureListener(this::handleError);
        }
        // 2. DECLINED (Subcollection)
        else if (fieldName.equals(FIELD_DECLINED)) {
            db.collection("events").document(eventId).collection("declined").get()
                    .addOnSuccessListener(snapshot -> {
                        List<String> userIds = new ArrayList<>();
                        for(DocumentSnapshot d : snapshot.getDocuments()) userIds.add(d.getId());
                        fetchUserNamesAndShowDialog(userIds, title, fieldName);
                    })
                    .addOnFailureListener(this::handleError);
        }
        // 3. RETRY (WaitingLists Collection -> Array)
        else if (fieldName.equals(FIELD_RETRY)) {
            db.collection("waitingLists").document(eventId).get()
                    .addOnSuccessListener(doc -> {
                        List<String> userIds = (List<String>) doc.get("retryParticipants");
                        fetchUserNamesAndShowDialog(userIds, title, fieldName);
                    })
                    .addOnFailureListener(this::handleError);
        }
        // 4. WAITLIST (Events Collection -> Array)
        else {
            db.collection("events").document(eventId).get()
                    .addOnSuccessListener(doc -> {
                        List<String> userIds = (List<String>) doc.get(FIELD_WAITLIST);
                        fetchUserNamesAndShowDialog(userIds, title, fieldName);
                    })
                    .addOnFailureListener(this::handleError);
        }
    }

    private void fetchUserNamesAndShowDialog(List<String> userIds, String title, String fieldName) {
        List<String> userNames = new ArrayList<>();
        final int[] fetchCount = {0};

        for (String userId : userIds) {
            db.collection("users").document(userId)
                    .get()
                    .addOnSuccessListener(userDoc -> {
                        String name = userDoc.exists() ? userDoc.getString("name") : "Unknown";
                        userNames.add(name);

                        fetchCount[0]++;
                        if (fetchCount[0] == userIds.size()) {
                            showEntrantListDialog(userNames, title, fieldName);
                        }
                    })
                    .addOnFailureListener(e -> {
                        userNames.add("Unknown");
                        fetchCount[0]++;
                        if (fetchCount[0] == userIds.size()) {
                            showEntrantListDialog(userNames, title, fieldName);
                        }
                    });
        }
    }
    private void showEntrantListDialog(List<String> userNames, String title, String fieldName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(title + " (" + userNames.size() + ")");

        // Create scrollable list
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 16, 32, 16);

        for (String name : userNames) {
            TextView nameView = new TextView(getContext());
            nameView.setText("• " + name);
            nameView.setTextSize(16);
            nameView.setPadding(8, 12, 8, 12);
            layout.addView(nameView);
        }

        builder.setView(layout);

        builder.setPositiveButton("Download CSV", (dialog, which) -> {
            String fileName = fieldName.replace("Entrants", "").toLowerCase();
            CSVDownloadManager.exportToCSV(getContext(), fileName, userNames);
        });

        builder.setNegativeButton("Close", null);
        builder.show();
    }
    private void downloadEntrantList(String fieldName, String fileName) {
        progressBar.setVisibility(View.VISIBLE);

        // 1. ACCEPTED
        if (fieldName.equals(FIELD_ACCEPTED)) {
            db.collection("events").document(eventId).collection("accepted").get()
                    .addOnSuccessListener(snapshot -> {
                        List<String> userIds = new ArrayList<>();
                        for(DocumentSnapshot d : snapshot.getDocuments()) userIds.add(d.getId());
                        fetchUserNamesAndDownload(userIds, fileName);
                    })
                    .addOnFailureListener(this::handleError);
        }
        // 2. DECLINED
        else if (fieldName.equals(FIELD_DECLINED)) {
            db.collection("events").document(eventId).collection("declined").get()
                    .addOnSuccessListener(snapshot -> {
                        List<String> userIds = new ArrayList<>();
                        for(DocumentSnapshot d : snapshot.getDocuments()) userIds.add(d.getId());
                        fetchUserNamesAndDownload(userIds, fileName);
                    })
                    .addOnFailureListener(this::handleError);
        }
        // 3. RETRY
        else if (fieldName.equals(FIELD_RETRY)) {
            db.collection("waitingLists").document(eventId).get()
                    .addOnSuccessListener(doc -> {
                        List<String> userIds = (List<String>) doc.get("retryParticipants");
                        fetchUserNamesAndDownload(userIds, fileName);
                    })
                    .addOnFailureListener(this::handleError);
        }
        // 4. WAITLIST
        else {
            db.collection("events").document(eventId).get()
                    .addOnSuccessListener(doc -> {
                        List<String> userIds = (List<String>) doc.get(FIELD_WAITLIST);
                        fetchUserNamesAndDownload(userIds, fileName);
                    })
                    .addOnFailureListener(this::handleError);
        }
    }

    private void handleError(Exception e) {
        progressBar.setVisibility(View.GONE);
        Log.e(TAG, "Error fetching list", e);
        Toast.makeText(getContext(), "Error loading list", Toast.LENGTH_SHORT).show();
    }

    private void fetchUserNamesAndDownload(List<String> userIds, String fileName) {
        List<String> userNames = new ArrayList<>();
        final int[] fetchCount = {0};

        for (String userId : userIds) {
            db.collection("users").document(userId)
                    .get()
                    .addOnSuccessListener(userDoc -> {
                        String name = userDoc.exists() ? userDoc.getString("name") : "Unknown";
                        userNames.add(name);

                        fetchCount[0]++;
                        if (fetchCount[0] == userIds.size()) {
                            progressBar.setVisibility(View.GONE);
                            CSVDownloadManager.exportToCSV(getContext(), fileName, userNames);
                        }
                    });
        }
    }

    private void showDrawLotteryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Run Lottery Draw");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_select, null);
        EditText etSampleSize = dialogView.findViewById(R.id.et_sample_size);

        if (poolSize > 0) {
            etSampleSize.setText(String.valueOf(poolSize));
        }

        builder.setView(dialogView);
        builder.setMessage("This will randomly select entrants from the waitlist and send them notifications. This can only be done once.");

        builder.setPositiveButton("Run Lottery", (dialog, which) -> {
            String sampleSizeStr = etSampleSize.getText().toString().trim();

            if (sampleSizeStr.isEmpty()) {
                Toast.makeText(getContext(), "Please enter sample size", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                int sampleSize = Integer.parseInt(sampleSizeStr);
                if (sampleSize <= 0) {
                    Toast.makeText(getContext(), "Sample size must be greater than 0", Toast.LENGTH_SHORT).show();
                    return;
                }

                runLottery(sampleSize);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Invalid number", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void runLottery(int sampleSize) {
        progressBar.setVisibility(View.VISIBLE);
        btnDrawLottery.setEnabled(false);

        prepareEventForLottery(eventId, new PrepareCallback() {
            @Override
            public void onSuccess() {
                lotteryManager.initializeLottery(eventId, sampleSize, new LotteryManager.LotteryCallback() {
                    @Override
                    public void onSuccess(String message) {
                        db.collection("events").document(eventId)
                                .update("lotteryStatus", "completed")
                                .addOnSuccessListener(aVoid -> {
                                    progressBar.setVisibility(View.GONE);
                                    lotteryStatus = "completed";
                                    updateLotteryStatusUI();
                                    loadEntrantLocations(); // Refresh map with new colors

                                    Toast.makeText(getContext(),
                                            "Lottery completed! " + message,
                                            Toast.LENGTH_LONG).show();
                                });
                    }

                    @Override
                    public void onError(String error) {
                        progressBar.setVisibility(View.GONE);
                        btnDrawLottery.setEnabled(true);
                        Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                btnDrawLottery.setEnabled(true);
                Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void prepareEventForLottery(String eventId, PrepareCallback callback) {
        db.collection("events").document(eventId)
                .get()
                .addOnSuccessListener(eventDoc -> {
                    if (!eventDoc.exists()) {
                        callback.onError("Event not found");
                        return;
                    }

                    List<String> waitlistUsers = (List<String>) eventDoc.get(FIELD_WAITLIST);

                    if (waitlistUsers == null || waitlistUsers.isEmpty()) {
                        callback.onError("No entrants in waitlist");
                        return;
                    }

                    Map<String, Object> waitingListData = new HashMap<>();
                    waitingListData.put("entrants", waitlistUsers);
                    waitingListData.put("eventId", eventId);
                    waitingListData.put("createdAt", System.currentTimeMillis());

                    db.collection("waitingLists").document(eventId)
                            .set(waitingListData)
                            .addOnSuccessListener(aVoid -> callback.onSuccess())
                            .addOnFailureListener(e -> callback.onError("Failed: " + e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onError("Failed: " + e.getMessage()));
    }

    private interface PrepareCallback {
        void onSuccess();
        void onError(String error);
    }
}











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
import java.util.List;
import java.util.Map;
/**
 * EventManagementFragment handles the management interface for a specific event.
 * It allows organizers to view entrant lists (waitlist, accepted, declined), run the lottery,
 * download entrant data as CSV, and view entrant locations on a map.
 */
public class EventManagementFragment extends Fragment implements OnMapReadyCallback {
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

    //  Firestore field names to match LotteryManager
    private static final String FIELD_WAITLIST = "waitlistUsers";
    private static final String FIELD_ACCEPTED = "accepted";        // Fixed: matches LotteryManager
    private static final String FIELD_DECLINED = "declined";        // Fixed: matches LotteryManager
    private static final String FIELD_RETRY = "retryEntrants";
    private static final String FIELD_SELECTED = "selected";        // Fixed: matches LotteryManager
    /**
     * Creates a new instance of EventManagementFragment with the specified event ID.
     *
     * @param eventId The unique identifier of the event to manage.
     * @return A new instance of fragment EventManagementFragment.
     */
    public static EventManagementFragment newInstance(String eventId) {
        EventManagementFragment fragment = new EventManagementFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EVENT_ID, eventId);
        fragment.setArguments(args);
        return fragment;
    }
    /**
     * Initializes the fragment, Firestore instance, and LotteryManager.
     * Retrieves the event ID from the arguments.
     *
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();
        lotteryManager = new LotteryManager();

        if (getArguments() != null) {
            eventId = getArguments().getString(ARG_EVENT_ID);
        }

        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(getContext(), "Invalid event ID", Toast.LENGTH_SHORT).show();
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        }
    }
    /**
     * Inflates the layout for the event management screen.
     *
     * @param inflater           The LayoutInflater object.
     * @param container          The parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return The View for the fragment's UI.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_event_management, container, false);
    }
    /**
     * Sets up views, initializes the map, loads event data, and configures button listeners after the view is created.
     *
     * @param view               The View returned by onCreateView.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        setupMap();
        loadEventData();
        setupListeners();
    }
    /**
     * Initializes UI components by finding them in the view layout.
     *
     * @param view The root view of the fragment.
     */
    private void initializeViews(View view) {
        // Header
        tvEventName = view.findViewById(R.id.tv_event_name);
        tvWaitlistCount = view.findViewById(R.id.tv_waitlist_count);
        tvPoolSize = view.findViewById(R.id.tv_pool_size);
        tvLotteryStatus = view.findViewById(R.id.tv_lottery_status);
        btnBack = view.findViewById(R.id.btn_back);

        // Waitlist Preview Section
        waitlistPreviewContainer = view.findViewById(R.id.waitlist_preview_container);
        btnViewAllWaitlist = view.findViewById(R.id.btn_view_all_waitlist);
        btnDownloadAllWaitlist = view.findViewById(R.id.btn_download_all_waitlist);

        // Map
        mapCard = view.findViewById(R.id.map_card);

        // Lottery Actions
        btnDrawLottery = view.findViewById(R.id.btn_draw_lottery);

        // Post-Draw Actions
        postDrawActionsContainer = view.findViewById(R.id.post_draw_actions_container);

        btnViewAccepted = view.findViewById(R.id.btn_view_accepted);
        btnDownloadAccepted = view.findViewById(R.id.btn_download_accepted);
        btnViewDeclined = view.findViewById(R.id.btn_view_declined);
        btnDownloadDeclined = view.findViewById(R.id.btn_download_declined);
        btnViewRetry = view.findViewById(R.id.btn_view_retry);
        btnDownloadRetry = view.findViewById(R.id.btn_download_retry);

        progressBar = view.findViewById(R.id.progress_bar);
    }
    /**
     * Initializes the Google Map fragment asynchronously.
     */
    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.map_fragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }
    /**
     * Callback triggered when the Google Map is ready to be used.
     * Configures map settings and loads entrant locations.
     *
     * @param map The GoogleMap instance.
     */
    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setScrollGesturesEnabled(true);
        loadEntrantLocations();
    }
    /**
     * Fetches event details from Firestore, including name, capacity, waitlist size, and lottery status.
     * Updates the UI accordingly.
     */
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

                    // Check if lottery has run by looking for the 'selected' map
                    Map<String, Object> selected = (Map<String, Object>) documentSnapshot.get(FIELD_SELECTED);
                    boolean isLotteryRun = selected != null && !selected.isEmpty();

                    if (poolSizeStr != null && !poolSizeStr.isEmpty()) {
                        try {
                            poolSize = Integer.parseInt(poolSizeStr);
                        } catch (NumberFormatException e) {
                            poolSize = 0;
                        }
                    }

                    tvEventName.setText(eventName != null ? eventName : "Unknown Event");
                    tvWaitlistCount.setText("Total Entrants: " + waitlistCount);
                    tvPoolSize.setText("Sample Size: " + poolSize);

                    updateLotteryStatusUI(isLotteryRun);
                    loadWaitlistPreview();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Error loading event", e);
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    /**
     * Updates the UI buttons and text based on whether the lottery has been completed.
     *
     * @param isCompleted True if the lottery has been run, false otherwise.
     */
    private void updateLotteryStatusUI(boolean isCompleted) {
        if (isCompleted) {
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
    /**
     * Loads a preview list of the first few entrants on the waitlist.
     */
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
                    int limit = Math.min(10, waitlistUsers.size());
                    for (int i = 0; i < limit; i++) {
                        addEntrantPreviewItem(waitlistUsers.get(i));
                    }
                });
    }
    /**
     * Fetches a user's name by ID and adds it to the waitlist preview container.
     *
     * @param userId The ID of the user to display.
     */
    private void addEntrantPreviewItem(String userId) {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(userDoc -> {
                    if (getContext() == null) return;

                    String name = userDoc.exists() ? userDoc.getString("name") : "Unknown User";
                    TextView nameView = new TextView(getContext());
                    nameView.setText("• " + name);
                    nameView.setTextSize(14);
                    nameView.setTextColor(Color.BLACK);
                    nameView.setPadding(16, 8, 16, 8);
                    waitlistPreviewContainer.addView(nameView);
                });
    }
    /**
     * Sets up click listeners for all interactive buttons in the fragment.
     */
    private void setupListeners() {
        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        btnViewAllWaitlist.setOnClickListener(v -> viewEntrantList(FIELD_WAITLIST, "All Waitlist Entrants"));
        btnDownloadAllWaitlist.setOnClickListener(v -> downloadEntrantList(FIELD_WAITLIST, "waitlist_entrants"));

        btnDrawLottery.setOnClickListener(v -> showDrawLotteryDialog());

        btnViewAccepted.setOnClickListener(v -> viewEntrantList(FIELD_ACCEPTED, "Accepted Entrants"));
        btnDownloadAccepted.setOnClickListener(v -> downloadEntrantList(FIELD_ACCEPTED, "accepted_entrants"));

        btnViewDeclined.setOnClickListener(v -> viewEntrantList(FIELD_DECLINED, "Declined Entrants"));
        btnDownloadDeclined.setOnClickListener(v -> downloadEntrantList(FIELD_DECLINED, "declined_entrants"));

        btnViewRetry.setOnClickListener(v -> viewEntrantList(FIELD_RETRY, "Retry Entrants"));
        btnDownloadRetry.setOnClickListener(v -> downloadEntrantList(FIELD_RETRY, "retry_entrants"));
    }
    /**
     * Fetches user IDs from a specific Firestore field and displays them in a dialog.
     *
     * @param fieldName The Firestore field name containing the list of user IDs.
     * @param title     The title to display on the dialog.
     */
    private void viewEntrantList(String fieldName, String title) {
        progressBar.setVisibility(View.VISIBLE);

        db.collection("events").document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    progressBar.setVisibility(View.GONE);
                    List<String> userIds = (List<String>) doc.get(fieldName);

                    if (userIds == null || userIds.isEmpty()) {
                        Toast.makeText(getContext(), "No entrants in this category", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    fetchUserNamesAndShowDialog(userIds, title, fieldName);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    /**
     * Resolves a list of user IDs to user names and shows them in a list dialog.
     *
     * @param userIds   The list of user IDs to resolve.
     * @param title     The title for the dialog.
     * @param fieldName The original field name (used for CSV export context).
     */
    private void fetchUserNamesAndShowDialog(List<String> userIds, String title, String fieldName) {
        List<String> userNames = new ArrayList<>();
        final int[] fetchCount = {0};

        if (userIds.isEmpty()) return;

        for (String userId : userIds) {
            db.collection("users").document(userId).get()
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
    /**
     * Displays a dialog containing the list of user names and an option to download as CSV.
     *
     * @param userNames The list of user names to display.
     * @param title     The title of the dialog.
     * @param fieldName The name of the list being displayed.
     */
    private void showEntrantListDialog(List<String> userNames, String title, String fieldName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(title + " (" + userNames.size() + ")");

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 16, 32, 16);

        android.widget.ScrollView scrollView = new android.widget.ScrollView(getContext());
        LinearLayout listContainer = new LinearLayout(getContext());
        listContainer.setOrientation(LinearLayout.VERTICAL);

        for (String name : userNames) {
            TextView nameView = new TextView(getContext());
            nameView.setText("• " + name);
            nameView.setTextSize(16);
            nameView.setPadding(8, 12, 8, 12);
            listContainer.addView(nameView);
        }

        scrollView.addView(listContainer);
        layout.addView(scrollView);
        builder.setView(layout);

        builder.setPositiveButton("Download CSV", (dialog, which) -> {
            String fileName = fieldName.toLowerCase();
            CSVDownloadManager.exportToCSV(getContext(), fileName, userNames);
        });

        builder.setNegativeButton("Close", null);
        builder.show();
    }
    /**
     * Downloads the list of users from a specific field as a CSV file.
     *
     * @param fieldName The Firestore field name containing the user IDs.
     * @param fileName  The base name for the downloaded file.
     */
    private void downloadEntrantList(String fieldName, String fileName) {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("events").document(eventId).get()
                .addOnSuccessListener(doc -> {
                    List<String> userIds = (List<String>) doc.get(fieldName);
                    if (userIds == null || userIds.isEmpty()) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "No entrants to download", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    fetchUserNamesAndDownload(userIds, fileName);
                });
    }
    /**
     * Resolves user IDs to names and triggers the CSV download.
     *
     * @param userIds  The list of user IDs to export.
     * @param fileName The filename for the export.
     */
    private void fetchUserNamesAndDownload(List<String> userIds, String fileName) {
        List<String> userNames = new ArrayList<>();
        final int[] fetchCount = {0};

        for (String userId : userIds) {
            db.collection("users").document(userId).get()
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
    /**
     * Displays a dialog prompting the user to confirm the lottery draw and set the sample size.
     */
    private void showDrawLotteryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Run Lottery Draw");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_select, null);
        EditText etSampleSize = dialogView.findViewById(R.id.et_sample_size);

        if (poolSize > 0) {
            etSampleSize.setText(String.valueOf(poolSize));
        }

        builder.setView(dialogView);
        builder.setMessage("This will randomly select entrants from the waitlist.");

        builder.setPositiveButton("Run Lottery", (dialog, which) -> {
            String sampleSizeStr = etSampleSize.getText().toString().trim();
            try {
                int sampleSize = Integer.parseInt(sampleSizeStr);
                if (sampleSize <= 0) throw new NumberFormatException();
                runLottery(sampleSize);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Invalid sample size", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    /**
     * Executes the lottery draw using the LotteryManager and updates the UI on completion.
     *
     * @param sampleSize The number of winners to select.
     */
    private void runLottery(int sampleSize) {
        progressBar.setVisibility(View.VISIBLE);
        btnDrawLottery.setEnabled(false);

        //  Directly call LotteryManager which now writes to 'events' correctly
        lotteryManager.initializeLottery(eventId, sampleSize, new LotteryManager.LotteryCallback() {
            @Override
            public void onSuccess(String message) {
                progressBar.setVisibility(View.GONE);
                updateLotteryStatusUI(true);
                loadEntrantLocations(); // Refresh map
                Toast.makeText(getContext(), "Lottery completed!", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                btnDrawLottery.setEnabled(true);
                Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }
    /**
     * Fetches entrant locations from Firestore and plots markers on the Google Map.
     */
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

                        if (geoPoint != null) {
                            LatLng position = new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude());

                            // Default color for now
                            float markerColor = BitmapDescriptorFactory.HUE_AZURE;

                            googleMap.addMarker(new MarkerOptions()
                                    .position(position)
                                    .title(userName != null ? userName : "Entrant")
                                    .icon(BitmapDescriptorFactory.defaultMarker(markerColor)));

                            boundsBuilder.include(position);
                            hasLocations = true;
                        }
                    }

                    if (hasLocations) {
                        LatLngBounds bounds = boundsBuilder.build();
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading locations", e));
    }
}